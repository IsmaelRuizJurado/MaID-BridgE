package com.maidbridge.monitoring.errortables;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ErrorsTable implements ToolWindowFactory, DumbAware{

    private static ErrorsTablePanel panel;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        panel = new ErrorsTablePanel(); // clase que extiende JPanel

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    public static void refreshData(List<ErrorsTablePanel.ErrorsTableEntry> data) {
        if (panel != null) {
            panel.refreshData(data);
        }
    }
}
