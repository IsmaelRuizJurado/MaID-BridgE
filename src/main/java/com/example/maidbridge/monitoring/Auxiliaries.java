package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.psi.*;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Auxiliaries {
    public static String getQualifiedClassName(PsiFile file) {
        if (!(file instanceof PsiJavaFile javaFile)) return null;
        PsiClass[] classes = javaFile.getClasses();
        if (classes.length == 0) return null;
        return classes[0].getQualifiedName();
    }

    //Métodos para Logs
    public static class LogData {
        public int count;
        public String level;
        public int countLast24h;

        public LogData(int count, String level, int countLast24h) {
            this.count = count;
            this.level = level;
            this.countLast24h = countLast24h;
        }
    }

    public static Color getColorForLevel(String level) {
        return switch (level.toUpperCase()) {
            case "INFO" -> Color.GRAY;
            case "WARNING", "WARN" -> Color.ORANGE;
            case "SEVERE", "ERROR" -> Color.RED;
            default -> Color.BLUE;
        };
    }

    public static Icon createColoredIcon(Color color, int count) {
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


    public static String getStringLiteralValue(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression literal && literal.getValue() instanceof String str) {
            return str;
        }
        return expr.getText().replace("\"", "");
    }

    public static String buildKibanaUrlLog(String loggerName, String message) {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        String baseUrl = settings.getKibanaURL();

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        String encodedQuery = URLEncoder.encode(
                String.format("logger_name:\"%s\" and message:\"%s\"", loggerName, message),
                StandardCharsets.UTF_8
        );

        return String.format(
                "%sapp/discover#/?_a=(index:'%s',query:(language:kuery,query:'%s'))&_g=(time:(from:now-24h,to:now))",
                baseUrl,
                settings.getIndex(),
                encodedQuery
        );
    }

    //Métodos para errores
    public static class ErrorData {
        public int count;
        public int countLast24h;
        public String type;
        public String stackTrace;

        public ErrorData(int count, int countLast24h, String type, String stackTrace) {
            this.count = count;
            this.countLast24h = countLast24h;
            this.type = type;
            this.stackTrace = stackTrace;
        }

    }

    public static String buildKibanaUrlError(String stackTrace) {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        String baseUrl = settings.getKibanaURL();

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        String encodedQuery = URLEncoder.encode(
                String.format("level:ERROR and stack_trace:\"%s\"", stackTrace),
                StandardCharsets.UTF_8
        );

        return String.format(
                "%sapp/discover#/?_a=(index:'%s',query:(language:kuery,query:'%s'))&_g=(time:(from:now-24h,to:now))",
                baseUrl,
                settings.getIndex(),
                encodedQuery
        );
    }

    public static String extractExceptionType(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) return "Unknown";
        String[] lines = stackTrace.split("\\\\r?\\\\n");
        if (lines.length == 0) return "Unknown";

        String firstLine = lines[0];
        int colon = firstLine.indexOf(":");
        String raw = colon != -1 ? firstLine.substring(0, colon) : firstLine;
        int lastDot = raw.lastIndexOf(".");
        return (lastDot != -1) ? raw.substring(lastDot + 1) : raw;
    }

    public static int extractLineNumberFromStackTrace(String stackTrace, String fallbackMessage) {
        if (stackTrace == null || stackTrace.isBlank()) return -1;

        Pattern pattern = Pattern.compile("at ([\\w.$]+)\\.[\\w$<>]+\\(([^:]+):(\\d+)\\)");
        Matcher matcher = pattern.matcher(stackTrace);

        while (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(3)) - 1; // Convert to 0-based
            } catch (Exception ignored) {}
        }

        return -1;
    }
}
