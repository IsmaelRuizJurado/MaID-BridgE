package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.ElasticSettingsState;
import com.intellij.psi.*;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Auxiliaries {
    public static String getQualifiedClassName(PsiFile file) {
        if (!(file instanceof PsiJavaFile javaFile)) return null;
        PsiClass[] classes = javaFile.getClasses();
        if (classes.length == 0) return null;
        return classes[0].getQualifiedName();
    }

    public static String extractField(String line, String fieldName) {
        String pattern = "\"" + fieldName + "\":\"";
        int startIdx = line.indexOf(pattern);
        if (startIdx < 0) return null;

        startIdx += pattern.length();
        int endIdx = line.indexOf("\"", startIdx);
        if (endIdx < 0) return null;

        return line.substring(startIdx, endIdx);
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

    public static String buildKibanaUrl(String loggerName, String message) {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        String baseUrl = settings.getScheme() + "://" + settings.getHost() + ":5601";

        String encodedQuery = URLEncoder.encode(
                String.format("logger_name:\"%s\" and message:\"%s\"", loggerName, message),
                StandardCharsets.UTF_8
        );

        return String.format(
                "%s/app/discover#/?_a=(index:'%s',query:(language:kuery,query:'%s'))&_g=(time:(from:now-24h,to:now))",
                baseUrl,
                settings.getIndex(),
                encodedQuery
        );
    }
}
