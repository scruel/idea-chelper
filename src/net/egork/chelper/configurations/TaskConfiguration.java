package net.egork.chelper.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.actions.TopCoderAction;
import net.egork.chelper.codegeneration.SolutionGenerator;
import net.egork.chelper.task.Task;
import net.egork.chelper.ui.TaskConfigurationEditor;
import net.egork.chelper.util.*;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.InputMismatchException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TaskConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule> {
    private static final StackTraceLogger LOG = new StackTraceLogger(TaskConfiguration.class);
    private Task configuration;

    public TaskConfiguration(Project project, String name, Task configuration, ConfigurationFactory factory) {
        super(name, new JavaRunConfigurationModule(project, false), factory);
        LOG.debugMethodInfo(true, true, "name", name, "configuration", configuration, "location", configuration.location);
        boolean coverSave = this.configuration != configuration;
        saveConfiguration(configuration, coverSave);
        this.configuration = configuration;
        LOG.debugMethodInfo(false, true, "name", name, "configuration", configuration, "location", configuration.location);
    }

    @Override
    public Collection<Module> getValidModules() {
        return JavaRunConfigurationModule.getModulesForClass(getProject(), configuration.taskClass);
    }

    @Override
    protected ModuleBasedConfiguration createInstance() {
        return new TaskConfiguration(getProject(), getName(), configuration, getFactory());
    }

    @Override
    public Collection<Module> getAllModules() {
        return getValidModules();
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new TaskConfigurationEditor(this);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        ProjectManagerEx projectManager = ProjectManagerEx.getInstanceEx();
        final JavaRunConfigurationModule configurationModule = getConfigurationModule();
        if (configuration == null) {
            throw new RuntimeConfigurationWarning("configuration method not found in project.");
        }
        final PsiClass psiClass = configurationModule.findNotNullClass(configuration.taskClass);
        if (psiClass.findMethodsByName("solve", false).length == 0) {
            throw new RuntimeConfigurationWarning("solve method not found in class '" + configuration.taskClass + "'.");
        }
        ClassUtils.checkClass(getProject(), configuration.inputClass);
        ClassUtils.checkClass(getProject(), configuration.outputClass);
        ClassUtils.checkClass(getProject(), configuration.checkerClass);
        if (StringUtils.isEmpty(configuration.location)) {
            throw new RuntimeConfigurationWarning("configuration location '" + configuration.location + "' invalid.");
        }
        VirtualFile taskFile = FileUtils.getFileByFQN(getProject(), configuration.taskClass);
        VirtualFile parentFile = FileUtils.getFile(getProject(), configuration.location);
        if (!taskFile.getParent().equals(parentFile)) {
            throw new RuntimeConfigurationWarning("configuration location '" + configuration.location + "' not match task class '" + configuration.taskClass + "'.");
        }
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env)
        throws ExecutionException {
        saveConfiguration(configuration, false);
        SolutionGenerator.createSourceFile(getProject(), configuration);
        JavaCommandLineState state = new JavaCommandLineState(env) {
            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                JavaParameters parameters = new JavaParameters();
                PsiDirectory directory = FileUtils.getPsiDirectory(getProject(), configuration.location);
                Module module = ProjectRootManager.getInstance(getProject()).getFileIndex().getModuleForFile(
                    directory.getVirtualFile());
                parameters.configureByModule(module, JavaParameters.JDK_AND_CLASSES);
                parameters.setWorkingDirectory(getProject().getBaseDir().getPath());
                parameters.setMainClass("net.egork.chelper.tester.NewTester");
                String[] vmParameters = configuration.vmArgs.split(" ");
                for (String parameter : vmParameters)
                    parameters.getVMParametersList().add(parameter);
                if (configuration.failOnOverflow) {
                    String path = TopCoderAction.getJarPathForClass(com.github.cojac.CojacAgent.class);
                    parameters.getVMParametersList().add("-javaagent:" + path + "=-Cints -Clongs -Ccasts -Cmath");
                }
                String taskFileName = TaskUtils.getTaskFileName(configuration.location, configuration.name);
                parameters.getProgramParametersList().add(taskFileName);
                if (ProjectUtils.getData(getProject()).smartTesting) {
                    VirtualFile report = FileUtils.getFile(getProject(), "CHelperReport.txt");
                    if (report != null) {
                        try {
                            InputReader reader = new InputReader(report.getInputStream());
                            if (reader.readString().equals(taskFileName)) {
                                int failedTestCount = reader.readInt();
                                if (failedTestCount != 0) {
                                    int firstFailed = reader.readInt();
                                    parameters.getProgramParametersList().add(Integer.toString(firstFailed));
                                }
                            }
                        } catch (IOException ignored) {
                        } catch (InputMismatchException ignored) {
                        }
                    }
                }
                return parameters;
            }
        };
        state.setConsoleBuilder(new TextConsoleBuilderImpl(getProject()));
        return state;
    }

    public Task getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Task configuration) {
        this.configuration = configuration;
        saveConfiguration(configuration, true);
    }

    private void saveConfiguration(Task configuration, boolean cover) {
        LOG.debugMethodInfo(true, true, "task", configuration);
        if (configuration == null) return;
        if (configuration.location == null) return;
        if (configuration.taskClass == null) return;
        if (configuration.name == null) return;
        if (configuration.name.length() == 0) return;

        VirtualFile parentFile = FileUtils.getFile(getProject(), configuration.location);
        if (parentFile == null) return;

        if (!cover) {
            VirtualFile dataFile = parentFile.findChild(TaskUtils.canonize(configuration.name) + ".task");
            if (dataFile != null) {
                try {
                    this.configuration = Task.load(new InputReader(dataFile.getInputStream()));
                    return;
                } catch (IOException ignore) {
                }
            }
        }
        VirtualFile taskFile = FileUtils.getFileByFQN(getProject(), configuration.taskClass);
        if (taskFile != null && taskFile.getParent().equals(parentFile))
            FileUtils.saveConfiguration(configuration.location, TaskUtils.canonize(configuration.name) + ".task", configuration, getProject());
        LOG.debugMethodInfo(false, true, "task", configuration);
    }

    // Used to support previous versions of JDK6.0
    @Nullable
    public GlobalSearchScope getSearchScope() {
        Module[] modules = getModules();
        if (modules.length == 0) {
            return null;
        } else {
            GlobalSearchScope scope = GlobalSearchScope.moduleRuntimeScope(modules[0], true);
            for (int idx = 1; idx < modules.length; idx++) {
                Module module = modules[idx];
                scope = scope.uniteWith(GlobalSearchScope.moduleRuntimeScope(module, true));
            }
            return scope;
        }
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);
        String fileName = element.getChildText("taskConf");
        if (fileName != null && fileName.trim().length() != 0) {
            try {
                configuration = FileUtils.readTask(getProject(), fileName);
            } catch (NullPointerException ignored) {
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        if (configuration == null) return;
        super.writeExternal(element);
        Element configurationElement = new Element("taskConf");
        element.addContent(configurationElement);
        String configurationFile = TaskUtils.getTaskFileName(configuration.location, configuration.name);
        if (configurationFile != null)
            configurationElement.setText(configurationFile);
    }
}
