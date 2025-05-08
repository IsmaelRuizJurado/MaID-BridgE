package com.example.maidbridge.monitoring;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ErrorTable implements ToolWindowFactory, DumbAware {

    private static ErrorTablePanel panel;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        panel = new ErrorTablePanel(); // clase que extiende JPanel

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }


    public static void refreshData(Map<String, Integer> data) {
        if (panel != null) {
            panel.refreshData(data);
        }
    }
}
