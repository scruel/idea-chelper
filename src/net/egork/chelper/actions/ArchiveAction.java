package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.ProjectUtils;

import java.io.IOException;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ArchiveAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!ProjectUtils.isEligible(e.getDataContext()))
            return;
        final Project project = ProjectUtils.getProject(e.getDataContext());
        final RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
        RunnerAndConfigurationSettings selectedConfiguration =
            manager.getSelectedConfiguration();
        if (selectedConfiguration == null || !ProjectUtils.isSupported(selectedConfiguration.getConfiguration())) {
            Messenger.publishMessage("Configuration not selected or selected configuration not supported",
                NotificationType.ERROR);
            return;
        }
        final RunConfiguration configuration = selectedConfiguration.getConfiguration();
        final TaskBase taskBase;
        if (configuration instanceof TaskConfiguration) {
            taskBase = ((TaskConfiguration) configuration).getConfiguration();
        } else {
            taskBase = ((TopCoderConfiguration) configuration).getConfiguration();

        }

        String archiveDir = ProjectUtils.getData(project).archiveDirectory;
        String dateAndContest = getDateAndContest(taskBase);
        final VirtualFile directory = FileUtils.createDirectoryIfMissing(project, archiveDir + "/" + dateAndContest);
        if (directory == null) {
            Messenger.publishMessage("Cannot create directory '" + archiveDir + "/" + dateAndContest + "' in archive",
                NotificationType.ERROR);
            return;
        }
        CodeGenerationUtils.createUnitTest(project, taskBase);
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    VirtualFile mainFile;
                    if (configuration instanceof TaskConfiguration) {
                        mainFile = FileUtils.getFileByFQN(project, ((Task) taskBase).taskClass);
                    } else {
                        mainFile = FileUtils.getFile(project,
                            ProjectUtils.getData(project).defaultDirectory
                                + "/" + taskBase.name + ".java");
                    }

                    if (mainFile != null) {
                        VfsUtil.copyFile(this, mainFile, directory);
                        mainFile.delete(this);
                    }
                    if (configuration instanceof TaskConfiguration) {
                        PsiElement checker = JavaPsiFacade.getInstance(project).findClass(((Task) taskBase).checkerClass, GlobalSearchScope.allScope(project));
                        VirtualFile checkerFile = checker == null ? null : checker.getContainingFile() == null ? null : checker.getContainingFile().getVirtualFile();
                        if (checkerFile != null && mainFile != null && checkerFile.getParent().equals(mainFile.getParent())) {
                            VfsUtil.copyFile(this, checkerFile, directory);
                            checkerFile.delete(this);
                        }
                    }
                    for (String testClass : taskBase.testClasses) {
                        VirtualFile testFile = FileUtils.getFileByFQN(project, testClass);
                        if (testFile != null) {
                            VfsUtil.copyFile(this, testFile, directory);
                            testFile.delete(this);
                        }
                    }

                    VirtualFile taskFile;
                    if (configuration instanceof TaskConfiguration) {
                        taskFile = FileUtils.getFile(project, ((Task) taskBase).location + "/" + canonize(taskBase.name) + ".task");
                    } else {
                        taskFile = FileUtils.getFile(project, ProjectUtils.getData(project).defaultDirectory + "/" + taskBase.name + ".tctask");

                    }
                    if (taskFile != null) {
                        VfsUtil.copyFile(this, taskFile, directory);
                        taskFile.delete(this);
                    }
                    manager.removeConfiguration(manager.getSelectedConfiguration());
                    setOtherConfiguration(manager, taskBase);
                    Messenger.publishMessage("Configuration '" + configuration.getName() + "' successfully archived",
                        NotificationType.INFORMATION);
                } catch (IOException e) {
                    Messenger.publishMessage("Error archiving configuration '" + configuration.getName() +
                        "' caused by " + e.getMessage(), NotificationType.ERROR);
                    Messenger.publishMessage("Configuration not deleted", NotificationType.WARNING);
                }
            }
        });
    }

    private String getDateAndContest(TaskBase task) {
        String yearAndMonth = task.date;
        int position = yearAndMonth.indexOf('.');
        if (position != -1)
            position = yearAndMonth.indexOf('.', position + 1);
        if (position != -1)
            yearAndMonth = yearAndMonth.substring(0, position);
        if (task instanceof Task && yearAndMonth.length() == 0) {
            return canonize(task.contestName.length() == 0 ? "unsorted" : task.contestName);
        }
        return canonize(yearAndMonth) + "/" + canonize(task.date + " - " + (task.contestName.length() == 0 ? "unsorted" : task.contestName));
    }

    public static String canonize(String filename) {
        filename = filename.replaceAll("[\\\\?%*:|\"<>/]", "-");
        while (filename.endsWith("."))
            filename = filename.substring(0, filename.length() - 1);
        return filename;
    }

    public static void setOtherConfiguration(RunManagerImpl manager, TaskBase task) {
        List<RunConfiguration> allConfigurations = manager.getAllConfigurationsList();
        for (RunConfiguration configuration : allConfigurations) {
            if (configuration instanceof TopCoderConfiguration) {
                TopCoderTask other = ((TopCoderConfiguration) configuration).getConfiguration();
                if (task != null && !task.contestName.equals(other.contestName))
                    continue;
                manager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(manager, configuration, false));
                return;
            } else if (configuration instanceof TaskConfiguration) {
                Task other = ((TaskConfiguration) configuration).getConfiguration();
                if (task != null && !task.contestName.equals(other.contestName))
                    continue;
                manager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(manager, configuration, false));
                return;
            }
        }
    }
}
