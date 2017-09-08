package net.egork.chelper.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.codegeneration.SolutionGenerator;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.task.Task;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskUtilities {
    public static void createSourceFile(Task task, Project project) {
        SolutionGenerator.createSourceFile(task, project);
    }

    public static VirtualFile getFile(String location, String name, Project project) {
        return FileUtilities.getFile(project, location + "/" + name + ".java");
    }

    public static String getTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + ArchiveAction.canonize(name) + ".task";
        return null;
    }

    public static String getTopCoderTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + name + ".tctask";
        return null;
    }

    /**
     * 获取同级目录中同名的 .task 文件
     *
     * @param file
     * @return
     */
    public static VirtualFile getTaskFile(VirtualFile file) {
        if (file == null) {
            return null;
        }
        String extension = file.getExtension();
        if (!"java".equals(extension) && !"task".equals(extension)) {
            return null;
        }
        String taskName = file.getNameWithoutExtension();
        VirtualFile parentFile = file.getParent();

        VirtualFile taskFile;
        if ("task".equals(extension)) {
            if (parentFile.findChild(taskName + ".java") == null) return null;
        }
        taskFile = parentFile.findChild(taskName + ".task");
        return taskFile;
    }

    /**
     * 自动根据传入 VirtualFile 修复 task，防止报错。
     *
     * @param project
     * @param task
     * @param taskDataFile
     * @return
     */
    public static Task taskOfFixedPath(Project project, Task task, VirtualFile taskDataFile) {
        Task currentTask = fixTask(project, task, taskDataFile);
        if (currentTask != task) {
            //just update local task file.
            new TaskConfiguration(task.name, project, task,
                TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
        }
        return currentTask;
    }

    private static Task fixTask(Project project, Task task, VirtualFile taskDataFile) {
        if (task == null || taskDataFile == null) return null;
        PsiClass aClass = MainFileTemplate.getClass(project, task.checkerClass);
        if (aClass == null) {
            task = task.setCheckerClass(PEStrictChecker.class.getCanonicalName());
        }
//        aClass = MainFileTemplate.getClass(project, task.inputClass);
//        if (aClass == null) {
//            task = task.setCheckerClass(InputReader.class.getCanonicalName());
//        }
//        aClass = MainFileTemplate.getClass(project, task.outputClass);
//        if (aClass == null) {
//            task = task.setCheckerClass(OutputWriter.class.getCanonicalName());
//        }

        // check task move
        String location = FileUtilities.getRelativePath(project.getBaseDir(), taskDataFile.getParent());
        if (location.equals(task.location))
            return task;
        String aPackage = FileUtilities.getPackage(FileUtilities.getPsiDirectory(project, location));
        task = task.setTaskClass(aPackage + "." + task.name);
        task = task.setLocation(location);
        return task;
    }
}
