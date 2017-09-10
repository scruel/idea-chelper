package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import net.egork.chelper.ProjectData;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.ui.ProjectDataDialog;
import net.egork.chelper.util.ProjectUtils;

/**
 * @author Egor Kulikov, scruel (egorku@yandex-team.ru)
 */
public class EditProjectProperties extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = ProjectUtils.getProject(e.getDataContext());
        ProjectData data = ProjectUtils.getData(project);
        ProjectData result = ProjectDataDialog.edit(project, data);
        if (result != null) {
            if (!result.author.equals(data.author)) {
                CodeGenerationUtils.refreshAllTemplate(project, data.author, result.author);
            } else {
                CodeGenerationUtils.createTemplatesIfNeeded(project);
            }
            result.save(project);
            ProjectUtils.putProjectData(project, result);
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                public void run() {
                    ProjectUtils.ensureLibrary(project);
                }
            });
        }
    }
}
