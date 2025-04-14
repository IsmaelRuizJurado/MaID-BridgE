package com.example.mpv_maidbridge.settings;

import com.intellij.openapi.options.Configurable;
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

    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "ElasticSearch Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        hostField = new JTextField();
        portField = new JTextField();
        schemeField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();

        mainPanel.add(new JLabel("Host:"));
        mainPanel.add(hostField);

        mainPanel.add(new JLabel("Port:"));
        mainPanel.add(portField);

        mainPanel.add(new JLabel("Scheme:"));
        mainPanel.add(schemeField);

        mainPanel.add(new JLabel("Username:"));
        mainPanel.add(userField);

        mainPanel.add(new JLabel("Password:"));
        mainPanel.add(passwordField);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        return !hostField.getText().equals(settings.getHost()) ||
                !portField.getText().equals(String.valueOf(settings.getPort())) ||
                !schemeField.getText().equals(settings.getScheme()) ||
                !userField.getText().equals(settings.getUsername()) ||
                !new String(passwordField.getPassword()).equals(settings.getPassword());
    }

    @Override
    public void apply() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        settings.setHost(hostField.getText());
        settings.setPort(Integer.parseInt(portField.getText()));
        settings.setScheme(schemeField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
    }

    @Override
    public void reset() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        hostField.setText(settings.getHost());
        portField.setText(String.valueOf(settings.getPort()));
        schemeField.setText(settings.getScheme());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
