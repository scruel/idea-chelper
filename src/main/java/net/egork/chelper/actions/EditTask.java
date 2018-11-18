package net.egork.chelper.actions;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.ui.CreateTaskDialog;
import net.egork.chelper.ui.EditTCDialog;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.ProjectUtils;

/**
 * @author egorku@yandex-team.ru
 */
public class EditTask extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!ProjectUtils.isEligible(e.getDataContext()))
            return;
        Project project = ProjectUtils.getProject(e.getDataContext());
        RunnerAndConfigurationSettings selectedConfiguration =
            RunManagerImpl.getInstanceImpl(project).getSelectedConfiguration();
        if (selectedConfiguration == null)
            return;
        RunConfiguration configuration = selectedConfiguration.getConfiguration();
        if (!ProjectUtils.isValidConfigurationOrDeleteIfNot(configuration)) {
            return;
        }
        if (configuration instanceof TaskConfiguration) {
            TaskConfiguration taskConfiguration = (TaskConfiguration) configuration;
            Task task = taskConfiguration.getConfiguration();
            if (task == null || task.location == null) {
                Messenger.publishMessage("Configuration corrupted.", NotificationType.WARNING);
                return;
            }
            task = CreateTaskDialog.showDialog(
                FileUtils.getPsiDirectory(project, task.location), task.name, task, false);
            if (task != null)
                taskConfiguration.setConfiguration(task);
        }
        if (configuration instanceof TopCoderConfiguration) {
            TopCoderConfiguration taskConfiguration = (TopCoderConfiguration) configuration;
            TopCoderTask task = taskConfiguration.getConfiguration();
            if (task == null) {
                Messenger.publishMessage("Configuration corrupted.", NotificationType.WARNING);
                return;
            }
            taskConfiguration.setConfiguration(EditTCDialog.show(project, task));
        }
    }
}
