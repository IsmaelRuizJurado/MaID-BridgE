package com.maidbridge.settings;

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

import java.time.format.DateTimeFormatter;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        if (isConfigured) {
            String errorRange = settings.getErrorTimeRange();
            String logRange = settings.getLogTimeRange();

            String errorCustomTime = "";
            if ("custom".equals(errorRange)) {
                errorCustomTime = "<br>- Error Custom Time: " + settings.getErrorCustomTime().format(formatter);
            }

            String logCustomTime = "";
            if ("custom".equals(logRange)) {
                logCustomTime = "<br>- Log Custom Time: " + settings.getLogCustomTime().format(formatter);
            }

            content = String.format("""
                    <html>
                    Current MaID-BridgE configuration:<br>
                    - Elasticsearch URL: %s<br>
                    - Index: %s<br>
                    - Kibana URL: %s<br>
                    - Error Range: %s%s<br>
                    - Log Range: %s%s
                    </html>
                    """,
                    settings.getElasticsearchURL(),
                    settings.getIndex(),
                    settings.getKibanaURL(),
                    errorRange.toUpperCase(), errorCustomTime,
                    logRange.toUpperCase(), logCustomTime
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