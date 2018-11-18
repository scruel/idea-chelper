package net.egork.chelper.configurations;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.util.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfigurationType implements ConfigurationType {
    private static final Icon ICON = IconLoader.getIcon("/icons/taskIcon.png");
    private final ConfigurationFactory factory;
    public static final TaskConfigurationType INSTANCE = new TaskConfigurationType();

    private TaskConfigurationType() {
        factory = new ConfigurationFactory(this) {
            @Override
            @NotNull
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new TaskConfiguration(project, "Task", ProjectUtils.getDefaultTask(), factory);
            }

            @Override
            public RunConfiguration createConfiguration(String name, RunConfiguration template) {
                return super.createConfiguration(name, template);
            }
        };
    }

    public String getDisplayName() {
        return "Task";
    }

    public String getConfigurationTypeDescription() {
        return ProjectUtils.PROJECT_NAME + " Task";
    }

    public Icon getIcon() {
        return ICON;
    }

    @NotNull
    public String getId() {
        return "Task";
    }

    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{factory};
    }
}
