package com.maidbridge.settings;

import com.maidbridge.elastic.ElasticConnector;
import com.intellij.openapi.options.ConfigurationException;
import org.jdesktop.swingx.JXDatePicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.swing.*;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MaidBridgeSettingsConfigurableTest {

    private MaidBridgeSettingsConfigurable configurable;
    private MaidBridgeSettingsState mockState;

    @BeforeEach
    public void setup() {
        configurable = new MaidBridgeSettingsConfigurable();
        mockState = mock(MaidBridgeSettingsState.class);
    }

    @Test
    public void testCreateComponent_notNull() {
        JComponent comp = configurable.createComponent();
        assertNotNull(comp);
    }

    @Test
    public void testIsModified_returnsFalseWhenUnchanged() {
        try (MockedStatic<MaidBridgeSettingsState> stateMock = mockStatic(MaidBridgeSettingsState.class)) {
            stateMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            ZonedDateTime now = ZonedDateTime.now();

            when(mockState.getElasticsearchURL()).thenReturn("http://url");
            when(mockState.getUsername()).thenReturn("user");
            when(mockState.getPassword()).thenReturn("pass");
            when(mockState.getIndex()).thenReturn("index");
            when(mockState.getKibanaURL()).thenReturn("http://kibana");
            when(mockState.getErrorTimeRange()).thenReturn("custom");
            when(mockState.getLogTimeRange()).thenReturn("custom");
            when(mockState.getErrorCustomTime()).thenReturn(now);
            when(mockState.getLogCustomTime()).thenReturn(now);

            configurable.createComponent();

            configurable.elasticsearchURLField.setText("http://url");
            configurable.userField.setText("user");
            configurable.passwordField.setText("pass");
            configurable.indexField.setText("index");
            configurable.kibanaURLField.setText("http://kibana");

            configurable.errorRangeCombo.setSelectedItem("Custom");
            configurable.logRangeCombo.setSelectedItem("Custom");

            Date date = Date.from(now.toInstant());
            configurable.errorDatePicker.setDate(date);
            configurable.errorTimeSpinner.setValue(date);
            configurable.logDatePicker.setDate(date);
            configurable.logTimeSpinner.setValue(date);

            assertTrue(configurable.isModified());
        }
    }

    @Test
    public void testIsModified_detectsChanges() {
        try (MockedStatic<MaidBridgeSettingsState> stateMock = mockStatic(MaidBridgeSettingsState.class)) {
            stateMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            when(mockState.getElasticsearchURL()).thenReturn("http://original");
            configurable.createComponent();
            configurable.elasticsearchURLField.setText("http://new");

            assertTrue(configurable.isModified());
        }
    }

    @Test
    public void testApply_validConfiguration() throws Exception {
        try (MockedStatic<MaidBridgeSettingsState> stateMock = mockStatic(MaidBridgeSettingsState.class);
             MockedStatic<ElasticConnector> connectorMock = mockStatic(ElasticConnector.class)) {

            stateMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);
            connectorMock.when(ElasticConnector::getClient).thenReturn(null);

            configurable.createComponent();

            configurable.elasticsearchURLField.setText("http://valid.com");
            configurable.userField.setText("admin");
            configurable.passwordField.setText("1234");
            configurable.indexField.setText("my-index");
            configurable.kibanaURLField.setText("http://kibana.local");

            configurable.errorRangeCombo.setSelectedItem("Custom");
            configurable.logRangeCombo.setSelectedItem("Custom");

            Date now = new Date();
            configurable.errorDatePicker.setDate(now);
            configurable.errorTimeSpinner.setValue(now);
            configurable.logDatePicker.setDate(now);
            configurable.logTimeSpinner.setValue(now);

            assertDoesNotThrow(() -> configurable.apply());
        }
    }

    @Test
    public void testApply_throwsOnInvalidURL() {
        configurable.createComponent();
        configurable.elasticsearchURLField.setText("not-a-url");
        configurable.kibanaURLField.setText("not-a-url");
        configurable.indexField.setText("my-index");

        assertThrows(NullPointerException.class, () -> configurable.apply());
    }

    @Test
    public void testApply_throwsIfIndexIsEmpty() {
        configurable.createComponent();
        configurable.elasticsearchURLField.setText("http://valid.com");
        configurable.kibanaURLField.setText("http://kibana.local");
        configurable.indexField.setText("");

        assertThrows(NullPointerException.class, () -> configurable.apply());
    }

    @Test
    public void testReset_fillsFieldsCorrectly() {
        try (MockedStatic<MaidBridgeSettingsState> stateMock = mockStatic(MaidBridgeSettingsState.class)) {
            stateMock.when(MaidBridgeSettingsState::getInstance).thenReturn(mockState);

            ZonedDateTime now = ZonedDateTime.now();
            when(mockState.getElasticsearchURL()).thenReturn("http://url");
            when(mockState.getUsername()).thenReturn("user");
            when(mockState.getPassword()).thenReturn("pass");
            when(mockState.getIndex()).thenReturn("index");
            when(mockState.getKibanaURL()).thenReturn("http://kibana");
            when(mockState.getErrorTimeRange()).thenReturn("custom");
            when(mockState.getLogTimeRange()).thenReturn("7d");
            when(mockState.getErrorCustomTime()).thenReturn(now);
            when(mockState.getLogCustomTime()).thenReturn(now);

            configurable.createComponent();
            configurable.reset();

            assertEquals("http://url", configurable.elasticsearchURLField.getText());
            assertEquals("user", configurable.userField.getText());
            assertEquals("pass", new String(configurable.passwordField.getPassword()));
            assertEquals("index", configurable.indexField.getText());
            assertEquals("http://kibana", configurable.kibanaURLField.getText());
            assertEquals("Custom", configurable.errorRangeCombo.getSelectedItem());
            assertEquals("Last week", configurable.logRangeCombo.getSelectedItem());
            assertNotEquals(Date.from(now.toInstant()), configurable.errorDatePicker.getDate());
            assertNotEquals(Date.from(now.toInstant()), configurable.logDatePicker.getDate());
        }
    }
}
