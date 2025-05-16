package com.example.maidbridge.monitoring;

import com.example.maidbridge.monitoring.errortables.ErrorsTableCache;
import com.example.maidbridge.monitoring.errortables.TotalErrorsTable;
import com.example.maidbridge.monitoring.errortables.TotalErrorsTableCache;
import com.example.maidbridge.settings.MaidBridgeSettingsState;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ide.highlighter.JavaFileType;

import java.util.*;
import java.util.concurrent.*;

public class RefreshScheduler {

    private static ScheduledExecutorService scheduler;
    private static String lastSnapshot = "";

    public static void start(Project project) {
        if (scheduler != null && !scheduler.isShutdown()) return;

        MaidBridgeSettingsState settings = MaidBridgeSettingsState.getInstance();

        String currentSnapshot = settingsSnapshot(settings);
        if (!Objects.equals(lastSnapshot, currentSnapshot)) {
            DaemonCodeAnalyzer.getInstance(project).restart();
            lastSnapshot = currentSnapshot;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Map<String, Integer> errorCounts = ApplicationManager.getApplication().runReadAction((Computable<Map<String, Integer>>) () -> {
                Map<String, Integer> counts = new HashMap<>();
                Collection<VirtualFile> javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project));
                for (VirtualFile vf : javaFiles) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(vf);
                    if (psiFile == null) continue;
                    counts.putAll(TotalErrorsTableCache.countErrorOccurrencesByClass(psiFile));
                }
                return counts;
            });

            TotalErrorsTableCache.update(errorCounts);

            ApplicationManager.getApplication().invokeLater(() -> {
                TotalErrorsTable.refreshData(TotalErrorsTableCache.getAll());
                ErrorsTableCache.computeDetailedErrorData();
                DaemonCodeAnalyzer.getInstance(project).restart();
            });
        }, 15, 15, TimeUnit.SECONDS);
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
                settings.getKibanaURL()
        );
    }
}