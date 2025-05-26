package com.maidbridge.monitoring;

import com.maidbridge.elastic.ElasticConnector;
import com.maidbridge.monitoring.ErrorMonitoring;
import com.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import static com.maidbridge.monitoring.Auxiliaries.*;

public class ErrorMonitoringTest {

    private ErrorMonitoring errorMonitoring;

    @BeforeEach
    public void setup() {
        errorMonitoring = new ErrorMonitoring();
    }

    @Test
    public void testGetLineMarkerInfoAlwaysReturnsNull() {
        PsiElement element = mock(PsiElement.class);
        assertNull(errorMonitoring.getLineMarkerInfo(element), "getLineMarkerInfo debe devolver null siempre");
    }

    @Test
    public void testCollectSlowLineMarkersWithEmptyInput() {
        List<PsiElement> elements = Collections.emptyList();
        Collection<LineMarkerInfo<?>> result = new ArrayList<>();

        errorMonitoring.collectSlowLineMarkers(elements, result);
        assertTrue(result.isEmpty(), "No deben generarse marcadores para lista vacía");
    }

    @Test
    public void testCountErrorOccurrencesEmptyElasticResponse() {
        try (
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
        ) {
            String jsonResponse = new JSONObject()
                    .put("hits", new JSONObject().put("hits", new ArrayList<>()))
                    .toString();

            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(jsonResponse);

            MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
            when(settings.getErrorTimeRange()).thenReturn("24h");
            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);

            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Debe retornar un mapa vacío si no hay logs de error");
        }
    }

    @Test
    public void testCountErrorOccurrencesMalformedJsonHandled() {
        try (
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
        ) {
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn("{{malformed_json");

            MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
            when(settings.getErrorTimeRange()).thenReturn("24h");
            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);

            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Debe manejar errores de parseo sin lanzar excepción");
        }
    }

    @Test
    public void testCountErrorOccurrencesIgnoresOldErrors() {
        ZonedDateTime now = ZonedDateTime.now();

        String logJson = String.format("""
        {
          "hits": {
            "hits": [
              {
                "_source": {
                  "@timestamp": "%s",
                  "stack_trace": "java.lang.Exception\\n at com.example.Foo.bar(Foo.java:42)",
                  "message": "threw exception [IllegalStateException]"
                }
              }
            ]
          }
        }
        """, now.minusDays(10));

        try (
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
        ) {
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(logJson);

            MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
            when(settings.getErrorTimeRange()).thenReturn("7d");
            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);

            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Errores fuera del rango no deben considerarse");
        }
    }

    @Test
    public void testCountErrorOccurrencesSkipsInvalidStackTrace() {
        ZonedDateTime now = ZonedDateTime.now();

        String logJson = String.format("""
        {
          "hits": {
            "hits": [
              {
                "_source": {
                  "@timestamp": "%s",
                  "stack_trace": "",
                  "message": "threw exception [NullPointerException]"
                }
              }
            ]
          }
        }
        """, now);

        try (
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
        ) {
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(logJson);

            MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
            when(settings.getErrorTimeRange()).thenReturn("24h");
            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);

            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
            assertNotNull(result);
            assertTrue(result.isEmpty(), "Stack traces vacíos deben ignorarse");
        }
    }
}
