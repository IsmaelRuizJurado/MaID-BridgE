package com.example.maidbridge.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;

public class MaidBridgeSettingsConfigurable implements Configurable {

    private JTextField elasticsearchURLField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JTextField indexField;
    private JTextField kibanaURLField;
    private JSpinner refreshIntervalSpinner;

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MaID-BridgE Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridLayout(11, 2, 1, 1));

        elasticsearchURLField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();
        indexField = new JTextField();
        kibanaURLField = new JTextField();
        refreshIntervalSpinner = new JSpinner(new SpinnerNumberModel(15, 1, Integer.MAX_VALUE, 1));

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

        mainPanel.add(new JLabel("Refresh interval (seconds):"));
        mainPanel.add(refreshIntervalSpinner);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        return !elasticsearchURLField.getText().equals(settings.getElasticsearchURL()) ||
                !userField.getText().equals(settings.getUsername()) ||
                !new String(passwordField.getPassword()).equals(settings.getPassword()) ||
                !indexField.getText().equals(String.valueOf(settings.getIndex())) ||
                !kibanaURLField.getText().equals(settings.getKibanaURL()) ||
                !refreshIntervalSpinner.getValue().equals(settings.getRefreshInterval());
    }

    @Override
    public void apply() throws ConfigurationException {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        try {
            new URL(elasticsearchURLField.getText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Elasticsearch URL.");
        }

        try {
            new URL(kibanaURLField.getText());
        } catch (MalformedURLException e) {
            throw new ConfigurationException("Invalid Kibana URL.");
        }

        int interval = (Integer) refreshIntervalSpinner.getValue();

        settings.setElasticsearchURL(elasticsearchURLField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
        settings.setIndex(indexField.getText());
        settings.setKibanaURL(kibanaURLField.getText());
        settings.setRefreshInterval(interval);
    }

    @Override
    public void reset() {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        elasticsearchURLField.setText(settings.getElasticsearchURL());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        indexField.setText(settings.getIndex());
        kibanaURLField.setText(settings.getKibanaURL());
        refreshIntervalSpinner.setValue(settings.getRefreshInterval());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
