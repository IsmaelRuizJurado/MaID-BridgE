package com.example.mpv_maidbridge.settings;

import javax.swing.*;
import java.awt.*;

public class ElasticSettingsComponent {

    private final JPanel panel;
    private final JTextField elasticUrlField;

    public ElasticSettingsComponent() {
        panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Elasticsearch URL:");
        elasticUrlField = new JTextField();

        panel.add(label, BorderLayout.WEST);
        panel.add(elasticUrlField, BorderLayout.CENTER);
    }

    public JPanel getPanel() {
        return panel;
    }

    public String getElasticUrl() {
        return elasticUrlField.getText();
    }

    public void setElasticUrl(String url) {
        elasticUrlField.setText(url);
    }
}
