package net.egork.chelper.util;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
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
    //for task map
    public static final String TASK_KEY = "TASK_KEY";
    public static final String TASK_SOURCE_KEY = "TASK_SOURCE_KEY";
    public static final String TASK_DATA_KEY = "TASK_DATA_KEY";

    private TaskUtils() {
    }

    public static void createSourceFile(Project project, Task task) {
        SolutionGenerator.createSourceFile(project, task);
    }

    public static VirtualFile getFile(Project project, String name, String location) {
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
    public static Map<String, Object> getTaskMapWitFile(Project project, VirtualFile file) {
        String extension = file.getExtension();
        if (!("java".equals(extension) || "task".equals(extension) || "tctask".equals(extension))) {
            return null;
        }

        Map<String, Object> taskMap;
        if ("java".equals(extension)) {
            taskMap = getTaskMapWithSourceFile(project, file);
        } else {
            taskMap = getTaskMapWithDataFile(project, file);
        }
        return taskMap;
    }

    /**
     * @param project
     * @param file    .java file
     * @return associated Task
     */
    public static Map<String, Object> getTaskMapWithSourceFile(Project project, VirtualFile file) {
        VirtualFile parentFile = file.getParent();
        String fileName = file.getNameWithoutExtension();
        VirtualFile taskFile;
        if ((taskFile = parentFile.findChild(fileName + ".task")) != null) {
            return getTaskMapWithDataFile(project, taskFile);
        }
        if ((taskFile = parentFile.findChild(fileName + ".tctask")) != null) {
            return getTaskMapWithDataFile(project, taskFile);
        }
        PsiDirectory directory = FileUtils.getPsiDirectory(project, FileUtils.getRelativePath(project.getBaseDir(), parentFile));
        if (directory == null) return null;
        for (VirtualFile virtualFile : parentFile.getChildren()) {
            if ("task".equals(virtualFile.getExtension()) || "tctask".equals(virtualFile.getExtension())) {
                Map<String, Object> map = getTaskMapWithDataFile(project, virtualFile);
                if (map == null) continue;
                if (file.equals(map.get(TASK_SOURCE_KEY)))
                    return map;
            }
        }
        return null;
    }

    /**
     * @param project
     * @param file    .task file
     * @return associated Task
     */
    public static Map<String, Object> getTaskMapWithDataFile(Project project, VirtualFile file) {
        Map<String, Object> map = new HashMap<String, Object>();
        TaskBase taskBase = FileUtils.readTask(project, FileUtils.getRelativePath(project.getBaseDir(), file));
        if (taskBase == null) {
            taskBase = FileUtils.readTopCoderTask(project, FileUtils.getRelativePath(project.getBaseDir(), file));
        }
        if (taskBase == null) return null;

        VirtualFile taskSourceFile = null;
        if (taskBase instanceof Task) {
            Task task = (Task) taskBase;
            String sourceFileName = task.taskClass;
            int i = StringUtilRt.lastIndexOf(sourceFileName, '.', 0, sourceFileName.length());
            sourceFileName = i < 0 ? sourceFileName : sourceFileName.substring(i + 1) + ".java";
            taskSourceFile = file.getParent().findChild(sourceFileName);
        } else if (taskBase instanceof TopCoderTask) {
            TopCoderTask task = (TopCoderTask) taskBase;
            taskSourceFile = TaskUtils.getFile(project, task.name, ProjectUtils.getData(project).defaultDirectory);
        }
        if (null == taskSourceFile) return null;

        map.put(TASK_KEY, taskBase);
        map.put(TASK_DATA_KEY, file);
        map.put(TASK_SOURCE_KEY, taskSourceFile);
        return map;
    }

    /**
     * @param runManager
     * @param sourceFile .java(source)
     * @return
     */
    public static RunConfiguration GetConfSettingsBySourceFile(Project project, RunManagerImpl runManager, VirtualFile sourceFile) {
        if (sourceFile == null)
            return null;
        for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
            if (!ProjectUtils.isSupported(configuration) || !ProjectUtils.isValidConfigurationAndDeleteIfNot(configuration)) {
                continue;
            }
            if (configuration instanceof TopCoderConfiguration) {
                TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
                if (sourceFile.equals(TaskUtils.getFile(project, task.name, ProjectUtils.getData(project).defaultDirectory))) {
                    return configuration;
                }
            } else if (configuration instanceof TaskConfiguration) {
                Task task = ((TaskConfiguration) configuration).getConfiguration();
                if (sourceFile.equals(FileUtils.getFileByFQN(configuration.getProject(), task.taskClass))) {
                    return configuration;
                }
            }
        }
        return null;
    }

    public static RunConfiguration GetConfSettingsBySourceFile(Project project, VirtualFile sourceFile) {
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
        return GetConfSettingsBySourceFile(project, runManager, sourceFile);
    }

    /**
     * 自动根据传入 VirtualFile 修复 task，防止报错。
     *
     * @param project
     * @param task
     * @param taskDataFile
     * @return
     */
    public static TaskBase fixedTaskByPath(Project project, TaskBase task, VirtualFile taskDataFile) {
        if (!FileUtils.isJavaDirectory(PsiManager.getInstance(project).findDirectory(taskDataFile.getParent())))
            return task;

        TaskBase fixed = null;
        if (task instanceof Task) {
            fixed = _fixedTaskByPath(project, (Task) task, taskDataFile);
        } else if (task instanceof TopCoderTask) {
            fixed = _fixedTopCoderTaskByPath(project, (TopCoderTask) task, taskDataFile);
        }
        if (fixed == null) return null;
        if (fixed != task) {
            if (task instanceof Task) {
                new TaskConfiguration(project, task.name, (Task) task,
                    TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
            } else if (task instanceof TopCoderTask) {
                new TopCoderConfiguration(project, task.name, (TopCoderTask) task,
                    TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]);
            }
        }
        return fixed;
    }

    private static Task _fixedTaskByPath(Project project, Task task, VirtualFile taskDataFile) {
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
        if (location.equals(task.location)) return task;
        String aPackage = FileUtils.getPackage(FileUtils.getPsiDirectory(project, location));
        if (aPackage == null) return task;
        task = task.setTaskClass(aPackage + "." + task.name);
        task = task.setLocation(location);
        return task;
    }

    private static TopCoderTask _fixedTopCoderTaskByPath(Project project, TopCoderTask task, VirtualFile taskDataFile) {
        //TODO haven't try topcoder.
        if (task == null || taskDataFile == null) return null;
        return task;
    }
}
