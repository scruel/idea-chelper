package net.egork.chelper;

import com.intellij.execution.RunManagerListener;
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
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.ProjectUtils;
import net.egork.chelper.util.TaskUtils;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public void projectOpened() {
        addSelectedConfigurationListener();
        addFileEditorListeners();
    }

    private void addSelectedConfigurationListener() {
        RunManagerImpl.getInstanceImpl(project).addRunManagerListener(new RunManagerListener() {
            @Override
            public void runConfigurationSelected() {
                RunnerAndConfigurationSettings selectedConfiguration =
                    RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
                if (selectedConfiguration == null)
                    return;
                RunConfiguration configuration = selectedConfiguration.getConfiguration();
                if (busy || !(configuration instanceof TopCoderConfiguration || configuration instanceof TaskConfiguration)) {
                    return;
                }
                busy = true;
                VirtualFile toOpen = null;
                if (configuration instanceof TopCoderConfiguration)
                    toOpen = TaskUtils.getFile(ProjectUtils.getData(project).defaultDirectory, ((TopCoderConfiguration) configuration).getConfiguration().name, project);
                else if (configuration instanceof TaskConfiguration)
                    toOpen = FileUtils.getFileByFQN(((TaskConfiguration) configuration).getConfiguration().taskClass, configuration.getProject());
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

    private void addFileEditorListeners() {
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

                        VirtualFile taskFile;
                        if (null == (taskFile = TaskUtils.getTaskFile(file))) return;
                        RunnerAndConfigurationSettings old = runManager.findConfigurationByName(taskFile.getNameWithoutExtension());
                        if (old != null) {
                            RunConfiguration configuration = old.getConfiguration();
                            if (configuration instanceof TopCoderConfiguration) {
                                TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
                                if (file.equals(TaskUtils.getFile(ProjectUtils.getData(project).defaultDirectory, task.name, project))) {
                                    busy = true;
                                    runManager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
                                        configuration, false));
                                    busy = false;
                                }
                            } else if (configuration instanceof TaskConfiguration) {
                                Task task = ((TaskConfiguration) configuration).getConfiguration();
                                task = TaskUtils.taskOfFixedPath(project, task, taskFile);
                                if (file.equals(FileUtils.getFileByFQN(task.taskClass, configuration.getProject()))) {
                                    busy = true;
                                    runManager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(runManager,
                                        configuration, false));
                                    busy = false;
                                }
                            }
                            return;
                        }

                        Task task = FileUtils.readTask(FileUtils.getRelativePath(project.getBaseDir(), taskFile), project);
                        task = TaskUtils.taskOfFixedPath(project, task, taskFile);
                        ProjectUtils.createConfiguration(task, true, project);
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
