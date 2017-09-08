package net.egork.chelper.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import net.egork.chelper.actions.ArchiveAction;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.codegeneration.SolutionGenerator;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.task.TopCoderTask;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class TaskUtils {
    private TaskUtils() {
    }

    public static void createSourceFile(Task task, Project project) {
        SolutionGenerator.createSourceFile(task, project);
    }

    public static VirtualFile getFile(String location, String name, Project project) {
        return FileUtils.getFile(project, location + "/" + name + ".java");
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
     * 获取关联的 .task 文件
     *
     * @param file .java / .task file
     * @return associated Task
     */
    public static Map<String, Object> getTaskMapWitFile(VirtualFile file, Project project) {
        String extension = file.getExtension();
        if (!("java".equals(extension) || "task".equals(extension) || "tctask".equals(extension))) {
            return null;
        }

        Map<String, Object> taskMap;
        if ("java".equals(extension)) {
            taskMap = getTaskMapWithSourceFile(file, project);
        } else {
            taskMap = getTaskMapWithDataFile(file, project);
        }
        return taskMap;
    }

    /**
     * @param file    .java file
     * @param project
     * @return associated Task
     */
    private static Map<String, Object> getTaskMapWithSourceFile(VirtualFile file, Project project) {
        VirtualFile parentFile = file.getParent();
        String fileName = file.getNameWithoutExtension();
        VirtualFile taskFile;
        if ((taskFile = parentFile.findChild(fileName + ".task")) != null) {
            return getTaskMapWithDataFile(taskFile, project);
        }
        if ((taskFile = parentFile.findChild(fileName + ".tctask")) != null) {
            return getTaskMapWithDataFile(taskFile, project);
        }
        PsiDirectory directory = FileUtils.getPsiDirectory(project, FileUtils.getRelativePath(project.getBaseDir(), parentFile));
        if (directory == null) return null;
        for (VirtualFile virtualFile : parentFile.getChildren()) {
            if ("task".equals(virtualFile.getExtension()) || "tctask".equals(virtualFile.getExtension())) {
                Map<String, Object> map = getTaskMapWithDataFile(virtualFile, project);
                if (map == null) continue;
                if (file.equals(map.get(TaskBase.TASK_SOURCE_KEY)))
                    return map;
            }
        }
        return null;
    }

    /**
     * @param file    .task file
     * @param project
     * @return associated Task
     */
    private static Map<String, Object> getTaskMapWithDataFile(VirtualFile file, Project project) {
        Map<String, Object> map = new HashMap<String, Object>();
        TaskBase taskBase = FileUtils.readTask(FileUtils.getRelativePath(project.getBaseDir(), file), project);
        if (taskBase == null) {
            taskBase = FileUtils.readTopCoderTask(FileUtils.getRelativePath(project.getBaseDir(), file), project);
        }
        if (taskBase == null) return null;

        VirtualFile taskDataFile = null;
        if (taskBase instanceof Task) {
            Task task = (Task) taskBase;
            taskDataFile = FileUtils.getFileByFQN(task.taskClass, project);
        } else if (taskBase instanceof TopCoderTask) {
            TopCoderTask task = (TopCoderTask) taskBase;
            taskDataFile = TaskUtils.getFile(ProjectUtils.getData(project).defaultDirectory, task.name, project);
        }
        if (null == taskDataFile) return null;

        map.put(TaskBase.TASK_KEY, taskBase);
        map.put(TaskBase.TASK_DATA_KEY, file);
        map.put(TaskBase.TASK_SOURCE_KEY, taskDataFile);
        return map;
    }

    /**
     * 自动根据传入 VirtualFile 修复 task，防止报错。
     *
     * @param task
     * @param project
     * @param taskDataFile
     * @return
     */
    public static TaskBase taskOfFixedPath(TaskBase task, Project project, VirtualFile taskDataFile) {
        TaskBase fixed = null;
        if (task instanceof Task) {
            fixed = fixedTask(project, (Task) task, taskDataFile);
        } else if (task instanceof TopCoderTask) {
            fixed = fixedTopCoderTask(project, (TopCoderTask) task, taskDataFile);
        }
        if (fixed == null) return null;
        if (fixed != task) {
            if (task instanceof Task) {
                new TaskConfiguration(task.name, project, (Task) task,
                    TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
            } else if (task instanceof TopCoderTask) {
                new TopCoderConfiguration(task.name, project, (TopCoderTask) task,
                    TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
            }
        }
        return fixed;
    }

    private static Task fixedTask(Project project, Task task, VirtualFile taskDataFile) {
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
        String location = FileUtils.getRelativePath(project.getBaseDir(), taskDataFile.getParent());
        if (location.equals(task.location))
            return task;
        String aPackage = FileUtils.getPackage(FileUtils.getPsiDirectory(project, location));
        task = task.setTaskClass(aPackage + "." + task.name);
        task = task.setLocation(location);
        return task;
    }

    private static TopCoderTask fixedTopCoderTask(Project project, TopCoderTask task, VirtualFile taskDataFile) {
        //TODO haven't try topcoder.
        if (task == null || taskDataFile == null) return null;
        return task;
    }
}
