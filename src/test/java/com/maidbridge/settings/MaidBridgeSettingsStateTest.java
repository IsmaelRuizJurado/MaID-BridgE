package com.maidbridge.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MaidBridgeSettingsStateTest {

    private MaidBridgeSettingsState settings;

    @BeforeEach
    public void setup() {
        settings = new MaidBridgeSettingsState();
    }

    @Test
    public void testDefaultValues() {
        MaidBridgeSettingsState.State state = settings.getState();
        assertEquals("http://localhost:9200/", state.elasticsearchURL);
        assertEquals("", state.username);
        assertEquals("", state.password);
        assertEquals("", state.index);
        assertEquals("http://localhost:5601/", state.kibanaURL);
        assertEquals("24h", state.errorTimeRange);
        assertEquals("24h", state.logTimeRange);
        assertNotNull(state.errorCustomTime);
        assertNotNull(state.logCustomTime);
    }

    @Test
    public void testSettersAndGetters() {
        settings.setElasticsearchURL("http://remote:9200");
        settings.setUsername("admin");
        settings.setPassword("secret");
        settings.setIndex("prod-logs");
        settings.setKibanaURL("http://kibana-host");
        settings.setErrorTimeRange("7d");
        settings.setLogTimeRange("30d");

        ZonedDateTime now = ZonedDateTime.now();
        settings.setErrorCustomTime(now);
        settings.setLogCustomTime(now.minusDays(1));

        assertEquals("http://remote:9200", settings.getElasticsearchURL());
        assertEquals("admin", settings.getUsername());
        assertEquals("secret", settings.getPassword());
        assertEquals("prod-logs", settings.getIndex());
        assertEquals("http://kibana-host", settings.getKibanaURL());
        assertEquals("7d", settings.getErrorTimeRange());
        assertEquals("30d", settings.getLogTimeRange());
        assertEquals(now, settings.getErrorCustomTime());
        assertEquals(now.minusDays(1), settings.getLogCustomTime());
    }

    @Test
    public void testSetAndGetElasticsearchURL() {
        String url = "http://example.com:9200/";
        settings.setElasticsearchURL(url);
        assertEquals(url, settings.getElasticsearchURL());
    }

    @Test
    public void testSetAndGetUsername() {
        String username = "testUser";
        settings.setUsername(username);
        assertEquals(username, settings.getUsername());
    }

    @Test
    public void testSetAndGetPassword() {
        String password = "testPass";
        settings.setPassword(password);
        assertEquals(password, settings.getPassword());
    }

    @Test
    public void testSetAndGetIndex() {
        String index = "myIndex";
        settings.setIndex(index);
        assertEquals(index, settings.getIndex());
    }
    @Test
    public void testSetAndGetKibanaURL() {
        String kibana = "http://kibana.example.com/";
        settings.setKibanaURL(kibana);
        assertEquals(kibana, settings.getKibanaURL());
    }


    @Test
    public void testLoadStateCopiesCorrectly() {
        MaidBridgeSettingsState.State newState = new MaidBridgeSettingsState.State();
        newState.elasticsearchURL = "http://newhost:9200";
        newState.username = "tester";
        newState.password = "pwd";
        newState.index = "test-index";
        newState.kibanaURL = "http://kibana-test";
        newState.errorTimeRange = "custom";
        newState.logTimeRange = "custom";
        newState.errorCustomTime = "2025-05-25T18:21:55.107409Z";
        newState.logCustomTime = "2025-05-26T18:21:55.107409Z";

        settings.loadState(newState);

        assertEquals("http://newhost:9200", settings.getElasticsearchURL());
        assertEquals("tester", settings.getUsername());
        assertEquals("pwd", settings.getPassword());
        assertEquals("test-index", settings.getIndex());
        assertEquals("http://kibana-test", settings.getKibanaURL());
        assertEquals("custom", settings.getErrorTimeRange());
        assertEquals("custom", settings.getLogTimeRange());
        assertEquals(ZonedDateTime.parse("2025-05-25T18:21:55.107409Z"), settings.getErrorCustomTime());
        assertEquals(ZonedDateTime.parse("2025-05-26T18:21:55.107409Z"), settings.getLogCustomTime());
    }

    @Test
    public void testErrorCustomTimeFallbackIfInvalid() {
        settings.getState().errorCustomTime = "invalid-date";
        ZonedDateTime fallback = settings.getErrorCustomTime();
        assertNotNull(fallback);
        assertTrue(fallback.isBefore(ZonedDateTime.now()));
    }

    @Test
    public void testLogCustomTimeFallbackIfInvalid() {
        settings.getState().logCustomTime = "invalid-date";
        ZonedDateTime fallback = settings.getLogCustomTime();
        assertNotNull(fallback);
        assertTrue(fallback.isBefore(ZonedDateTime.now()));
    }
}
