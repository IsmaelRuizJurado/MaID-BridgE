package com.maidbridge.elastic;

import com.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import org.apache.http.HttpEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ElasticConnectorTest {

    private static MockedStatic<MaidBridgeSettingsState> settingsMock;

    @BeforeEach
    public void setUp() {
        // Mock de la configuración
        MaidBridgeSettingsState settings = mock(MaidBridgeSettingsState.class);
        when(settings.getUsername()).thenReturn("testUser");
        when(settings.getPassword()).thenReturn("testPass");
        when(settings.getElasticsearchURL()).thenReturn("http://localhost:9200");
        when(settings.getIndex()).thenReturn("test-index");

        settingsMock = mockStatic(MaidBridgeSettingsState.class);
        settingsMock.when(MaidBridgeSettingsState::getInstance).thenReturn(settings);
    }

    @AfterEach
    public void tearDown() throws Exception {
        settingsMock.close();
        ElasticConnector.close();
    }

    @Test
    public void testGetClientReturnsSingleton() throws Exception {
        RestClient client1 = ElasticConnector.getClient();
        RestClient client2 = ElasticConnector.getClient();
        assertSame(client1, client2, "Debe devolver la misma instancia (singleton)");
    }

    @Test
    public void testCloseResetsClient() throws Exception {
        ElasticConnector.getClient();
        ElasticConnector.close();

        Field clientField = ElasticConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        assertNull(clientField.get(null), "El cliente debe quedar en null tras close()");
    }

    @Test
    public void testPerformSearchReturnsResponse() throws Exception {
        // Mock del cliente y respuesta
        RestClient mockClient = mock(RestClient.class);
        Response mockResponse = mock(Response.class);

        String fakeJson = "{\"hits\":[]}";
        ByteArrayInputStream responseStream = new ByteArrayInputStream(fakeJson.getBytes());
        HttpEntity mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContent()).thenReturn(responseStream);
        when(mockResponse.getEntity()).thenReturn(mockEntity);
        when(mockClient.performRequest(any(Request.class))).thenReturn(mockResponse);

        // Inyectar cliente simulado en ElasticConnector
        Field clientField = ElasticConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(null, mockClient);

        String response = ElasticConnector.performSearch("{ \"query\": { \"match_all\": {} } }");

        assertEquals(fakeJson, response, "Debe devolver el JSON de respuesta simulado");
    }

    @Test
    public void testNotifyEmptyResponseOnlyOnce() {
        NotificationGroup mockGroup = mock(NotificationGroup.class);
        Notification mockNotification = mock(Notification.class);

        when(mockGroup.createNotification(anyString(), any(NotificationType.class))).thenReturn(mockNotification);

        try (MockedStatic<NotificationGroupManager> groupManagerMock = mockStatic(NotificationGroupManager.class)) {
            NotificationGroupManager mockManager = mock(NotificationGroupManager.class);
            groupManagerMock.when(NotificationGroupManager::getInstance).thenReturn(mockManager);
            when(mockManager.getNotificationGroup("MaID-BridgE Notification Group")).thenReturn(mockGroup);

            ElasticConnector.emptyNotificationShown = false;

            ElasticConnector.notifyEmptyResponse(); // Primera vez: debe notificar
            ElasticConnector.notifyEmptyResponse(); // Segunda vez: no debe notificar

            verify(mockGroup, times(1)).createNotification(anyString(), eq(NotificationType.WARNING));
        }
    }
}

