package net.egork.chelper.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
            result.save(project);
            ProjectUtils.putProjectData(project, result);
            if (data != null && !result.equals(data)) {
                CodeGenerationUtils.refreshAllTemplate(project, data, result);
            } else {
                CodeGenerationUtils.createTemplatesIfNeeded(project);
            }
            ProjectUtils.ensureLibrary(project);
        }
    }
}
