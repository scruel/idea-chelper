package net.egork.chelper;

import com.intellij.execution.RunManagerAdapter;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.ProjectUtils;
import net.egork.chelper.util.TaskUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class AutoSwitcher implements ProjectComponent {
    private final Project project;
    private boolean busy;

    public AutoSwitcher(Project project) {
        this.project = project;
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "AutoSwitcher";
    }

    public void projectOpened() {
        addAutoSwitchFileListener();
        addAutoSwitchConfListener();
    }

    private void addAutoSwitchFileListener() {
        RunManagerImpl.getInstanceImpl(project).addRunManagerListener(new RunManagerAdapter() {
            public void runConfigurationSelected() {
                RunnerAndConfigurationSettings selectedConfiguration =
                    RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
                if (selectedConfiguration == null)
                    return;
                RunConfiguration configuration = selectedConfiguration.getConfiguration();
                if (!ProjectUtils.isValidConfigurationAndDeleteIfNot(configuration)) {
                    return;
                }
                if (busy
                    || !(configuration instanceof TopCoderConfiguration || configuration instanceof TaskConfiguration)) {
                    return;
                }
                busy = true;
                VirtualFile toOpen = null;
                if (configuration instanceof TopCoderConfiguration)
                    toOpen = TaskUtils.getFile(project, ((TopCoderConfiguration) configuration).getConfiguration().name, ProjectUtils.getData(project).defaultDirectory);
                else if (configuration instanceof TaskConfiguration)
                    toOpen = FileUtils.getFileByFQN(configuration.getProject(), ((TaskConfiguration) configuration).getConfiguration().taskClass);

                if (toOpen != null) {
                    final VirtualFile finalToOpen = toOpen;
                    ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
                        @Override
                        public void run() {
                            FileEditorManager.getInstance(project).openFile(finalToOpen, true);
                        }
                    });
                }
                busy = false;
            }
        });
    }

    private void addAutoSwitchConfListener() {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                selectTask(file);
            }

            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            }

            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                selectTask(event.getNewFile());
            }

            private void selectTask(final VirtualFile file) {
                Runnable selectTaskRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (busy || file == null)
                            return;
                        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);

                        Map<String, Object> taskMap;
                        if (null == (taskMap = TaskUtils.getTaskMapWitFile(project, file))) return;

                        RunConfiguration configuration = TaskUtils.GetConfSettingsBySourceFile(project, runManager,
                            (VirtualFile) taskMap.get(TaskUtils.TASK_SOURCE_KEY));
                        if (configuration != null) {
                            busy = true;
                            if (configuration instanceof TopCoderConfiguration) {
                                runManager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
                                    configuration, false));
                            } else if (configuration instanceof TaskConfiguration) {
                                runManager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
                                    configuration, false));
                            }
                            busy = false;
                            return;
                        }

                        repairAndConfigureTask(taskMap);
                    }

                    private void repairAndConfigureTask(Map<String, Object> taskMap) {
                        TaskBase task = (TaskBase) taskMap.get(TaskUtils.TASK_KEY);
                        VirtualFile taskFile = (VirtualFile) taskMap.get(TaskUtils.TASK_SOURCE_KEY);
                        task = TaskUtils.fixedTaskByPath(project, task, taskFile);
                        busy = true;
                        ProjectUtils.createConfiguration(project, task, true);
                        busy = false;
                    }
                };
                DumbService.getInstance(project).smartInvokeLater(selectTaskRunnable);
            }

        });
    }

    @Override
    public void projectClosed() {
        // called when project is being closed
    }
}
