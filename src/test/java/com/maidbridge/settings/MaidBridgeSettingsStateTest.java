//package com.example.maidbridge.settings;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.time.ZonedDateTime;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//public class MaidBridgeSettingsStateTest {
//
//    private MaidBridgeSettingsState settings;
//
//    @BeforeEach
//    public void setUp() {
//        settings = new MaidBridgeSettingsState();
//    }
//
//    @Test
//    public void testDefaultValues() {
//        // Valores por defecto iniciales
//        assertEquals("http://localhost:9200/", settings.getElasticsearchURL());
//        assertEquals("", settings.getUsername());
//        assertEquals("", settings.getPassword());
//        assertEquals("", settings.getIndex());
//        assertEquals("http://localhost:5601/", settings.getKibanaURL());
//        assertNotNull(settings.getStartTime());
//    }
//
//    @Test
//    public void testSetAndGetElasticsearchURL() {
//        String url = "http://example.com:9200/";
//        settings.setElasticsearchURL(url);
//        assertEquals(url, settings.getElasticsearchURL());
//    }
//
//    @Test
//    public void testSetAndGetUsername() {
//        String username = "testUser";
//        settings.setUsername(username);
//        assertEquals(username, settings.getUsername());
//    }
//
//    @Test
//    public void testSetAndGetPassword() {
//        String password = "testPass";
//        settings.setPassword(password);
//        assertEquals(password, settings.getPassword());
//    }
//
//    @Test
//    public void testSetAndGetIndex() {
//        String index = "myIndex";
//        settings.setIndex(index);
//        assertEquals(index, settings.getIndex());
//    }
//
//    @Test
//    public void testSetAndGetKibanaURL() {
//        String kibana = "http://kibana.example.com/";
//        settings.setKibanaURL(kibana);
//        assertEquals(kibana, settings.getKibanaURL());
//    }
//
//    @Test
//    public void testSetAndGetStartTime() {
//        ZonedDateTime now = ZonedDateTime.now();
//        settings.setStartTime(now);
//        assertEquals(now, settings.getStartTime());
//    }
//
//    @Test
//    public void testGetStartTimeHandlesNullOrEmpty() {
//        // Manipulamos directamente el estado para probar casos nulos o vacíos
//        MaidBridgeSettingsState.State state = new MaidBridgeSettingsState.State();
//
//        // Caso startTime null
//        state.startTime = null;
//        settings.loadState(state);
//        ZonedDateTime startTime = settings.getStartTime();
//        assertNotNull(startTime);
//        assertTrue(startTime.isBefore(ZonedDateTime.now().plusSeconds(1)));  // Debe ser menos que ahora
//
//        // Caso startTime vacío
//        state.startTime = "";
//        settings.loadState(state);
//        startTime = settings.getStartTime();
//        assertNotNull(startTime);
//        assertTrue(startTime.isBefore(ZonedDateTime.now().plusSeconds(1)));
//    }
//
//    @Test
//    public void testLoadStateAndGetStateConsistency() {
//        MaidBridgeSettingsState.State newState = new MaidBridgeSettingsState.State();
//        newState.elasticsearchURL = "http://new-url:9200/";
//        newState.username = "user2";
//        newState.password = "pass2";
//        newState.index = "index2";
//        newState.kibanaURL = "http://new-kibana/";
//        newState.startTime = ZonedDateTime.now().minusDays(1).toString();
//
//        settings.loadState(newState);
//
//        assertEquals(newState, settings.getState());
//        assertEquals(newState.elasticsearchURL, settings.getElasticsearchURL());
//        assertEquals(newState.username, settings.getUsername());
//        assertEquals(newState.password, settings.getPassword());
//        assertEquals(newState.index, settings.getIndex());
//        assertEquals(newState.kibanaURL, settings.getKibanaURL());
//        assertEquals(ZonedDateTime.parse(newState.startTime), settings.getStartTime());
//    }
//}
