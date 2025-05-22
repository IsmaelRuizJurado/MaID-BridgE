package com.maidbridge.monitoring.errortables;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.maidbridge.elastic.ElasticConnector;
import com.maidbridge.monitoring.errortables.ErrorsTablePanel.ErrorsTableEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

public class ErrorsTableCacheTest {

    private MockedStatic<ElasticConnector> elasticMock;

    @BeforeEach
    public void setUp() {
        elasticMock = mockStatic(ElasticConnector.class);
    }

    @AfterEach
    public void tearDown() {
        elasticMock.close();
    }

    @Test
    public void testUpdateAndGetAll() {
        ErrorsTableEntry entry = new ErrorsTableEntry("MyClass", "NullPointerException", 42, "Some debug message", 3);
        ErrorsTableCache.update(List.of(entry));
        List<ErrorsTableEntry> cached = ErrorsTableCache.getAll();

        assertEquals(1, cached.size());
        ErrorsTableEntry cachedEntry = cached.get(0);
        assertEquals("MyClass", cachedEntry.className);
        assertEquals("NullPointerException", cachedEntry.errorType);
        assertEquals(42, cachedEntry.lineNumber);
        assertEquals("Some debug message", cachedEntry.lineContent);
        assertEquals(3, cachedEntry.occurrences);
    }

    @Test
    public void testComputeDetailedErrorData_withValidResponse() {
        // Simular respuesta JSON con 1 error y 1 debug asociado en mismo thread
        String jsonResponse = """
        {
          "hits": {
            "hits": [
              {
                "_source": {
                  "level": "DEBUG",
                  "thread_name": "thread1",
                  "@timestamp": "2025-05-15T12:00:00Z",
                  "message": "Some debug message Exception: details here"
                }
              },
              {
                "_source": {
                  "level": "ERROR",
                  "thread_name": "thread1",
                  "@timestamp": "2025-05-15T12:01:00Z",
                  "stack_trace": "java.lang.NullPointerException\\n\tat com.example.MyClass.method(MyClass.java:42)",
                  "message": "Exception thrown"
                }
              }
            ]
          }
        }
        """;

        elasticMock.when(() -> ElasticConnector.performSearch(anyString())).thenReturn(jsonResponse);

        // Ejecutar método (no lanza excepción)
        assertDoesNotThrow(() -> ErrorsTableCache.computeDetailedErrorData());

        // Tras ejecución, el caché no debe estar vacío
        List<ErrorsTableEntry> entries = ErrorsTableCache.getAll();
        assertFalse(entries.isEmpty(), "Cache debe contener al menos una entrada");
        ErrorsTableEntry e = entries.get(0);
        assertEquals("MyClass", e.className);
        assertEquals("NullPointerException", e.errorType);
        assertEquals(41, e.lineNumber);
        assertTrue(e.lineContent.contains("details here"));
        assertEquals(1, e.occurrences);
    }

    @Test
    public void testComputeDetailedErrorData_handlesEmptyResponse() {
        String emptyResponse = """
        {
          "hits": {
            "hits": []
          }
        }
        """;

        elasticMock.when(() -> ElasticConnector.performSearch(anyString())).thenReturn(emptyResponse);

        ErrorsTableCache.update(List.of(new ErrorsTableEntry("Dummy", "Exception", 1, "msg", 1)));

        ErrorsTableCache.computeDetailedErrorData();

        // Como la respuesta está vacía, el caché debería vaciarse
        List<ErrorsTableEntry> entries = ErrorsTableCache.getAll();
        assertTrue(entries.isEmpty(), "Cache debe estar vacía después de respuesta sin hits");
    }

    @Test
    public void testComputeDetailedErrorData_handlesMalformedJson() {
        String malformed = "{ this is not valid JSON }";

        elasticMock.when(() -> ElasticConnector.performSearch(anyString())).thenReturn(malformed);

        // El método no debe lanzar excepción, aunque la respuesta sea inválida
        assertDoesNotThrow(() -> ErrorsTableCache.computeDetailedErrorData());

        // El caché debería limpiarse o quedar vacío
        assertTrue(ErrorsTableCache.getAll().isEmpty());
    }
}
