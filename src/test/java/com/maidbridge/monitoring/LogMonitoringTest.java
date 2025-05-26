package com.maidbridge.monitoring;

import com.maidbridge.elastic.ElasticConnector;
import com.maidbridge.monitoring.Auxiliaries.LogData;
import com.maidbridge.monitoring.Auxiliaries.LogKey;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogMonitoringTest {

    private static final String TEST_CLASS = "com.example.MyClass";
    private static final String TEST_MESSAGE = "My log message";

    private PsiFile mockFile;
    private PsiMethodCallExpression mockLogCall;
    private PsiReferenceExpression mockMethodExpr;
    private PsiExpressionList mockArgList;
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

        when(mockLogCall.getArgumentList()).thenReturn(mockArgList);
        when(mockArgList.getExpressions()).thenReturn(new PsiExpression[]{mockLiteral});
        when(mockLiteral.getValue()).thenReturn(TEST_MESSAGE);

        when(mockLogCall.getContainingFile()).thenReturn(mockFile);
        when(mockLogCall.getTextRange()).thenReturn(new TextRange(0, 10));
    }

    @Test
    public void testGetLineMarkerInfoAlwaysReturnsNull() {
        assertNull(monitoring.getLineMarkerInfo(mock(PsiElement.class)));
    }

    @Test
    public void testCountLogOccurrences_returnsEmptyForEmptyElasticResponse() throws Exception {
        PsiFile dummyFile = mock(PsiFile.class);
        try (
                MockedStatic<ElasticConnector> elastic = mockStatic(ElasticConnector.class);
                MockedStatic<Auxiliaries> aux = mockStatic(Auxiliaries.class)
        ) {
            aux.when(() -> Auxiliaries.getQualifiedClassName(dummyFile)).thenReturn(TEST_CLASS);
            elastic.when(() -> ElasticConnector.performSearch(any())).thenReturn("{ \"hits\": { \"hits\": [] } }");

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);

            Map<LogKey, LogData> result = (Map<LogKey, LogData>) method.invoke(null, dummyFile);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testCountLogOccurrences_withValidEntries() throws Exception {
        PsiFile dummyFile = mock(PsiFile.class);
        try (
                MockedStatic<ElasticConnector> elastic = mockStatic(ElasticConnector.class);
                MockedStatic<Auxiliaries> aux = mockStatic(Auxiliaries.class)
        ) {
            aux.when(() -> Auxiliaries.getQualifiedClassName(dummyFile)).thenReturn(TEST_CLASS);

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
            elastic.when(() -> ElasticConnector.performSearch(any())).thenReturn(json);

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);

            Map<LogKey, LogData> result = (Map<LogKey, LogData>) method.invoke(null, dummyFile);

            assertEquals(2, result.size());
            assertTrue(result.containsKey(new LogKey("msg1", "INFO")));
            assertTrue(result.containsKey(new LogKey("msg2", "ERROR")));
        }
    }


    @Test
    public void testCountLogOccurrences_handlesMalformedJson() throws Exception {
        PsiFile dummyFile = mock(PsiFile.class);
        try (
                MockedStatic<ElasticConnector> elastic = mockStatic(ElasticConnector.class);
                MockedStatic<Auxiliaries> aux = mockStatic(Auxiliaries.class)
        ) {
            aux.when(() -> Auxiliaries.getQualifiedClassName(dummyFile)).thenReturn(TEST_CLASS);
            elastic.when(() -> ElasticConnector.performSearch(any())).thenReturn("malformed_json");

            var method = LogMonitoring.class.getDeclaredMethod("countLogOccurrences", PsiFile.class);
            method.setAccessible(true);

            assertThrows(Exception.class, () -> method.invoke(null, dummyFile));
        }
    }
}
