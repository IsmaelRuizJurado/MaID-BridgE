package com.example.maidbridge.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jdesktop.swingx.JXDatePicker;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import static com.example.maidbridge.elastic.ElasticConnector.*;

public class MaidBridgeSettingsConfigurable implements Configurable {

    private JTextField elasticsearchURLField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JTextField indexField;
    private JTextField kibanaURLField;
    private JSpinner refreshIntervalSpinner;
    private JXDatePicker startDatePicker;
    private JSpinner timeSpinner;

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MaID-BridgE Settings";
    }

    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridLayout(12, 2, 1, 1));

        elasticsearchURLField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();
        indexField = new JTextField();
        kibanaURLField = new JTextField();

        // Date picker setup
        startDatePicker = new JXDatePicker();
        startDatePicker.setFormats("yyyy-MM-dd"); // solo fecha

        // Spinner para seleccionar hora y minutos
        SpinnerDateModel timeModel = new SpinnerDateModel();
        timeSpinner = new JSpinner(timeModel);
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setValue(new Date()); // valor inicial

        mainPanel.add(new JLabel("Elasticsearch deployment URL:"));
        mainPanel.add(elasticsearchURLField);

        mainPanel.add(new JLabel("Elasticsearch username:"));
        mainPanel.add(userField);

        mainPanel.add(new JLabel("Elasticsearch password:"));
        mainPanel.add(passwordField);

        mainPanel.add(new JLabel("Elasticsearch index name:"));
        mainPanel.add(indexField);

        mainPanel.add(new JLabel("Kibana deployment URL:"));
        mainPanel.add(kibanaURLField);

        mainPanel.add(new JLabel("Error detection start date:"));
        mainPanel.add(startDatePicker);

        mainPanel.add(new JLabel("Error detection start time (HH:mm):"));
        mainPanel.add(timeSpinner);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        ZonedDateTime selectedStart = ZonedDateTime.ofInstant(startDatePicker.getDate().toInstant(), ZoneId.systemDefault());

        return !elasticsearchURLField.getText().equals(settings.getElasticsearchURL()) ||
                !userField.getText().equals(settings.getUsername()) ||
                !new String(passwordField.getPassword()).equals(settings.getPassword()) ||
                !indexField.getText().equals(String.valueOf(settings.getIndex())) ||
                !kibanaURLField.getText().equals(settings.getKibanaURL()) ||
                !selectedStart.equals(settings.getStartTime());
    }

    @Override
    public void apply() throws ConfigurationException {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        try {
            new URL(elasticsearchURLField.getText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Elasticsearch URL.");
        }

        if (indexField.getText().isEmpty()){
            throw new ConfigurationException("An index name is required.");
        }

        try {
            new URL(kibanaURLField.getText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Kibana URL.");
        }

        Date datePart = startDatePicker.getDate();
        Date timePart = (Date) timeSpinner.getValue();

        if (datePart == null || timePart == null) {
            throw new ConfigurationException("Start date and time must be selected.");
        }

        Calendar calDate = Calendar.getInstance();
        calDate.setTime(datePart);

        Calendar calTime = Calendar.getInstance();
        calTime.setTime(timePart);

        // Combinar fecha y hora
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));

        ZonedDateTime startTime = calDate.toInstant().atZone(ZoneOffset.systemDefault());

        if (startTime.isAfter(ZonedDateTime.now())) {
            throw new ConfigurationException("Start time cannot be in the future.");
        }

        close();
        settings.setElasticsearchURL(elasticsearchURLField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
        try {
            getClient();
            emptyNotificationShown = false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        settings.setIndex(indexField.getText());
        settings.setKibanaURL(kibanaURLField.getText());
        settings.setStartTime(startTime);
    }

    @Override
    public void reset() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        elasticsearchURLField.setText(settings.getElasticsearchURL());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        indexField.setText(settings.getIndex());
        kibanaURLField.setText(settings.getKibanaURL());

        ZonedDateTime stored = settings.getStartTime();
        Date storedDate = Date.from(stored.toInstant());

        startDatePicker.setDate(storedDate);
        timeSpinner.setValue(storedDate);

    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
