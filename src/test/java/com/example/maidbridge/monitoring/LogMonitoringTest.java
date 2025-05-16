package com.example.maidbridge.monitoring;

import com.example.maidbridge.elastic.ElasticConnector;
import com.example.maidbridge.monitoring.Auxiliaries.LogData;
import com.example.maidbridge.monitoring.Auxiliaries.LogKey;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.Function;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogMonitoringTest {

    private PsiFile mockFile;
    private PsiMethodCallExpression mockLogCall;
    private PsiReferenceExpression mockMethodExpr;
    private PsiExpressionList mockArgList;
    private PsiExpression[] mockArgs;
    private PsiLiteralExpression mockLiteral;
    private LogMonitoring monitoring;

    @BeforeEach
    public void setup() {
        monitoring = new LogMonitoring();

        mockLogCall = mock(PsiMethodCallExpression.class);
        mockMethodExpr = mock(PsiReferenceExpression.class);
        mockArgList = mock(PsiExpressionList.class);
        mockLiteral = mock(PsiLiteralExpression.class);
        mockFile = mock(PsiFile.class);

        Project mockProject = mock(Project.class);
        when(mockFile.getProject()).thenReturn(mockProject);
        when(mockLogCall.getProject()).thenReturn(mockProject);

        when(mockLogCall.getMethodExpression()).thenReturn(mockMethodExpr);
        when(mockMethodExpr.getReferenceName()).thenReturn("info");

        mockArgs = new PsiExpression[]{mockLiteral};
        when(mockLogCall.getArgumentList()).thenReturn(mockArgList);
        when(mockArgList.getExpressions()).thenReturn(mockArgs);
        when(mockLiteral.getValue()).thenReturn("Test message");

        when(mockLogCall.getContainingFile()).thenReturn(mockFile);
        when(mockLogCall.getTextRange()).thenReturn(new TextRange(0, 10));
    }

    @Test
    public void testCollectSlowLineMarkers_ignoresWhenClassNotFound() {
        try (MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class)) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(mockFile)).thenReturn(null);

            List<LineMarkerInfo<?>> result = new ArrayList<>();
            monitoring.collectSlowLineMarkers(List.of(mockLogCall), result);

            assertTrue(result.isEmpty(), "No se debe generar ningún marcador si no se encuentra el nombre de clase");
        }
    }

    @Test
    public void testCountLogOccurrences_emptyResponse() throws Exception {
        PsiFile mockFile = mock(PsiFile.class);
        try (
                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)
        ) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(mockFile)).thenReturn("com.example.Dummy");
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn("""
                { "hits": { "hits": [] } }
            """);

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);
            Map<LogKey, LogData> result = (Map<LogKey, LogData>) method.invoke(null, mockFile);

            assertNotNull(result);
            assertTrue(result.isEmpty(), "Debe devolver un mapa vacío si no hay logs");
        }
    }

    @Test
    public void testNoMarkerCreatedIfLogDoesNotMatch() throws IOException {
        try (
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class)
        ) {
            // Simular nombre de clase y literal en código
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(mockFile)).thenReturn("com.example.MyClass");
            auxMock.when(() -> Auxiliaries.getStringLiteralValue(mockLiteral)).thenReturn("My log message");

            // JSON con mensaje que NO coincide con el del código
            String json = new JSONObject(Map.of(
                    "hits", Map.of("hits", List.of(
                            Map.of("_source", Map.of(
                                    "message", "Completely different message",
                                    "level", "INFO",
                                    "@timestamp", ZonedDateTime.now().toString()
                            ))
                    ))
            )).toString();
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(json);

            // Ejecutar lógica
            List<LineMarkerInfo<?>> result = new ArrayList<>();
            monitoring.collectSlowLineMarkers(List.of(mockLogCall), result);

            // Verificación: no debe haber ningún marcador
            assertTrue(result.isEmpty(), "No debe generarse marcador si no hay coincidencias de mensaje");
        }
    }

    @Test
    public void testGetLineMarkerInfoAlwaysReturnsNull() {
        assertNull(monitoring.getLineMarkerInfo(mock(PsiElement.class)), "getLineMarkerInfo siempre debe devolver null");
    }

    @Test
    public void testCountLogOccurrences_withVariousJson() throws Exception {
        PsiFile mockFile = mock(PsiFile.class);
        try (MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
             MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)) {

            auxMock.when(() -> Auxiliaries.getQualifiedClassName(mockFile)).thenReturn("com.example.TestClass");

            // Respuesta JSON con 2 logs diferentes
            String json = """
        {
          "hits": {
            "hits": [
              { "_source": { "message": "msg1", "level": "INFO", "@timestamp": "2025-01-01T10:00:00Z" } },
              { "_source": { "message": "msg2", "level": "ERROR", "@timestamp": "2025-01-02T11:00:00Z" } }
            ]
          }
        }
        """;

            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(json);

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);
            Map<LogKey, LogData> result = (Map<LogKey, LogData>) method.invoke(null, mockFile);

            assertEquals(2, result.size());
            assertTrue(result.keySet().stream().anyMatch(k -> k.level.equals("INFO")));
            assertTrue(result.keySet().stream().anyMatch(k -> k.level.equals("ERROR")));
        }
    }

    @Test
    public void testCollectSlowLineMarkers_ignoresInvalidLogCalls() {
        PsiMethodCallExpression invalidLogCall = mock(PsiMethodCallExpression.class);
        when(invalidLogCall.getMethodExpression()).thenReturn(null);

        List<LineMarkerInfo<?>> markers = new ArrayList<>();
        monitoring.collectSlowLineMarkers(List.of(invalidLogCall), markers);

        assertTrue(markers.isEmpty(), "No debe generar marcadores para llamadas inválidas");
    }

    @Test
    public void testCountLogOccurrences_handlesMalformedJsonGracefully() throws Exception {
        PsiFile mockFile = mock(PsiFile.class);
        try (MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
             MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)) {

            auxMock.when(() -> Auxiliaries.getQualifiedClassName(mockFile)).thenReturn("com.example.TestClass");

            String malformedJson = "{ invalid json }";

            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(malformedJson);

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> {
                method.invoke(null, mockFile);
            });
        }
    }


}
