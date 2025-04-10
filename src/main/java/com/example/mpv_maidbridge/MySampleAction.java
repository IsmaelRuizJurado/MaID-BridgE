package com.example.mpv_maidbridge;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class MySampleAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Messages.showMessageDialog(
                "Hola desde un plugin en Java 🎉",
                "Mensaje del Plugin",
                Messages.getInformationIcon()
        );
    }
}
