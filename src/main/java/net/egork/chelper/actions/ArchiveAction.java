package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.util.*;

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
        if (!ProjectUtils.isValidConfigurationOrDeleteIfNot(configuration)) {
            return;
        }
        final TaskBase taskBase;
        if (configuration instanceof TaskConfiguration) {
            taskBase = ((TaskConfiguration) configuration).getConfiguration();
        } else {
            taskBase = ((TopCoderConfiguration) configuration).getConfiguration();
        }
        if (taskBase == null) return;
        String archiveDir = ProjectUtils.getData(project).archiveDirectory;
        String dateAndContest = getDateAndContest(taskBase);
        final PsiDirectory directory = FileUtils.createPsiDirectoryIfMissing(project, archiveDir + "/" + dateAndContest);
        if (directory == null) {
            Messenger.publishMessage("Cannot create directory '" + archiveDir + "/" + dateAndContest + "' in archive",
                NotificationType.ERROR);
            return;
        }
        CodeGenerationUtils.createUnitTest(project, taskBase);
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    PsiFile sourceFile;
                    if (configuration instanceof TaskConfiguration) {
                        sourceFile = FileUtils.getPsiFileByFQN(project, ((Task) taskBase).taskClass);
                    } else {
                        sourceFile = FileUtils.getPsiFile(project,
                            ProjectUtils.getData(project).defaultDirectory
                                + "/" + taskBase.name + ".java");
                    }
                    if (sourceFile == null) {
                        Messenger.publishMessage("SourceFile where task was located is no longer exists",
                            NotificationType.ERROR);
                        return;
                    }
                    directory.copyFileFrom(sourceFile.getName(), sourceFile);
                    if (configuration instanceof TaskConfiguration) {
                        PsiElement checker = JavaPsiFacade.getInstance(project).findClass(((Task) taskBase).checkerClass, GlobalSearchScope.allScope(project));
                        PsiFile checkerFile = checker == null ? null : checker.getContainingFile();
                        if (checkerFile != null && checkerFile.getParent().equals(sourceFile.getParent())) {
                            directory.copyFileFrom(checkerFile.getName(), checkerFile);
                            checkerFile.delete();
                        }
                    }
                    for (String testClass : taskBase.testClasses) {
                        PsiFile testFile = FileUtils.getPsiFileByFQN(project, testClass);
                        if (testFile != null) {
                            directory.copyFileFrom(testFile.getName(), testFile);
                            testFile.delete();
                        }
                    }

                    PsiFile taskFile;
                    if (configuration instanceof TaskConfiguration) {
                        taskFile = FileUtils.getPsiFile(project, ((Task) taskBase).location + "/" + TaskUtils.canonize(taskBase.name) + ".task");
                    } else {
                        taskFile = FileUtils.getPsiFile(project, ProjectUtils.getData(project).defaultDirectory + "/" + taskBase.name + ".tctask");
                    }
                    if (taskFile != null) {
                        directory.copyFileFrom(taskFile.getName(), taskFile);
                    }
                    FileUtils.deleteTaskIfExists(project, CompatibilityUtils.getPsiFile(project, sourceFile.getVirtualFile()), true);
                    ProjectUtils.setOtherConfiguration(manager, taskBase);
                    Messenger.publishMessage("Configuration '" + configuration.getName() + "' successfully archived",
                        NotificationType.INFORMATION);
                } catch (IncorrectOperationException e) {
                    Messenger.publishMessage("Error archiving configuration '" + configuration.getName() +
                        "' caused by " + e.getMessage(), NotificationType.ERROR);
                    Messenger.publishMessage("Configuration not deleted", NotificationType.WARNING);
                }
            }
        }, false);
    }

    private String getDateAndContest(TaskBase task) {
        String yearAndMonth = task.date;
        int position = yearAndMonth.indexOf('.');
        if (position != -1)
            position = yearAndMonth.indexOf('.', position + 1);
        if (position != -1)
            yearAndMonth = yearAndMonth.substring(0, position);
        if (task instanceof Task && yearAndMonth.length() == 0) {
            return TaskUtils.canonize(task.contestName.length() == 0 ? "unsorted" : task.contestName);
        }
        return TaskUtils.canonize(yearAndMonth) + "/" + TaskUtils.canonize(task.date + " - " + (task.contestName.length() == 0 ? "unsorted" : task.contestName));
    }

}
