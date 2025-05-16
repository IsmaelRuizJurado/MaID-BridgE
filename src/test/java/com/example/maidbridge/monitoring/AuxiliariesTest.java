package com.example.maidbridge.monitoring;

import com.example.maidbridge.monitoring.Auxiliaries.*;
import com.example.maidbridge.settings.MaidBridgeSettingsState;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.intellij.psi.PsiLiteralExpression;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuxiliariesTest {

    @Test
    public void testGetColorForLevel() {
        assertEquals(java.awt.Color.GRAY, Auxiliaries.getColorForLevel("INFO"));
        assertEquals(java.awt.Color.ORANGE, Auxiliaries.getColorForLevel("WARN"));
        assertEquals(java.awt.Color.ORANGE, Auxiliaries.getColorForLevel("WARNING"));
        assertEquals(java.awt.Color.RED, Auxiliaries.getColorForLevel("ERROR"));
        assertEquals(java.awt.Color.RED, Auxiliaries.getColorForLevel("SEVERE"));
        assertEquals(java.awt.Color.BLUE, Auxiliaries.getColorForLevel("UNKNOWN"));
    }

    @Test
    public void testGetStringLiteralValueWithPsiLiteralExpression() {
        PsiLiteralExpression mockExpr = mock(PsiLiteralExpression.class);
        when(mockExpr.getValue()).thenReturn("Hola Mundo");

        String result = Auxiliaries.getStringLiteralValue(mockExpr);
        assertEquals("Hola Mundo", result);
    }

    @Test
    public void testBuildKibanaUrlLog() {
        MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
        when(settings.getKibanaURL()).thenReturn("http://localhost:5601");
        when(settings.getIndex()).thenReturn("logs-*");

        try (MockedStatic<MaidBridgeSettingsState> mock = mockStatic(MaidBridgeSettingsState.class)) {
            mock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);
            String url = Auxiliaries.buildKibanaUrlLog("MyLogger", "Something happened", "INFO");

            assertTrue(url.contains("MyLogger"));
            assertTrue(url.contains("Something+happened"));
            assertTrue(url.contains("INFO"));
        }
    }

    @Test
    public void testBuildKibanaUrlError() {
        MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
        when(settings.getKibanaURL()).thenReturn("http://localhost:5601");
        when(settings.getIndex()).thenReturn("errors-*");

        try (MockedStatic<MaidBridgeSettingsState> mock = mockStatic(MaidBridgeSettingsState.class)) {
            mock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);
            String url = Auxiliaries.buildKibanaUrlError("NullPointerException", "doSomething");

            assertTrue(url.contains("NullPointerException"));
            assertTrue(url.contains("doSomething"));
        }
    }

    @Test
    public void testExtractExceptionType() {
        String trace = "java.lang.NullPointerException: something went wrong\n\tat MyClass.method(MyClass.java:42)";
        assertEquals("NullPointerException", Auxiliaries.extractExceptionType(trace));

        assertEquals("Unknown", Auxiliaries.extractExceptionType(""));
    }

    @Test
    public void testExtractLineNumberFromStackTrace() {
        String trace = "at com.example.MyClass.method(MyClass.java:123)";
        int line = Auxiliaries.extractLineNumberFromStackTrace(trace, "fallback");
        assertEquals(122, line); // -1 porque IDEA trabaja con 0-index

        String noLine = "no stack trace info here";
        assertEquals(-1, Auxiliaries.extractLineNumberFromStackTrace(noLine, "fallback"));
    }

    @Test
    public void testExtractClassNameFromStackTrace() {
        String trace = """
                at java.base/java.util.ArrayList.get(ArrayList.java:427)
                at com.example.MyClass.method(MyClass.java:42)
                """;

        String result = Auxiliaries.extractClassNameFromStackTrace(trace);
        assertEquals("com.example.MyClass", result);

        String noMatch = "at java.lang.Thread.run(Thread.java:750)";
        assertNull(Auxiliaries.extractClassNameFromStackTrace(noMatch));
    }

    @Test
    public void testParseStackTrace() {
        String trace = "at com.example.MyClass.myMethod(MyClass.java:20)\n" +
                "at java.base/java.util.ArrayList.get(ArrayList.java:427)";
        Map<String, com.intellij.psi.PsiFile> fqcnMap = Map.of("com.example.MyClass", mock(com.intellij.psi.PsiFile.class));

        Auxiliaries.StackTraceInfo info = Auxiliaries.parseStackTrace(trace, fqcnMap);

        assertNotNull(info);
        assertEquals("com.example.MyClass", info.fqcn);
        assertEquals("myMethod", info.methodName);
        assertEquals(19, info.line);
        assertEquals("java", info.exceptionType);
    }

    @Test
    public void testLogKeyEquality() {
        LogKey key1 = new LogKey("hello", "INFO");
        LogKey key2 = new LogKey("hello", "INFO");
        LogKey key3 = new LogKey("other", "ERROR");

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testErrorDataCreation() {
        ZonedDateTime now = ZonedDateTime.now();
        ErrorData errorData = new ErrorData(5, 2, "IOException", "stackTrace", now);

        assertEquals(5, errorData.count);
        assertEquals(2, errorData.countLast24h);
        assertEquals("IOException", errorData.type);
        assertEquals("stackTrace", errorData.stackTrace);
        assertEquals(now, errorData.errorTime);
    }

    @Test
    public void testMethodKeyEquality() {
        MethodKey key1 = new MethodKey("A.B.C", "run");
        MethodKey key2 = new MethodKey("A.B.C", "run");
        MethodKey key3 = new MethodKey("A.B.D", "other");

        assertEquals(key1, key2);
        assertNotEquals(key1, key3);
        assertEquals(key1.hashCode(), key2.hashCode());
    }
}
