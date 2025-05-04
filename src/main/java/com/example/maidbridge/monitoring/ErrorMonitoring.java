package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.maidbridge.monitoring.Auxiliaries.*;


public class ErrorMonitoring implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement element) {
        return null; // We use collectSlowLineMarkers
    }

    private final Map<String, Map<Integer, Map<String, Integer>>> errorMap = new ConcurrentHashMap<>();
    private final Set<PsiElement> markedElements = ConcurrentHashMap.newKeySet();

    public ErrorMonitoring() {
        RefreshScheduler.addRefreshListener(() -> {
            try {
                fetchAndUpdateErrors();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super LineMarkerInfo<?>> result) {
        markedElements.clear();

        if (elements.isEmpty()) return;

        PsiFile file = elements.get(0).getContainingFile();
        if (!(file instanceof PsiJavaFile javaFile)) return;

        String className = Auxiliaries.getQualifiedClassName(file);
        if (className == null) {
            System.out.println("⚠️ No se pudo obtener el nombre completo de la clase.");
            return;
        }

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            System.out.println("⚠️ No se pudo obtener el documento.");
            return;
        }

        for (PsiClass psiClass : javaFile.getClasses()) {
            for (PsiMethod method : psiClass.getMethods()) {
                String key = className + "." + method.getName();
                System.out.println("🔍 Procesando método: " + key);

                Map<Integer, Map<String, Integer>> lineToErrors = errorMap.get(key);
                if (lineToErrors == null) {
                    System.out.println("❌ No se encontraron errores para este método.");
                    continue;
                }

                System.out.println("✅ Errores encontrados en líneas: " + lineToErrors.keySet());

                Map<PsiElement, Map<String, Integer>> groupedByTarget = new HashMap<>();

                for (Map.Entry<Integer, Map<String, Integer>> entry : lineToErrors.entrySet()) {
                    int line = entry.getKey();
                    Map<String, Integer> errors = entry.getValue();

                    if (line < 0 || line >= document.getLineCount()) {
                        System.out.println("⚠️ Línea inválida: " + line);
                        continue;
                    }

                    int offset = document.getLineStartOffset(line);
                    PsiElement element = file.findElementAt(offset);
                    if (element == null) {
                        System.out.println("⚠️ No se encontró PsiElement en línea " + line);
                        continue;
                    }

                    // Subir hasta el primer statement útil, si existe
                    PsiElement significant = element;
                    while (significant != null && !(significant instanceof PsiStatement || significant instanceof PsiTryStatement)) {
                        significant = significant.getParent();
                    }

                    PsiElement fallbackTarget = (significant != null) ? significant : element;

                    PsiTryStatement tryStmt = getEnclosingTryBlock(fallbackTarget);
                    PsiElement target = (tryStmt != null) ? tryStmt : fallbackTarget;

                    groupedByTarget.computeIfAbsent(target, k -> new HashMap<>());
                    Map<String, Integer> groupedMap = groupedByTarget.get(target);

                    for (Map.Entry<String, Integer> e : errors.entrySet()) {
                        groupedMap.merge(e.getKey(), e.getValue(), Integer::sum);
                    }
                }

                for (Map.Entry<PsiElement, Map<String, Integer>> entry : groupedByTarget.entrySet()) {
                    PsiElement target = entry.getKey();
                    Map<String, Integer> exceptionCounts = entry.getValue();

                    if (markedElements.contains(target)) continue;
                    markedElements.add(target);

                    int totalCount = exceptionCounts.values().stream().mapToInt(Integer::intValue).sum();

                    String tooltipBody = exceptionCounts.entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue() + " time" + (e.getValue() > 1 ? "s" : ""))
                            .collect(Collectors.joining("<br>"));
                    String finalTooltip = "<html><b>Error Detected:</b><br>" + tooltipBody + "</html>";

                    System.out.println("➡️ Marcando icono en: " + target.getText().replace("\n", " ") + " con " + totalCount + " errores");

                    LineMarkerInfo<PsiElement> markerInfo = new LineMarkerInfo<>(
                            target,
                            target.getTextRange(),
                            createIconWithText(totalCount),
                            Pass.LINE_MARKERS,
                            element -> finalTooltip,
                            null,
                            GutterIconRenderer.Alignment.LEFT
                    );
                    result.add(markerInfo);
                }
            }
        }
    }

    @Nullable
    private PsiTryStatement getEnclosingTryBlock(PsiElement element) {
        while (element != null && !(element instanceof PsiFile)) {
            if (element instanceof PsiTryStatement) {
                return (PsiTryStatement) element;
            }
            element = element.getParent();
        }
        return null;
    }

    private void fetchAndUpdateErrors() throws IOException {
        String queryJson = """
        {
          "size": 1000,
          "_source": ["stack_trace"],
          "query": {
            "match": {
              "level": "ERROR"
            }
          }
        }
        """;

        Map<String, Map<Integer, Map<String, Integer>>> newErrorMap = new ConcurrentHashMap<>();

        String responseBody = ElasticConnector.performSearch(queryJson);

        // Extraer línea de stack_trace con método y ubicación
        Pattern entryPattern = Pattern.compile("at ([\\w\\.$]+)\\.([\\w$<>]+)\\(([^:]+):(\\d+)\\)");
        // Extraer tipo de excepción desde el principio del stack
        Pattern exceptionPattern = Pattern.compile("^([\\w\\.$]+):?");

        String[] entries = responseBody.split("\"stack_trace\"\\s*:\\s*\"");
        for (String entry : entries) {
            String[] lines = entry.split("\\\\r?\\\\n");

            if (lines.length == 0) continue;

            // Detectar tipo de excepción de la primera línea
            String exceptionType = "Unknown";
            Matcher exceptionMatcher = exceptionPattern.matcher(lines[0]);
            if (exceptionMatcher.find()) {
                String fqName = exceptionMatcher.group(1);
                int lastDot = fqName.lastIndexOf(".");
                exceptionType = (lastDot != -1) ? fqName.substring(lastDot + 1) : fqName;
            }

            // Buscar la primera línea del stack que contenga clase/método/línea
            for (String line : lines) {
                Matcher m = entryPattern.matcher(line);
                if (m.find()) {
                    String className = m.group(1);
                    String methodName = m.group(2);
                    int lineNumber = Integer.parseInt(m.group(4)) - 1;

                    String key = className + "." + methodName;
                    newErrorMap
                            .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(lineNumber, k -> new ConcurrentHashMap<>())
                            .merge(exceptionType, 1, Integer::sum);

                    break;
                }
            }
        }

        errorMap.clear();
        errorMap.putAll(newErrorMap);
        System.out.println("Errores encontrados: " + errorMap);
    }
}

