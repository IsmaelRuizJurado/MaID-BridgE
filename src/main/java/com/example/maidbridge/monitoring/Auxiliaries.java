package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZonedDateTime;
import java.util.*;
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
        public int countLast24h;

        public LogData(int count, int countLast24h) {
            this.count = count;
            this.countLast24h = countLast24h;
        }
    }

    public static class LogKey {
        public String message;
        public String level;

        public LogKey(String message, String level) {
            this.message = message;
            this.level = level;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LogKey other)) return false;
            return Objects.equals(message, other.message) && Objects.equals(level, other.level);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message, level);
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

    public static Icon createColoredIcon(Color color, int count, @Nullable Color colorText) {
        String text;

        if (count >= 100_000) {
            text = "99K+";
        } else if (count >= 10_000) {
            text = (count / 1000) + "K";
        } else if (count >= 1000) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat df = new DecimalFormat("#.#", symbols);
            text = df.format(count / 1000.0) + "K";
            if (text.length() > 4) {
                text = text.substring(0, 3);
            }
        } else {
            text = String.valueOf(count);
        }

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Font editorFont = scheme.getFont(EditorFontType.PLAIN).deriveFont(Font.BOLD);
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(editorFont);

        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        int padding = 6;
        int width = textWidth + padding * 2;
        int height = textHeight;
        int arc = height / 2;

        BufferedImage image = UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setColor(color);
        g.fillRoundRect(0, 0, width, height, arc, arc);

        Color textColor = colorText != null ? colorText : Color.BLACK;
        g.setColor(textColor);
        g.setFont(editorFont);

        int x = (width - textWidth) / 2;
        int y = (height - fm.getDescent());

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

    public static String buildKibanaUrlLog(String loggerName, String message, String level) {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        String baseUrl = settings.getKibanaURL();

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        String encodedQuery = URLEncoder.encode(
                String.format("logger_name:\"%s\" and message:\"%s\" and level:\"%s\"", loggerName, message, level),
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
        public ZonedDateTime errorTime;

        public ErrorData(int count, int countLast24h, String type, String stackTrace, ZonedDateTime errorTime) {
            this.count = count;
            this.countLast24h = countLast24h;
            this.type = type;
            this.stackTrace = stackTrace;
            this.errorTime = errorTime;
        }
    }

    public static class MethodKey {
        public final String classQualifiedName;
        public final String methodName;

        public MethodKey(String classQualifiedName, String methodName) {
            this.classQualifiedName = classQualifiedName;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodKey)) return false;
            MethodKey that = (MethodKey) o;
            return Objects.equals(classQualifiedName, that.classQualifiedName) &&
                    Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classQualifiedName, methodName);
        }
    }

    public static String buildKibanaUrlError(String exceptionType, String methodName) {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        String baseUrl = settings.getKibanaURL();

        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        // Construir query segura para Kibana
        String query = String.format("level:ERROR AND stack_trace: *%s* AND stack_trace: *%s* ", exceptionType, methodName);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        return String.format(
                "%sapp/discover#/?_a=(index:'%s',query:(language:kuery,query:'%s'))&_g=(time:(from:now-24h,to:now))",
                baseUrl,
                settings.getIndex(),
                encodedQuery
        );
    }

    public static String extractExceptionType(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) return "Unknown";
        int end = stackTrace.indexOf('\n');
        String firstLine = (end != -1 ? stackTrace.substring(0, end) : stackTrace).trim();
        int colon = firstLine.indexOf(":");
        String raw = colon != -1 ? firstLine.substring(0, colon) : firstLine;
        int lastDot = raw.lastIndexOf(".");
        return (lastDot != -1) ? raw.substring(lastDot + 1) : raw;
    }

    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile("\\(([^:]+):(\\d+)\\)");

    public static int extractLineNumberFromStackTrace(String stackTrace, String fallbackMessage) {
        if (stackTrace == null || stackTrace.isBlank()) return -1;

        Matcher matcher = LINE_NUMBER_PATTERN.matcher(stackTrace);
        while (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(2)) - 1;
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("at\\s+([\\w.$]+)\\.\\w+\\([^:]+:\\d+\\)");

    public static String extractClassNameFromStackTrace(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) return null;

        Matcher matcher = CLASS_NAME_PATTERN.matcher(stackTrace);
        while (matcher.find()) {
            String fqcn = matcher.group(1);
            if (!fqcn.startsWith("java.") && !fqcn.startsWith("jdk.") && !fqcn.startsWith("sun.")) {
                return fqcn;
            }
        }
        return null;
    }

    public static class StackTraceInfo {
        public final String fqcn;
        public final String methodName;
        public final int line;
        public final String exceptionType;

        public StackTraceInfo(String fqcn, String methodName, int line, String exceptionType) {
            this.fqcn = fqcn;
            this.methodName = methodName;
            this.line = line;
            this.exceptionType = exceptionType;
        }
    }

    public static StackTraceInfo parseStackTrace(String stackTrace, Map<String, PsiFile> fqcnToFile) {
        Pattern pattern = Pattern.compile("at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([^:]+):(\\d+)\\)");
        Matcher matcher = pattern.matcher(stackTrace);
        while (matcher.find()) {
            String fqcn = matcher.group(1);
            String methodName = matcher.group(2);
            int line = Integer.parseInt(matcher.group(4)) - 1;

            if (!fqcn.startsWith("java.") && fqcnToFile.containsKey(fqcn)) {
                String exceptionType = extractExceptionType(stackTrace);
                return new StackTraceInfo(fqcn, methodName, line, exceptionType);
            }
        }
        return null;
    }

    public static Map<String, PsiFile> preloadFqcnMap(Project project) {
        Map<String, PsiFile> map = new HashMap<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            if (DumbService.isDumb(project)) return;

            Collection<VirtualFile> files = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
            PsiManager psiManager = PsiManager.getInstance(project);

            for (VirtualFile vf : files) {
                PsiFile file = psiManager.findFile(vf);
                if (file instanceof PsiJavaFile javaFile) {
                    String pkg = javaFile.getPackageName();
                    for (PsiClass cls : javaFile.getClasses()) {
                        map.put(pkg + "." + cls.getName(), file);
                    }
                }
            }
        });
        return map;
    }

}
