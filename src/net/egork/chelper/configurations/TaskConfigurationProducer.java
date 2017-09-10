package net.egork.chelper.configurations;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.ProjectUtils;
import net.egork.chelper.util.TaskUtils;

import java.util.Map;

/**
 * Created by Scruel on 2017/9/6.
 * Github : https://github.com/scruel
 */
public class TaskConfigurationProducer extends RunConfigurationProducer<TaskConfiguration> {
    protected TaskConfigurationProducer(TaskConfigurationType configurationType) {
        super(configurationType);
    }


    @Override
    protected boolean setupConfigurationFromContext(TaskConfiguration configuration, ConfigurationContext context, Ref sourceElement) {
        final Location location = context.getLocation();
        if (location == null) return false;
        final PsiFile script = location.getPsiElement().getContainingFile();
        if (!isAvailable(location, script)) return false;
        final VirtualFile vFile = script.getVirtualFile();
        if (vFile == null) return false;
        Project project = context.getProject();
        Map<String, Object> map = TaskUtils.getTaskMapWitFile(project, vFile);
        if (map == null) return false;
        VirtualFile dataFile;
        if (null == (dataFile = (VirtualFile) map.get(TaskUtils.TASK_DATA_KEY)))
            return false;
        Task task = FileUtils.readTask(project, FileUtils.getRelativePath(project.getBaseDir(), dataFile));
        task = (Task) TaskUtils.fixedTaskByPath(project, task, dataFile);
        if (task == null) return false;
        // configuration = new TaskConfiguration(task.name, project, task,
        //     TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
        configuration.setName(task.name);
        configuration.setConfiguration(task);
        return true;
    }

    private static boolean isAvailable(final Location location, final PsiFile script) {
        if (script == null) return false;
        final Module module = ModuleUtilCore.findModuleForPsiElement(script);
        return module != null;
    }

    @Override
    public boolean isConfigurationFromContext(TaskConfiguration configuration, ConfigurationContext context) {
        final Location location = context.getLocation();
        if (location == null) return false;
        final PsiFile script = location.getPsiElement().getContainingFile();
        if (!isAvailable(location, script)) return false;
        final VirtualFile vFile = script.getVirtualFile();
        if (vFile == null) return false;
        if (vFile instanceof LightVirtualFile) return false;

        Project project = context.getProject();
        Map<String, Object> map = TaskUtils.getTaskMapWitFile(project, vFile);
        if (map == null) return false;
        if (null == (map.get(TaskUtils.TASK_DATA_KEY))) return false;

        if (!ProjectUtils.isValidConfigurationOrDeleteIfNot(configuration)) return false;
        final String taskLocation = configuration.getConfiguration().location;
        final String scriptName = configuration.getConfiguration().name;
        final String path = FileUtils.getRelativePath(context.getProject().getBaseDir(), vFile);
        return path.equals(taskLocation + "/" + scriptName + ".java") || path.equals(taskLocation + "/" + scriptName + ".task");
    }
}
