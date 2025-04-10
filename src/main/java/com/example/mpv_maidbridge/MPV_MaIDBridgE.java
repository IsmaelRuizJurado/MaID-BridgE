package com.example.mpv_maidbridge;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

import static com.example.mpv_maidbridge.Auxiliares.*;

public class MPV_MaIDBridgE implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null; // Usamos collectSlowLineMarkers
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) return;

        PsiFile file = elements.get(0).getContainingFile();
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
                // logger.info("mensaje")
                logLevel = methodName.toUpperCase();
                logMessage = getStringLiteralValue(args[0]);
            } else if (methodName.equals("log") && args.length >= 2) {
                // logger.log(Level.WARNING, "mensaje")
                String levelExpr = args[0].getText();
                if (levelExpr.contains("Level.")) {
                    logLevel = levelExpr.substring(levelExpr.indexOf("Level.") + 6).toUpperCase();
                    logMessage = getStringLiteralValue(args[1]);
                }
            }

            if (logLevel == null || logMessage == null) continue;

            for (Map.Entry<String, LogData> entry : logCounts.entrySet()) {
                String knownMessage = entry.getKey();
                LogData data = entry.getValue();

                if (logMessage.contains(knownMessage)) {
                    Icon icon = createColoredIcon(getColorForLevel(data.level), data.count);

                    LineMarkerInfo<PsiElement> marker = new LineMarkerInfo<>(
                            element,
                            element.getTextRange(),
                            icon,
                            Pass.LINE_MARKERS,
                            (Function<PsiElement, String>) psi ->
                                    String.format("Log '%s'\nNivel: %s\nOcurrencias: %d",
                                            knownMessage, data.level, data.count),
                            null,
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
      "_source": ["message", "logger_name", "level"]
    }
    """, classQualifiedName);

        try {
            String responseJson = ElasticConnector.performSearch("spring-petclinic-logs", queryJson);

            // Parsear JSON con org.json
            org.json.JSONObject response = new org.json.JSONObject(responseJson);
            org.json.JSONArray hits = response.getJSONObject("hits").getJSONArray("hits");

            for (int i = 0; i < hits.length(); i++) {
                org.json.JSONObject source = hits.getJSONObject(i).getJSONObject("_source");

                String message = source.optString("message");
                String level = source.optString("level");

                if (message != null && level != null) {
                    counts.compute(message, (k, v) -> {
                        if (v == null) return new LogData(1, level);
                        v.count++;
                        return v;
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return counts;
    }




}