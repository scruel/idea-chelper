package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.parser.TaskProcessReceiver;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.ParseDialog;
import net.egork.chelper.util.ProjectUtils;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ParseContestAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!ProjectUtils.isEligible(e.getDataContext()))
            return;
        final Project project = ProjectUtils.getProject(e.getDataContext());

        TaskProcessReceiver taskProcessReceiver = new TaskProcessReceiver(project) {
            @Override
            protected void processTask(Task task) {
                PsiElement element = JavaPsiFacade.getInstance(project).findClass(task.taskClass, GlobalSearchScope.allScope(project));
                ProjectUtils.createConfiguration(project, task, isFirstConfiguration());
                setFirstConfiguration(false);
                if (getFirstElement() == null)
                    setFirstElement(element);
            }
        };
        new ParseDialog(project, taskProcessReceiver);
    }
}
