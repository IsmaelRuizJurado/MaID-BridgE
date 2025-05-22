package com.maidbridge.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jdesktop.swingx.JXDatePicker;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import static com.maidbridge.elastic.ElasticConnector.*;

public class MaidBridgeSettingsConfigurable implements Configurable {

    public JTextField elasticsearchURLField;
    public JTextField userField;
    public JPasswordField passwordField;
    public JTextField indexField;
    public JTextField kibanaURLField;

    public JComboBox<String> errorRangeCombo;
    public JComboBox<String> logRangeCombo;

    public JXDatePicker errorDatePicker;
    public JSpinner errorTimeSpinner;
    public JXDatePicker logDatePicker;
    public JSpinner logTimeSpinner;

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MaID-BridgE Settings";
    }

    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // Sección: Elasticsearch
        addLabeledField("Elasticsearch deployment URL:", elasticsearchURLField = new JTextField(), row++, gbc);
        addLabeledField("Elasticsearch username:", userField = new JTextField(), row++, gbc);
        addLabeledField("Elasticsearch password:", passwordField = new JPasswordField(), row++, gbc);
        addLabeledField("Elasticsearch index name:", indexField = new JTextField(), row++, gbc);
        addLabeledField("Kibana deployment URL:", kibanaURLField = new JTextField(), row++, gbc);

        // Separador con título para errores
        addTitledSeparator("Error Settings", row++, gbc);

        errorRangeCombo = new JComboBox<>(new String[]{"Last 24 hours", "Last week", "Last month", "Custom"});
        errorRangeCombo.addActionListener(e -> toggleCustomFields());
        addLabeledField("Error detection range:", errorRangeCombo, row++, gbc);

        errorDatePicker = new JXDatePicker();
        errorDatePicker.setFormats("yyyy-MM-dd");
        errorTimeSpinner = createTimeSpinner();
        addLabeledField("Error custom date:", errorDatePicker, row++, gbc);
        addLabeledField("Error custom time:", errorTimeSpinner, row++, gbc);

        // Separador con título para logs
        addTitledSeparator("Log Settings", row++, gbc);

        logRangeCombo = new JComboBox<>(new String[]{"Last 24 hours", "Last week", "Last month", "Custom"});
        logRangeCombo.addActionListener(e -> toggleCustomFields());
        addLabeledField("Log detection range:", logRangeCombo, row++, gbc);

        logDatePicker = new JXDatePicker();
        logDatePicker.setFormats("yyyy-MM-dd");
        logTimeSpinner = createTimeSpinner();
        addLabeledField("Log custom date:", logDatePicker, row++, gbc);
        addLabeledField("Log custom time:", logTimeSpinner, row++, gbc);

        return mainPanel;
    }

    private void addLabeledField(String label, JComponent field, int row, GridBagConstraints gbc) {
        // Etiqueta (sin expansión)
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel(label), gbc);

        // Campo (expande horizontalmente)
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mainPanel.add(field, gbc);
    }

    private void addTitledSeparator(String title, int row, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 0, 12, 0);

        JLabel separatorLabel = new JLabel("──── " + title + " ────", SwingConstants.CENTER);
        separatorLabel.setFont(separatorLabel.getFont().deriveFont(Font.PLAIN, 12f));
        separatorLabel.setForeground(Color.GRAY);

        mainPanel.add(separatorLabel, gbc);

        // Restaurar valores por defecto
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
    }

    private JSpinner createTimeSpinner() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);
        spinner.setValue(new Date());
        return spinner;
    }

    private void toggleCustomFields() {
        boolean errorCustom = "Custom".equals(errorRangeCombo.getSelectedItem());
        errorDatePicker.setEnabled(errorCustom);
        errorTimeSpinner.setEnabled(errorCustom);

        boolean logCustom = "Custom".equals(logRangeCombo.getSelectedItem());
        logDatePicker.setEnabled(logCustom);
        logTimeSpinner.setEnabled(logCustom);
    }

    private String mapRangeToCode(String label) {
        return switch (label) {
            case "Last 24 hours" -> "24h";
            case "Last week" -> "7d";
            case "Last month" -> "30d";
            case "Custom" -> "custom";
            default -> "24h";
        };
    }

    private ZonedDateTime getZonedDate(JXDatePicker datePicker, JSpinner timeSpinner) {
        Date datePart = datePicker.getDate();
        Date timePart = (Date) timeSpinner.getValue();
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(datePart);
        Calendar calTime = Calendar.getInstance();
        calTime.setTime(timePart);
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        return calDate.toInstant().atZone(ZoneOffset.systemDefault());
    }

    @Override
    public boolean isModified() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        return !elasticsearchURLField.getText().equals(settings.getElasticsearchURL()) ||
                !userField.getText().equals(settings.getUsername()) ||
                !new String(passwordField.getPassword()).equals(settings.getPassword()) ||
                !indexField.getText().equals(settings.getIndex()) ||
                !kibanaURLField.getText().equals(settings.getKibanaURL()) ||
                !mapRangeToCode((String) errorRangeCombo.getSelectedItem()).equals(settings.getErrorTimeRange()) ||
                !mapRangeToCode((String) logRangeCombo.getSelectedItem()).equals(settings.getLogTimeRange()) ||
                ("custom".equals(settings.getErrorTimeRange()) &&
                        !getZonedDate(errorDatePicker, errorTimeSpinner).equals(settings.getErrorCustomTime())) ||
                ("custom".equals(settings.getLogTimeRange()) &&
                        !getZonedDate(logDatePicker, logTimeSpinner).equals(settings.getLogCustomTime()));
    }

    @Override
    public void apply() throws ConfigurationException {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        try {
            new URL(elasticsearchURLField.getText());
            new URL(kibanaURLField.getText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid URL.");
        }

        if (indexField.getText().isEmpty()) {
            throw new ConfigurationException("An index name is required.");
        }

        close();

        settings.setElasticsearchURL(elasticsearchURLField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
        settings.setIndex(indexField.getText());
        settings.setKibanaURL(kibanaURLField.getText());

        settings.setErrorTimeRange(mapRangeToCode((String) errorRangeCombo.getSelectedItem()));
        settings.setLogTimeRange(mapRangeToCode((String) logRangeCombo.getSelectedItem()));

        if ("custom".equals(settings.getErrorTimeRange())) {
            settings.setErrorCustomTime(getZonedDate(errorDatePicker, errorTimeSpinner));
        }

        if ("custom".equals(settings.getLogTimeRange())) {
            settings.setLogCustomTime(getZonedDate(logDatePicker, logTimeSpinner));
        }

        try {
            getClient();
            emptyNotificationShown = false;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        elasticsearchURLField.setText(settings.getElasticsearchURL());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        indexField.setText(settings.getIndex());
        kibanaURLField.setText(settings.getKibanaURL());

        errorRangeCombo.setSelectedItem(switch (settings.getErrorTimeRange()) {
            case "7d" -> "Last week";
            case "30d" -> "Last month";
            case "custom" -> "Custom";
            default -> "Last 24 hours";
        });

        logRangeCombo.setSelectedItem(switch (settings.getLogTimeRange()) {
            case "7d" -> "Last week";
            case "30d" -> "Last month";
            case "custom" -> "Custom";
            default -> "Last 24 hours";
        });

        ZonedDateTime errorTime = settings.getErrorCustomTime();
        ZonedDateTime logTime = settings.getLogCustomTime();

        errorDatePicker.setDate(Date.from(errorTime.toInstant()));
        errorTimeSpinner.setValue(Date.from(errorTime.toInstant()));

        logDatePicker.setDate(Date.from(logTime.toInstant()));
        logTimeSpinner.setValue(Date.from(logTime.toInstant()));

        toggleCustomFields();
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}