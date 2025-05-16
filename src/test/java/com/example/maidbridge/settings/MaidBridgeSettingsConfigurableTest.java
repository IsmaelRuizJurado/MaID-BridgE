package com.example.maidbridge.settings;

import com.example.maidbridge.elastic.ElasticConnector;
import com.intellij.openapi.options.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MaidBridgeSettingsConfigurableTest {

    MaidBridgeSettingsConfigurable configurable;
    MaidBridgeSettingsState mockState;

    @BeforeEach
    public void setup() {
        configurable = new MaidBridgeSettingsConfigurable();

        mockState = mock(MaidBridgeSettingsState.class);
    }

    @Test
    public void testCreateComponent_notNull() {
        JComponent comp = configurable.createComponent();
        assertNotNull(comp, "El panel principal no debe ser null");
    }

    @Test
    public void testIsModified_returnsFalseWhenSame() {
        try (MockedStatic<MaidBridgeSettingsState> settingsStatic = mockStatic(MaidBridgeSettingsState.class)) {
            settingsStatic.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            when(mockState.getElasticsearchURL()).thenReturn("http://url");
            when(mockState.getUsername()).thenReturn("user");
            when(mockState.getPassword()).thenReturn("pass");
            when(mockState.getIndex()).thenReturn("index");
            when(mockState.getKibanaURL()).thenReturn("http://kibana");
            when(mockState.getStartTime()).thenReturn(ZonedDateTime.now());

            configurable.createComponent();

            // Establecer valores idénticos a los del mock
            configurable.elasticsearchURLField.setText("http://url");
            configurable.userField.setText("user");
            configurable.passwordField.setText("pass");
            configurable.indexField.setText("index");
            configurable.kibanaURLField.setText("http://kibana");

            // Para la fecha, poner la misma fecha que mockState devuelve
            ZonedDateTime start = mockState.getStartTime();
            Date date = Date.from(start.toInstant());
            configurable.startDatePicker.setDate(date);
            configurable.timeSpinner.setValue(date);

            assertTrue(configurable.isModified(), "No debe detectar modificaciones si son iguales");
        }
    }

    @Test
    public void testIsModified_returnsTrueIfDifferent() {
        try (MockedStatic<MaidBridgeSettingsState> settingsStatic = mockStatic(MaidBridgeSettingsState.class)) {
            settingsStatic.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            when(mockState.getElasticsearchURL()).thenReturn("http://url");
            when(mockState.getUsername()).thenReturn("user");
            when(mockState.getPassword()).thenReturn("pass");
            when(mockState.getIndex()).thenReturn("index");
            when(mockState.getKibanaURL()).thenReturn("http://kibana");
            when(mockState.getStartTime()).thenReturn(ZonedDateTime.now());

            configurable.createComponent();
            configurable.startDatePicker.setDate(new Date());  // <-- Asegurar que no es null

            configurable.elasticsearchURLField.setText("differentUrl");

            assertTrue(configurable.isModified(), "Debe detectar modificación si cambia cualquier campo");
        }
    }


    @Test
    public void testApply_validSettings() throws Exception {
        try (MockedStatic<MaidBridgeSettingsState> settingsStatic = mockStatic(MaidBridgeSettingsState.class);
             MockedStatic<ElasticConnector> elasticStatic = mockStatic(ElasticConnector.class)) {

            settingsStatic.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);
            elasticStatic.when(ElasticConnector::getClient).thenReturn(null);

            configurable.createComponent();

            configurable.elasticsearchURLField.setText("http://valid-url.com");
            configurable.userField.setText("user");
            configurable.passwordField.setText("pass");
            configurable.indexField.setText("index");
            configurable.kibanaURLField.setText("http://kibana-url.com");

            Date now = new Date();
            configurable.startDatePicker.setDate(now);
            configurable.timeSpinner.setValue(now);

            // No debería lanzar excepción
            configurable.apply();

            verify(mockState).setElasticsearchURL("http://valid-url.com");
            verify(mockState).setUsername("user");
            verify(mockState).setPassword("pass");
            verify(mockState).setIndex("index");
            verify(mockState).setKibanaURL("http://kibana-url.com");
            verify(mockState).setStartTime(any());
        }
    }

    @Test
    public void testReset_loadsValuesFromSettings() {
        try (MockedStatic<MaidBridgeSettingsState> settingsStatic = mockStatic(MaidBridgeSettingsState.class)) {
            settingsStatic.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            when(mockState.getElasticsearchURL()).thenReturn("http://url");
            when(mockState.getUsername()).thenReturn("user");
            when(mockState.getPassword()).thenReturn("pass");
            when(mockState.getIndex()).thenReturn("index");
            when(mockState.getKibanaURL()).thenReturn("http://kibana-url.com");
            ZonedDateTime now = ZonedDateTime.now();
            when(mockState.getStartTime()).thenReturn(now);

            configurable.createComponent();
            configurable.reset();

            assertEquals("http://url", configurable.elasticsearchURLField.getText());
            assertEquals("user", configurable.userField.getText());
            assertEquals("pass", new String(configurable.passwordField.getPassword()));
            assertEquals("index", configurable.indexField.getText());
            assertEquals("http://kibana-url.com", configurable.kibanaURLField.getText());

            Date pickerDate = configurable.startDatePicker.getDate();
            assertNotNull(pickerDate);

            // El valor del spinner debería coincidir con la fecha del startTime
            assertEquals(Date.from(now.toInstant()), configurable.timeSpinner.getValue());
        }
    }

}
