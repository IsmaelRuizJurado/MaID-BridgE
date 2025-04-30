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

public class ElasticSettingsNotifier implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ElasticSettingsState settings = ElasticSettingsState.getInstance();

        boolean isConfigured =
                settings.getHost() != null && !settings.getHost().isEmpty() &&
                        settings.getPort() > 0 &&
                        settings.getScheme() != null && !settings.getScheme().isEmpty() &&
                        settings.getUsername() != null && !settings.getUsername().isEmpty() &&
                        settings.getPassword() != null && !settings.getPassword().isEmpty() &&
                        settings.getIndex() != null && !settings.getIndex().isEmpty();

        String content;
        NotificationType type;

        if (isConfigured) {
            content = String.format(
                    "<html>Current MaID-BridgE configuration:<br>- Host: %s<br>- Port: %d<br>- Scheme: %s<br>- Index: %s<br>- Refresh Interval: %d s</html>",
                    settings.getHost(),
                    settings.getPort(),
                    settings.getScheme(),
                    settings.getIndex(),
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
                ShowSettingsUtil.getInstance().showSettingsDialog(project, ElasticSettingsConfigurable.class)
        ));

        notification.notify(project);

        return CompletableFuture.completedFuture(null);
    }
}
