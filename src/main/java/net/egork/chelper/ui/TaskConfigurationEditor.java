package net.egork.chelper.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.util.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfigurationEditor extends SettingsEditor<TaskConfiguration> {
    private TaskConfiguration taskConfiguration;
    private JPanel wrapper;
    private TaskConfigurationPanel taskConfigurationPanel;

    public TaskConfigurationEditor(TaskConfiguration taskConfiguration) {
        this.taskConfiguration = taskConfiguration;
        applyTask();
    }

    private void applyTask() {
        if (wrapper == null)
            wrapper = new JPanel(new BorderLayout());
        if (!ProjectUtils.isValidConfiguration(taskConfiguration)) {
            taskConfiguration = new TaskConfiguration(taskConfiguration.getProject(), taskConfiguration.getName(), ProjectUtils.getDefaultTask().setName(taskConfiguration.getName()), taskConfiguration.getFactory());
        }
        taskConfigurationPanel = new TaskConfigurationPanel(taskConfiguration.getProject(), taskConfiguration.getName(), taskConfiguration.getConfiguration(), false, null, null);
        wrapper.add(taskConfigurationPanel, BorderLayout.CENTER);
    }

    @Override
    protected void resetEditorFrom(TaskConfiguration s) {
        taskConfiguration = s;
        applyTask();
    }

    @Override
    protected void applyEditorTo(TaskConfiguration s) throws ConfigurationException {
        if (!ProjectUtils.isValidConfigurationOrDeleteIfNot(s)) {
            return;
        }
        s.setConfiguration(taskConfigurationPanel.getTask());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        applyTask();
        return wrapper;
    }

    @Override
    protected void disposeEditor() {
    }
}
