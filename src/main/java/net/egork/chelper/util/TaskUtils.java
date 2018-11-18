package net.egork.chelper.util;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.configurations.TaskConfiguration;
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

    /**
     * Standardize the file name so that it can be saved.
     *
     * @param filename
     * @return
     */
    public static String canonize(String filename) {
        filename = filename.replaceAll("[\\\\?%*:|\"<>/]", "-");
        while (filename.endsWith("."))
            filename = filename.substring(0, filename.length() - 1);
        return filename;
    }

    private static String getTaskSourceFileLocation(TaskBase taskBase) {
        if (taskBase instanceof Task) {
            Task task = (Task) taskBase;
            String location = task.location;
            String taskClass = task.taskClass;
            if (location == null || taskClass == null) return null;
            String res = location + "/" + ProjectUtils.getSimpleName(taskClass) + ".java";
            return res;
        } else if (taskBase instanceof TopCoderTask) {
            //TODO haven't use TopCoder
        }
        return null;
    }

    public static VirtualFile getFile(Project project, String name, String location) {
        return FileUtils.getFile(project, location + "/" + name + ".java");
    }

    private static String getTaskDataFileLocation(TaskBase taskBase) {
        if (taskBase instanceof Task) {
            Task task = (Task) taskBase;
            return getTaskFileName(task.location, task.name);
        } else if (taskBase instanceof TopCoderTask) {
            TopCoderTask task = (TopCoderTask) taskBase;
            //TODO haven't use TopCoder
        }
        return null;
    }

    public static String getTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + TaskUtils.canonize(name) + ".task";
        return null;
    }

    public static String getTopCoderTaskFileName(String location, String name) {
        if (location != null && name != null)
            return location + "/" + name + ".tctask";
        return null;
    }

    /**
     * Get the associated .task file
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
        TaskBase taskBase = FileUtils.readTask(file);
        if (taskBase == null) {
            taskBase = FileUtils.readTopCoderTask(project, FileUtils.getRelativePath(project.getBaseDir(), file));
        }
        if (taskBase == null) return null;

        VirtualFile taskSourceFile = null;
        if (taskBase instanceof Task) {
            Task task = (Task) taskBase;
            String sourceFileName = ProjectUtils.getSimpleName(task.taskClass) + ".java";
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
    public static RunConfiguration getConfSettingsBySourceFile(Project project, RunManagerImpl runManager, VirtualFile sourceFile) {
        if (sourceFile == null)
            return null;
        for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
            if (!ProjectUtils.isValidConfigurationOrDeleteIfNot(configuration)) {
                continue;
            }
            if (configuration instanceof TopCoderConfiguration) {
                TopCoderTask task = ((TopCoderConfiguration) configuration).getConfiguration();
                VirtualFile configurationFile = TaskUtils.getFile(project, task.name, ProjectUtils.getData(project).defaultDirectory);
                if (configurationFile != null && sourceFile.equals(configurationFile)) {
                    return configuration;
                }
            } else if (configuration instanceof TaskConfiguration) {
                Task task = ((TaskConfiguration) configuration).getConfiguration();
                String fileLocation = FileUtils.getRelativePath(project.getBaseDir(), sourceFile);
                if (fileLocation == null) return null;
                String sourceLocation = getTaskSourceFileLocation(task);
                String dataLocation = getTaskDataFileLocation(task);
                if (fileLocation.equals(sourceLocation) || fileLocation.equals(dataLocation))
                    return configuration;
            }
        }
        return null;
    }

    public static RunConfiguration getConfSettingsBySourceFile(Project project, VirtualFile sourceFile) {
        RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
        return getConfSettingsBySourceFile(project, runManager, sourceFile);
    }

    /**
     * Fixes .task file by parameters.
     * When invoke this method, must be sure about task is valid(both source and data file exits).
     * return the fixed {@link TaskBase} by file(avoid error or exception), return original if it can't be fixed.
     *
     * @param project
     * @param task
     * @param taskFile either dataFile or taskFile.
     * @return
     */
    public static TaskBase fixedTaskByTaskFile(Project project, TaskBase task, VirtualFile taskFile) {
        if (!FileUtils.isJavaDirectory(PsiManager.getInstance(project).findDirectory(taskFile.getParent())))
            return task;

        TaskBase fixed = null;
        if (task instanceof Task) {
            fixed = _fixedTaskByTaskFile(project, (Task) task, taskFile);
        } else if (task instanceof TopCoderTask) {
            fixed = _fixedTopCoderTaskByTaskFile(project, (TopCoderTask) task, taskFile);
        }
        if (fixed == null) return null;
        return fixed;
    }

    /**
     * return the fixed {@link Task} , return original if it can't be fixed.
     *
     * @param project
     * @param task
     * @param taskFile
     * @return
     */
    private static Task _fixedTaskByTaskFile(Project project, Task task, VirtualFile taskFile) {
        if (task == null || taskFile == null) return task;
        PsiClass checkerClazz = MainFileTemplate.getClass(project, task.checkerClass);
        if (checkerClazz == null) {
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
        String location = FileUtils.getRelativePath(project.getBaseDir(), taskFile.getParent());
        if (location.equals(task.location)) return task;
        String aPackage = FileUtils.getPackage(FileUtils.getPsiDirectory(project, location));
        if (aPackage == null) return task;
        task = task.setTaskClass(aPackage + "." + ProjectUtils.getSimpleName(task.taskClass));
        task = task.setLocation(location);
        return task;
    }


    /**
     * return the fixed {@link TopCoderTask} , return original if it can't be fixed.
     *
     * @param project
     * @param task
     * @param taskFile
     * @return
     */
    private static TopCoderTask _fixedTopCoderTaskByTaskFile(Project project, TopCoderTask task, VirtualFile taskFile) {
        //TODO haven't try topcoder.
        if (task == null || taskFile == null) return task;
        return task;
    }
}
