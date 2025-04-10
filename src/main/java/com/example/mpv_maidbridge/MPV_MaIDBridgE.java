package com.example.mpv_maidbridge;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static class LogData {
        int count;
        String level;

        LogData(int count, String level) {
            this.count = count;
            this.level = level;
        }
    }

    private static Map<String, LogData> countLogOccurrences(PsiFile sourceFile) {
        Map<String, LogData> counts = new HashMap<>();
        VirtualFile vf = sourceFile.getVirtualFile();
        if (vf == null) return counts;

        String directoryPath = vf.getParent().getPath();
        File logFile = new File(directoryPath, "logs.txt");
        if (!logFile.exists()) return counts;

        String classQualifiedName = getQualifiedClassName(sourceFile);
        if (classQualifiedName == null) return counts;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String message = extractField(line, "message");
                String loggerName = extractField(line, "logger_name");
                String level = extractField(line, "level");

                if (message != null && loggerName != null && level != null &&
                        loggerName.equals(classQualifiedName)) {
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

    private static String getQualifiedClassName(PsiFile file) {
        if (!(file instanceof PsiJavaFile javaFile)) return null;
        PsiClass[] classes = javaFile.getClasses();
        if (classes.length == 0) return null;
        return classes[0].getQualifiedName();
    }

    private static String extractField(String line, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int startIdx = line.indexOf(pattern);
        if (startIdx < 0) return null;

        startIdx += pattern.length();
        int endIdx = line.indexOf("\"", startIdx);
        if (endIdx < 0) return null;

        return line.substring(startIdx, endIdx);
    }

    private static Color getColorForLevel(String level) {
        return switch (level.toUpperCase()) {
            case "INFO" -> Color.GRAY;
            case "WARNING" -> Color.ORANGE;
            case "SEVERE", "ERROR" -> Color.RED;
            default -> Color.BLUE;
        };
    }

    private static Icon createColoredIcon(Color color, int count) {
        int size = 16;
        BufferedImage image = UIUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Fondo circular
        g.setColor(color);
        g.fillOval(0, 0, size, size);

        // Determinar color del texto
        Color textColor = (color.equals(Color.ORANGE)) ? Color.BLACK : Color.WHITE;

        // Texto (número)
        g.setColor(textColor);
        g.setFont(new Font("Arial", Font.BOLD, 11));

        String text = String.valueOf(count);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int x = (size - textWidth) / 2;
        int y = (size + textHeight) / 2 - 2;

        g.drawString(text, x, y);
        g.dispose();
        return new ImageIcon(image);
    }


    private static String getStringLiteralValue(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression literal && literal.getValue() instanceof String str) {
            return str;
        }
        return expr.getText().replace("\"", "");
    }

}
