package com.example.maidbridge.settings;

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
    private JTextField indexField;


    private JPanel mainPanel;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "MaID-BridgE Settings";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new JPanel(new GridLayout(6, 2, 10, 10)); // 6 rows now

        hostField = new JTextField();
        portField = new JTextField();
        schemeField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();
        indexField = new JTextField();

        mainPanel.add(new JLabel("ElasticSearch host:"));
        mainPanel.add(hostField);

        mainPanel.add(new JLabel("ElasticSearch port:"));
        mainPanel.add(portField);

        mainPanel.add(new JLabel("ElasticSearch scheme:"));
        mainPanel.add(schemeField);

        mainPanel.add(new JLabel("ElasticSearch username:"));
        mainPanel.add(userField);

        mainPanel.add(new JLabel("ElasticSearch password:"));
        mainPanel.add(passwordField);

        mainPanel.add(new JLabel("ElasticSearch index:"));
        mainPanel.add(indexField);

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
                !indexField.getText().equals(settings.getIndex());
    }

    @Override
    public void apply() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        settings.setHost(hostField.getText());
        settings.setPort(Integer.parseInt(portField.getText()));
        settings.setScheme(schemeField.getText());
        settings.setUsername(userField.getText());
        settings.setPassword(new String(passwordField.getPassword()));
        settings.setIndex(indexField.getText());
    }

    @Override
    public void reset() {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();
        hostField.setText(settings.getHost());
        portField.setText(String.valueOf(settings.getPort()));
        schemeField.setText(settings.getScheme());
        userField.setText(settings.getUsername());
        passwordField.setText(settings.getPassword());
        indexField.setText(settings.getIndex());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}
