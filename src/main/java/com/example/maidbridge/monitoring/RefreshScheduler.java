package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RefreshScheduler {

    private static ScheduledExecutorService scheduler;
    private static String lastSnapshot = "";

    public static void start(Project project) {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();
        int interval = settings.getRefreshInterval(); // in seconds

        String currentSnapshot = settingsSnapshot(settings);
        if (!Objects.equals(lastSnapshot, currentSnapshot)) {
            DaemonCodeAnalyzer.getInstance(project).restart();
            lastSnapshot = currentSnapshot;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }, interval, interval, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
    }

    private static String settingsSnapshot(MaidBridgeSettingsState settings) {
        return String.join("|",
                settings.getElasticsearchURL(),
                settings.getUsername(),
                settings.getPassword(),
                settings.getIndex(),
                settings.getKibanaURL(),
                String.valueOf(settings.getRefreshInterval())
        );
    }
}
