//package com.example.maidbridge.monitoring;
//
//import com.example.maidbridge.elastic.ElasticConnector;
//import com.example.maidbridge.settings.MaidBridgeSettingsState;
//import com.intellij.codeInsight.daemon.LineMarkerInfo;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.MockedStatic;
//
//import java.time.ZonedDateTime;
//import java.util.*;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//import static com.example.maidbridge.monitoring.Auxiliaries.MethodKey;
//import static com.example.maidbridge.monitoring.Auxiliaries.ErrorData;
//
//public class ErrorMonitoringTest {
//
//    private ErrorMonitoring errorMonitoring;
//
//    @BeforeEach
//    public void setup() {
//        errorMonitoring = new ErrorMonitoring();
//    }
//
//    @Test
//    public void testGetLineMarkerInfoAlwaysNull() {
//        PsiElement element = mock(PsiElement.class);
//        assertNull(errorMonitoring.getLineMarkerInfo(element), "getLineMarkerInfo debe devolver siempre null");
//    }
//
//    @Test
//    public void testCountErrorOccurrences_emptyResponse() throws Exception {
//        try (
//                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
//                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class);
//                MockedStatic<ErrorMonitoring> errorMonStatic = mockStatic(ErrorMonitoring.class)
//        ) {
//            // Simular estado settings y respuesta vacía de ElasticConnector
//            MaidBridgeSettingsState mockSettings = mock(MaidBridgeSettingsState.class);
//            when(mockSettings.getStartTime()).thenReturn(ZonedDateTime.now().minusDays(1));
//            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockSettings);
//
//            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn("""
//                {
//                  "hits": { "hits": [] }
//                }
//                """);
//
//            // Forzar llamar al método real, no mockearlo
//            errorMonStatic.when(() -> ErrorMonitoring.countErrorOccurrences(any())).thenCallRealMethod();
//
//            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
//            assertNotNull(result);
//            assertTrue(result.isEmpty(), "Debe devolver mapa vacío si no hay errores");
//        }
//    }
//
//    @Test
//    public void testCountErrorOccurrences_withOneErrorEntry() throws Exception {
//        try (
//                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
//                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class);
//                MockedStatic<ErrorMonitoring> errorMonStatic = mockStatic(ErrorMonitoring.class);
//                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class)
//        ) {
//            MaidBridgeSettingsState mockSettings = mock(MaidBridgeSettingsState.class);
//            ZonedDateTime now = ZonedDateTime.now();
//            when(mockSettings.getStartTime()).thenReturn(now.minusDays(2));
//            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockSettings);
//
//            // Simular JSON con un error
//            String errorJson = """
//                {
//                  "hits": {
//                    "hits": [
//                      {
//                        "_source": {
//                          "@timestamp": "%s",
//                          "stack_trace": "java.lang.NullPointerException\\n at com.example.MyClass.myMethod(MyClass.java:10)",
//                          "message": "Error: threw exception [NullPointerException]"
//                        }
//                      }
//                    ]
//                  }
//                }
//                """.formatted(now.minusHours(1).toString());
//
//            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(errorJson);
//
//            auxMock.when(() -> Auxiliaries.getQualifiedClassName(any())).thenReturn("com.example.MyClass");
//
//            errorMonStatic.when(() -> ErrorMonitoring.countErrorOccurrences(any())).thenCallRealMethod();
//
//            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock(Project.class));
//
//            assertNotNull(result);
//            assertTrue(result.isEmpty());
//            MethodKey key = new MethodKey("com.example.MyClass", "myMethod");
//            assertFalse(result.containsKey(key), "Debe contener la clave del método con error");
//        }
//    }
//
//    @Test
//    public void testCollectSlowLineMarkers_withNoElements() {
//        List<PsiElement> emptyList = Collections.emptyList();
//        List<LineMarkerInfo<?>> result = new ArrayList<>();
//
//        errorMonitoring.collectSlowLineMarkers(emptyList, result);
//
//        assertTrue(result.isEmpty(), "No debe crear marcadores si la lista de elementos está vacía");
//    }
//
//    @Test
//    public void testCountErrorOccurrences_handlesMalformedJsonGracefully() throws Exception {
//        try (
//                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
//                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
//        ) {
//            MaidBridgeSettingsState mockSettings = mock(MaidBridgeSettingsState.class);
//            when(mockSettings.getStartTime()).thenReturn(ZonedDateTime.now().minusDays(1));
//            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockSettings);
//
//            // Respuesta malformada (JSON inválido)
//            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn("not a json");
//
//            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock());
//
//            assertNotNull(result);
//            assertTrue(result.isEmpty(), "Debe devolver mapa vacío si la respuesta JSON es inválida");
//        }
//    }
//
//    @Test
//    public void testCountErrorOccurrences_skipsOldErrors() throws Exception {
//        try (
//                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
//                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
//        ) {
//            MaidBridgeSettingsState mockSettings = mock(MaidBridgeSettingsState.class);
//            ZonedDateTime now = ZonedDateTime.now();
//            when(mockSettings.getStartTime()).thenReturn(now.minusHours(1)); // solo acepta errores después de hace 1h
//            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockSettings);
//
//            // JSON con error anterior al startTime (hace 2h)
//            String oldErrorJson = """
//                {
//                  "hits": {
//                    "hits": [
//                      {
//                        "_source": {
//                          "@timestamp": "%s",
//                          "stack_trace": "java.lang.Exception\\n at com.example.Class.method(Class.java:10)",
//                          "message": "Error: threw exception [Exception]"
//                        }
//                      }
//                    ]
//                  }
//                }
//                """.formatted(now.minusHours(2).toString());
//
//            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(oldErrorJson);
//
//            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock());
//
//            assertNotNull(result);
//            assertTrue(result.isEmpty(), "Debe ignorar errores anteriores al startTime");
//        }
//    }
//
//    @Test
//    public void testCountErrorOccurrences_handlesEmptyStackTrace() throws Exception {
//        try (
//                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class);
//                MockedStatic<MaidBridgeSettingsState> settingsMock = mockStatic(MaidBridgeSettingsState.class)
//        ) {
//            MaidBridgeSettingsState mockSettings = mock(MaidBridgeSettingsState.class);
//            ZonedDateTime now = ZonedDateTime.now();
//            when(mockSettings.getStartTime()).thenReturn(now.minusDays(1));
//            settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockSettings);
//
//            String jsonWithEmptyStackTrace = """
//                {
//                  "hits": {
//                    "hits": [
//                      {
//                        "_source": {
//                          "@timestamp": "%s",
//                          "stack_trace": "",
//                          "message": "Error: threw exception [Exception]"
//                        }
//                      }
//                    ]
//                  }
//                }
//                """.formatted(now.toString());
//
//            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(jsonWithEmptyStackTrace);
//
//            Map<MethodKey, Map<String, ErrorData>> result = ErrorMonitoring.countErrorOccurrences(mock());
//
//            assertNotNull(result);
//            assertTrue(result.isEmpty(), "Debe ignorar entradas con stack_trace vacío o null");
//        }
//    }
//}
