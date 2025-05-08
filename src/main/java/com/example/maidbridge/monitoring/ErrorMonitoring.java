package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;

import static com.example.maidbridge.monitoring.Auxiliaries.*;


public class ErrorMonitoring implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null;
    }

    Integer count = 0;

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) return;

        if(count == 0) {
            PsiFile file = elements.get(0).getContainingFile();
            Project project = file.getProject();
            String classQualifiedName = getQualifiedClassName(file);
            if (classQualifiedName == null) return;

            Map<Integer, Map<String, ErrorData>> errorMapByLine = countErrorOccurrences();
            if (errorMapByLine.isEmpty()) return;

            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            if (document == null) return;

            for (Map.Entry<Integer, Map<String, ErrorData>> lineEntry : errorMapByLine.entrySet()) {
                int line = lineEntry.getKey();

                if (line < 0 || line >= document.getLineCount()) continue;

                int offset = document.getLineStartOffset(line);
                PsiElement element = file.findElementAt(offset);
                if (element == null) continue;

                // Subir al primer elemento significativo si es necesario
                PsiElement target = element;
                while (target != null && !(target instanceof PsiStatement || target instanceof PsiTryStatement)) {
                    target = target.getParent();
                }
                if (target == null) target = element;

                // Elegir el error más frecuente de esa línea
                Map<String, ErrorData> messages = lineEntry.getValue();
                Map.Entry<String, ErrorData> mostFrequent = messages.entrySet().stream()
                        .max(Comparator.comparingInt(e -> e.getValue().count))
                        .orElse(null);

                if (mostFrequent == null) continue;

                String message = mostFrequent.getKey();
                ErrorData data = mostFrequent.getValue();

                String fqcn = extractClassNameFromStackTrace(data.stackTrace);

                if (data.stackTrace == null || !fqcn.equals(classQualifiedName)) continue;

                Icon icon = createColoredIcon(Color.RED, data.count, Color.WHITE);

                String kibanaUrl = buildKibanaUrlError(data.stackTrace);

                LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
                        target,
                        target.getTextRange(),
                        icon,
                        psi -> String.format("""
                        <html>
                        <b>Error Type:</b> %s<br>
                        <b>Message:</b> %s<br>
                        <b>Total occurrences:</b> %d<br>
                        <b>Occurrences (last 24h):</b> %d<br>
                        <b>Click icon to copy Kibana URL</b>
                        </html>
                        """,
                                data.type,
                                message,
                                data.count,
                                data.countLast24h
                        ),
                        (mouseEvent, psiElement) -> {
                            StringSelection selection = new StringSelection(kibanaUrl);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

                            NotificationGroupManager.getInstance()
                                    .getNotificationGroup("MaID-BridgE Notification Group")
                                    .createNotification("URL copiada al portapapeles", NotificationType.INFORMATION)
                                    .notify(ProjectUtil.guessProjectForFile(file.getVirtualFile()));
                        },
                        GutterIconRenderer.Alignment.LEFT,
                        () -> "maid-bridge error"
                );

                result.add(marker);
            }
            count++;
        } else {
            count = 0;
        }
    }



    public static Map<Integer, Map<String, ErrorData>> countErrorOccurrences() {
        Map<Integer, Map<String, ErrorData>> result = new HashMap<>();

        String queryJson = """
        {
          "size": 10000,
          "_source": ["message", "level", "@timestamp", "stack_trace", "thread_name"],
          "query": {
            "bool": {
              "should": [
                { "match": { "level": "ERROR" } },
                { "match": { "level": "DEBUG" } }
              ]
            }
          },
          "sort": [{ "@timestamp": "asc" }]
        }
        """;

        try {
            String responseJson = ElasticConnector.performSearch(queryJson);
            JSONArray hits = new JSONObject(responseJson).getJSONObject("hits").getJSONArray("hits");

            Map<String, List<JSONObject>> debugLogsByThread = new HashMap<>();
            List<JSONObject> errorLogs = new ArrayList<>();

            for (int i = 0; i < hits.length(); i++) {
                JSONObject log = hits.getJSONObject(i).getJSONObject("_source");
                String level = log.optString("level", "");
                String thread = log.optString("thread_name", "unknown");

                if (level.equalsIgnoreCase("DEBUG")) {
                    debugLogsByThread.computeIfAbsent(thread, k -> new ArrayList<>()).add(log);
                } else if (level.equalsIgnoreCase("ERROR")) {
                    errorLogs.add(log);
                }
            }

            for (JSONObject error : errorLogs) {
                String thread = error.optString("thread_name", "unknown");
                String errorTimestamp = error.optString("@timestamp", "");
                ZonedDateTime errorTime;
                try {
                    errorTime = ZonedDateTime.parse(errorTimestamp);
                } catch (Exception e) {
                    continue;
                }

                List<JSONObject> candidates = debugLogsByThread.getOrDefault(thread, List.of());

                JSONObject matchedDebug = null;
                for (int i = candidates.size() - 1; i >= 0; i--) {
                    JSONObject debug = candidates.get(i);
                    try {
                        ZonedDateTime debugTime = ZonedDateTime.parse(debug.optString("@timestamp", ""));
                        if (!debugTime.isAfter(errorTime)) {
                            matchedDebug = debug;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (matchedDebug == null) continue;

                String debugMessage = matchedDebug.optString("message", "");
                String stackTrace = error.optString("stack_trace", "");
                String exceptionType = extractExceptionType(stackTrace);
                int line = extractLineNumberFromStackTrace(stackTrace, debugMessage);

                boolean isRecent = errorTime.isAfter(ZonedDateTime.now(ZoneOffset.UTC).minusHours(24));

                if (line >= 0) {
                    result.computeIfAbsent(line, k -> new HashMap<>())
                            .compute(debugMessage, (msg, existing) -> {
                                if (existing == null) {
                                    return new ErrorData(1, isRecent ? 1 : 0, exceptionType, stackTrace);
                                } else {
                                    existing.count++;
                                    if (isRecent) existing.countLast24h++;
                                    return existing;
                                }
                            });
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


}

