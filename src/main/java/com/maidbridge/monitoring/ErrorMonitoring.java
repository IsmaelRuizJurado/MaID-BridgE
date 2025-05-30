package com.maidbridge.monitoring;

import com.maidbridge.elastic.ElasticConnector;
import com.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;

import static com.maidbridge.elastic.ElasticConnector.notifyEmptyResponse;
import static com.maidbridge.monitoring.Auxiliaries.*;


public class ErrorMonitoring implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null;
    }

    //The G.O.A.T.
    Integer count = 0;

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) return;

        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        if (count == 0) {
            PsiFile file = elements.get(0).getContainingFile();
            Project project = file.getProject();
            String classQualifiedName = getQualifiedClassName(file);
            if (classQualifiedName == null) return;

            Map<MethodKey, Map<String, ErrorData>> errorsByMethod = countErrorOccurrences(project);
            if (errorsByMethod.isEmpty()) return;

            file.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethod(PsiMethod method) {
                    super.visitMethod(method);

                    String methodName = method.getName();
                    MethodKey key = new MethodKey(classQualifiedName, methodName);
                    Map<String, ErrorData> errorMap = errorsByMethod.get(key);
                    if (errorMap == null || errorMap.isEmpty()) return;

                    ZonedDateTime threshold = switch (settings.getErrorTimeRange()) {
                        case "7d" -> ZonedDateTime.now().minusDays(7);
                        case "30d" -> ZonedDateTime.now().minusDays(30);
                        case "custom" -> settings.getErrorCustomTime();
                        default -> ZonedDateTime.now().minusHours(24);
                    };

                    int totalCount = 0;
                    int totalLast24h = 0;
                    StringBuilder tooltipBuilder = new StringBuilder("<html>");

                    for (Map.Entry<String, ErrorData> entry : errorMap.entrySet()) {
                        String message = entry.getKey();
                        ErrorData data = entry.getValue();

                        if (!data.errorTime.isAfter(threshold)) continue;
                        if (data.stackTrace == null || !extractClassNameFromStackTrace(data.stackTrace).equals(classQualifiedName)) continue;

                        totalCount += data.count;
                        totalLast24h += data.countLast24h;

                        tooltipBuilder.append(String.format("""
                        <b>Error Type:</b> %s<br>
                        <b>Message:</b> %s<br>
                        <b>Total occurrences:</b> %d<br>
                        <b>Occurrences (last 24h):</b> %d<br><br>
                        """,
                                data.type, message, data.count, data.countLast24h));
                    }

                    if (totalCount == 0) return;

                    tooltipBuilder.append("<b>Click the icon to open the errors in this method in Kibana (URL will be copied)</b></html>");

                    Icon icon = createColoredIcon(Color.RED, totalCount, Color.WHITE);
                    String kibanaUrl = buildKibanaUrlError("multiple", key.methodName);

                    LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
                            method.getNameIdentifier(),
                            method.getNameIdentifier().getTextRange(),
                            icon,
                            psi -> tooltipBuilder.toString(),
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
            });
            count++;
        } else {
            count = 0;
        }
    }

    public static Map<MethodKey, Map<String, ErrorData>> countErrorOccurrences(Project project) {
        Map<MethodKey, Map<String, ErrorData>> result = new HashMap<>();

        String queryJson = """
    {
      "size": 10000,
      "_source": ["message", "@timestamp", "stack_trace"],
      "query": {
        "bool": {
          "should": [
            { "match": { "level": "ERROR" } }
          ]
        }
      },
      "sort": [{ "@timestamp": "asc" }]
    }
    """;

        try {
            String responseJson = ElasticConnector.performSearch(queryJson);
            JSONArray hits = new JSONObject(responseJson).getJSONObject("hits").getJSONArray("hits");

            MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
            ZonedDateTime threshold = switch (settings.getErrorTimeRange()) {
                case "7d" -> ZonedDateTime.now().minusDays(7);
                case "30d" -> ZonedDateTime.now().minusDays(30);
                case "custom" -> settings.getErrorCustomTime();
                default -> ZonedDateTime.now().minusHours(24);
            };
            Map<String, PsiFile> fqcnToFile = preloadFqcnMap(project);

            for (int i = 0; i < hits.length(); i++) {
                try {
                    if (result.size() > 10000) break;

                    JSONObject log = hits.getJSONObject(i).getJSONObject("_source");
                    ZonedDateTime errorTime = ZonedDateTime.parse(log.optString("@timestamp", ""));
                    if (!errorTime.isAfter(threshold)) continue;

                    String stackTrace = log.optString("stack_trace", "");
                    if (stackTrace == null || stackTrace.isBlank()) continue;

                    StackTraceInfo info = parseStackTrace(stackTrace, fqcnToFile);
                    if (info == null || info.fqcn == null || info.methodName == null || info.line < 0) continue;

                    String rawMessage = log.optString("message", "");
                    String extractedMessage;
                    int start = rawMessage.indexOf("threw exception [");
                    int end = rawMessage.indexOf(']', start);
                    if (start != -1 && end != -1 && end > start) {
                        extractedMessage = rawMessage.substring(start + 17, end);
                    } else {
                        extractedMessage = rawMessage;
                    }

                    MethodKey key = new MethodKey(info.fqcn, info.methodName);
                    boolean isRecent = errorTime.isAfter(ZonedDateTime.now(ZoneOffset.UTC).minusHours(24));

                    result.computeIfAbsent(key, k -> new HashMap<>())
                            .compute(extractedMessage, (msg, existing) -> {
                                if (existing == null) {
                                    return new ErrorData(1, isRecent ? 1 : 0, info.exceptionType, stackTrace, errorTime);
                                } else {
                                    existing.count++;
                                    if (isRecent) existing.countLast24h++;
                                    return existing;
                                }
                            });

                } catch (Exception inner) {
                    inner.printStackTrace();
                }
            }

        } catch (Exception e) {
            notifyEmptyResponse();
            e.printStackTrace();
        }

        return result;
    }
}
