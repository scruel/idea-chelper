package net.egork.chelper.configurations;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderConfigurationType implements ConfigurationType {
    private static final Icon ICON = IconLoader.getIcon("/icons/topcoder.png");
    private final ConfigurationFactory factory;
    public static final TopCoderConfigurationType INSTANCE = new TopCoderConfigurationType();

    private TopCoderConfigurationType() {
        factory = new ConfigurationFactory(this) {
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new TopCoderConfiguration("TopCoderTask", project,
                    new TopCoderTask("TopCoderTask", null, null, "", "", new String[0], null, false, "256M"), factory);
            }

            @Override
            public RunConfiguration createConfiguration(String name, RunConfiguration template) {
                return super.createConfiguration(name, template);
            }
        };
    }

    public String getDisplayName() {
        return "TopCoder Task";
    }

    public String getConfigurationTypeDescription() {
        return ProjectUtils.PROJECT_NAME + " TopCoder Task";
    }

    public Icon getIcon() {
        return ICON;
    }

    @NotNull
    public String getId() {
        return "TopCoderTask";
    }

    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{factory};
    }
}
