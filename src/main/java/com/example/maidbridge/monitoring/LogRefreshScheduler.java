package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.ElasticSettingsState;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogRefreshScheduler {

    private static ScheduledExecutorService scheduler;

    public static void start(Project project) {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        int interval = ElasticSettingsState.getInstance().getRefreshInterval(); // in seconds

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
}
