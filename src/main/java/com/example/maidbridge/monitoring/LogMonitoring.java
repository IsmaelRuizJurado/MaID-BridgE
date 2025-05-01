package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.psi.*;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import static com.example.maidbridge.monitoring.Auxiliaries.*;

public class LogMonitoring implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null; // We use collectSlowLineMarkers
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) return;

        PsiFile file = elements.get(0).getContainingFile();
        String classQualifiedName = getQualifiedClassName(file);
        if (classQualifiedName == null) return;
        Map<String, LogData> logCounts = countLogOccurrences(file);

        for (PsiElement element : elements) {
            if (!(element instanceof PsiMethodCallExpression callExpr)) continue;

            PsiReferenceExpression methodExpr = callExpr.getMethodExpression();
            String methodName = methodExpr.getReferenceName();
            if (methodName == null) continue;

            String logLevel = null;
            String logMessage = null;

            PsiExpression[] args = callExpr.getArgumentList().getExpressions();

            if ((methodName.equals("info") || methodName.equals("warning") || methodName.equals("severe")) && args.length >= 1) {
                logLevel = methodName.toUpperCase();
                logMessage = getStringLiteralValue(args[0]);
            } else if (methodName.equals("log") && args.length >= 2) {
                PsiExpression levelExpr = args[0];
                if (levelExpr instanceof PsiReferenceExpression refExpr) {
                    PsiElement resolved = refExpr.resolve();
                    if (resolved instanceof PsiField field && field.getContainingClass() != null) {
                        String className = field.getContainingClass().getQualifiedName();
                        String fieldName = field.getName();
                        if ("java.util.logging.Level".equals(className) && fieldName != null) {
                            logLevel = fieldName.toUpperCase();
                            logMessage = getStringLiteralValue(args[1]);
                        }
                    }
                }
            }

            if (logLevel == null || logMessage == null) continue;

            for (Map.Entry<String, LogData> entry : logCounts.entrySet()) {
                String knownMessage = entry.getKey();
                LogData data = entry.getValue();

                if (logMessage.contains(knownMessage)) {
                    Icon icon = createColoredIcon(getColorForLevel(data.level), data.count);

                    String kibanaUrl = buildKibanaUrl(classQualifiedName, knownMessage);

                    LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
                            element,
                            element.getTextRange(),
                            icon,
                            Pass.LINE_MARKERS,
                            (Function<PsiElement, String>) psi -> String.format("""
                                <html>
                                <b>Type:</b> %s<br>
                                <b>Message:</b> %s<br>
                                <b>Total occurrences:</b> %d<br>
                                <b>Occurrences (last 24h):</b> %d<br>
                                <b>Click icon to copy Kibana URL</b>
                                </html>
                                """,
                                    data.level,
                                    knownMessage,
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
                            GutterIconRenderer.Alignment.LEFT
                    );

                    result.add(marker);
                    break;
                }
            }
        }
    }


    private static Map<String, LogData> countLogOccurrences(PsiFile sourceFile) {
        Map<String, LogData> counts = new HashMap<>();

        String classQualifiedName = getQualifiedClassName(sourceFile);
        if (classQualifiedName == null) return counts;

        String queryJson = String.format("""
            {
              "query": {
                "bool": {
                  "must": [
                    { "match": { "logger_name": "%s" } }
                  ]
                }
              },
              "size": 10000,
              "_source": ["message", "logger_name", "level", "@timestamp"]
            }
            """, classQualifiedName);

        try {
            String responseJson = ElasticConnector.performSearch(queryJson);
            JSONObject response = new JSONObject(responseJson);
            JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");

            for (int i = 0; i < hits.length(); i++) {
                JSONObject source = hits.getJSONObject(i).getJSONObject("_source");

                String message = source.optString("message");
                String level = source.optString("level");

                counts.compute(message, (k, v) -> {
                    boolean isRecent = false;
                    String timestampStr = source.optString("@timestamp");
                    if (timestampStr != null && !timestampStr.isEmpty()) {
                        try {
                            ZonedDateTime logTime = ZonedDateTime.parse(timestampStr);
                            isRecent = logTime.isAfter(ZonedDateTime.now(java.time.ZoneOffset.UTC).minusHours(24));
                        } catch (Exception e) {
                            // Optionally handle parsing error
                        }
                    }

                    if (v == null) return new LogData(1, level, isRecent ? 1 : 0);
                    v.count++;
                    if (isRecent) v.countLast24h++;
                    return v;
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return counts;
    }
}
