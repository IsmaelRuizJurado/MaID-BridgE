package com.maidbridge.monitoring.errortables;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maidbridge.elastic.ElasticConnector;
import com.maidbridge.monitoring.Auxiliaries;
import com.intellij.psi.PsiFile;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.Map;

public class TotalErrorsTableCacheTest {

    @Test
    public void testUpdateAndGetAll() {
        Map<String, Integer> data = Map.of("classA", 5, "classB", 3);
        TotalErrorsTableCache.update(data);

        Map<String, Integer> cached = TotalErrorsTableCache.getAll();
        assertEquals(2, cached.size());
        assertEquals(5, cached.get("classA"));
        assertEquals(3, cached.get("classB"));
    }

    @Test
    public void testCountErrorOccurrencesByClass_withNullClassName() {
        PsiFile file = mock(PsiFile.class);

        try (MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class)) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(file)).thenReturn(null);

            Map<String, Integer> result = TotalErrorsTableCache.countErrorOccurrencesByClass(file);
            assertTrue(result.isEmpty(), "Debe devolver mapa vacío si nombre de clase es null");
        }
    }

    @Test
    public void testCountErrorOccurrencesByClass_emptyResponse() throws Exception {
        PsiFile file = mock(PsiFile.class);
        String className = "com.example.TestClass";

        try (
                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)
        ) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(file)).thenReturn(className);
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn("""
                {
                  "hits": {
                    "hits": []
                  }
                }
                """);

            Map<String, Integer> result = TotalErrorsTableCache.countErrorOccurrencesByClass(file);
            assertTrue(result.isEmpty(), "Debe devolver mapa vacío si no hay errores");
        }
    }

    @Test
    public void testCountErrorOccurrencesByClass_withMatchingErrors() throws Exception {
        PsiFile file = mock(PsiFile.class);
        String className = "com.example.MyClass";

        String jsonResponse = new JSONObject()
                .put("hits", new JSONObject()
                        .put("hits", new org.json.JSONArray()
                                .put(new JSONObject().put("_source", new JSONObject().put("stack_trace", "at com.example.MyClass.method(MyClass.java:10)")))
                                .put(new JSONObject().put("_source", new JSONObject().put("stack_trace", "at com.example.OtherClass.method(OtherClass.java:20)")))
                                .put(new JSONObject().put("_source", new JSONObject().put("stack_trace", "at com.example.MyClass.anotherMethod(MyClass.java:15)")))
                        )
                ).toString();

        try (
                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)
        ) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(file)).thenReturn(className);
            auxMock.when(() -> Auxiliaries.extractClassNameFromStackTrace(anyString()))
                    .thenCallRealMethod(); // Usa implementación real para el test

            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenReturn(jsonResponse);

            Map<String, Integer> result = TotalErrorsTableCache.countErrorOccurrencesByClass(file);

            assertEquals(1, result.size());
            assertEquals(2, result.get(className), "Debe contar dos errores que pertenecen a la clase");
        }
    }

    @Test
    public void testCountErrorOccurrencesByClass_handlesIOException() throws Exception {
        PsiFile file = mock(PsiFile.class);
        String className = "com.example.TestClass";

        try (
                MockedStatic<Auxiliaries> auxMock = mockStatic(Auxiliaries.class);
                MockedStatic<ElasticConnector> elasticMock = mockStatic(ElasticConnector.class)
        ) {
            auxMock.when(() -> Auxiliaries.getQualifiedClassName(file)).thenReturn(className);
            elasticMock.when(() -> ElasticConnector.performSearch(any())).thenThrow(new IOException("Simulated IO exception"));

            Map<String, Integer> result = TotalErrorsTableCache.countErrorOccurrencesByClass(file);
            assertTrue(result.isEmpty(), "Debe devolver mapa vacío si ocurre IOException");
        }
    }
}
