package com.example.maidbridge.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ElasticSettingsConfigurable implements Configurable {

    private JTextField hostField;
    private JTextField portField;
    private JTextField schemeField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JTextField refreshIntervalField;

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MaID-BridgE Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridLayout(6, 2, 10, 10));

        hostField = new JTextField();
        portField = new JTextField();
        schemeField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();
        refreshIntervalField = new JTextField();

        mainPanel.add(new JLabel("Elasticsearch host:"));
        mainPanel.add(hostField);

        mainPanel.add(new JLabel("Elasticsearch port:"));
        mainPanel.add(portField);

        mainPanel.add(new JLabel("Elasticsearch scheme:"));
        mainPanel.add(schemeField);

        mainPanel.add(new JLabel("Elasticsearch username:"));
        mainPanel.add(userField);

        mainPanel.add(new JLabel("Elasticsearch password:"));
        mainPanel.add(passwordField);

        mainPanel.add(new JLabel("Refresh interval (seconds):"));
        mainPanel.add(refreshIntervalField);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        return !hostField.getText().equals(settings.getHost()) ||
                !portField.getText().equals(String.valueOf(settings.getPort())) ||
                !schemeField.getText().equals(settings.getScheme()) ||
                !userField.getText().equals(settings.getUsername()) ||
                !new String(passwordField.getPassword()).equals(settings.getPassword()) ||
                !refreshIntervalField.getText().equals(String.valueOf(settings.getRefreshInterval()));
    }

    @Override
    public void apply() throws ConfigurationException {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();

        int interval;
        try {
            interval = Integer.parseInt(refreshIntervalField.getText());
            if (interval <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Refresh interval must be a positive integer.");
        }

        settings.setHost(hostField.getText());
        settings.setPort(Integer.parseInt(portField.getText()));
        settings.setScheme(schemeField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
        settings.setRefreshInterval(interval);
    }

    @Override
    public void reset() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        hostField.setText(settings.getHost());
        portField.setText(String.valueOf(settings.getPort()));
        schemeField.setText(settings.getScheme());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        refreshIntervalField.setText(String.valueOf(settings.getRefreshInterval()));
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
