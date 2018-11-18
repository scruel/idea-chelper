package net.egork.chelper.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import net.egork.chelper.ProjectData;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.ProjectUtils;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class NewTaskDefaultAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!ProjectUtils.isEligible(e.getDataContext()))
            return;
        Project project = ProjectUtils.getProject(e.getDataContext());
        createTaskInDefaultDirectory(project, null);
    }

    public static void createTaskInDefaultDirectory(Project project, Task task) {
        ProjectData data = ProjectUtils.getData(project);
        PsiDirectory directory = FileUtils.getPsiDirectory(project, data.defaultDirectory);
        if (directory == null) {
            FileUtils.createDirectoryIfMissing(project, data.defaultDirectory);
            directory = FileUtils.getPsiDirectory(project, data.defaultDirectory);
            if (directory == null) {
                Messenger.publishMessage("Unable to create default directory", NotificationType.ERROR);
                return;
            }
        }
        PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        if (aPackage == null || aPackage.getName() == null || "".equals(aPackage.getName())) {
            Messenger.publishMessage("defaultDirectory should be under source and in non-default package", NotificationType.WARNING);
            return;
        }
        PsiElement[] result = NewTaskAction.createTask(task == null ? null : task.name, directory, task, true);
        for (PsiElement element : result) {
            ProjectUtils.openElement(project, element);
        }
    }

}
