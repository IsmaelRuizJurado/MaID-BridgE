package com.example.maidbridge.monitoring;

import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RefreshScheduler {

    private static ScheduledExecutorService scheduler;
    private static String lastSnapshot = "";
    private static final List<Runnable> refreshListeners = new CopyOnWriteArrayList<>();

    public static void addRefreshListener(Runnable listener) {
        refreshListeners.add(listener);
    }

    public static void removeRefreshListener(Runnable listener) {
        refreshListeners.remove(listener);
    }

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
            // First, update all listeners (e.g., log and error data)
            for (Runnable listener : refreshListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Then refresh the UI
            DaemonCodeAnalyzer.getInstance(project).restart();
        }, interval, interval, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        scheduler = null;
        refreshListeners.clear();
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
