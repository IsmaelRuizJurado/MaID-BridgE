package com.example.maidbridge.settings;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class MaidBridgeSettingsNotifier implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        boolean isConfigured =
                settings.getElasticsearchURL() != null && !settings.getElasticsearchURL().isEmpty() &&
                        settings.getUsername() != null && !settings.getUsername().isEmpty() &&
                        settings.getPassword() != null && !settings.getPassword().isEmpty() &&
                        settings.getIndex() != null && !settings.getIndex().isEmpty() &&
                        settings.getKibanaURL() != null && !settings.getKibanaURL().isEmpty();
        String content;
        NotificationType type;

        if (isConfigured) {
            content = String.format(
                    "<html>Current MaID-BridgE configuration:<br>- Elasticsearch URL: %s<br>- Index: %s<br>- Kibana URL: %s<br>- Refresh Interval: %d s</html>",
                    settings.getElasticsearchURL(),
                    settings.getIndex(),
                    settings.getKibanaURL(),
                    settings.getRefreshInterval()
            );
            type = NotificationType.INFORMATION;
        } else {
            content = "MaID-BridgE plugin is not fully configured. Please configure it now.";
            type = NotificationType.WARNING;
        }


        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("MaID-BridgE Notification Group")
                .createNotification("MaID-BridgE Configuration", content, type);

        notification.addAction(NotificationAction.createSimple("Configure Now", () ->
                ShowSettingsUtil.getInstance().showSettingsDialog(project, MaidBridgeSettingsConfigurable.class)
        ));

        notification.notify(project);

        return CompletableFuture.completedFuture(null);
    }
}
