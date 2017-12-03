package net.egork.chelper.util;

import com.intellij.ide.IdeView;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.task.TopCoderTask;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class FileUtils {
    private static final StackTraceLogger LOG = new StackTraceLogger(FileUtils.class);

    private FileUtils() {
    }

    /**
     * @param psiFile either source or file
     */
    public static void deleteTaskIfExists(final Project project, final PsiFile psiFile, final boolean removeConf) {
        if (psiFile == null) {
            return;
        }
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile vFile = psiFile.getVirtualFile();
                Map<String, Object> taskMap = TaskUtils.getTaskMapWitFile(project, vFile);
                if (taskMap == null) {
                    return;
                }
                VirtualFile sourceFile = (VirtualFile) taskMap.get(TaskUtils.TASK_SOURCE_KEY);
                VirtualFile dataFile = (VirtualFile) taskMap.get(TaskUtils.TASK_DATA_KEY);
                if (removeConf)
                    ProjectUtils.removeConfigurationIfExists(TaskUtils.getConfSettingsBySourceFile(psiFile.getProject(), sourceFile));
                try {
                    sourceFile.delete(null);
                    dataFile.delete(null);
                } catch (IOException e) {
                    Messenger.publishMessage("Unable to delete file: " + e, NotificationType.ERROR);
                }
            }
        }, false);
    }

    public static Properties loadProperties(VirtualFile file) {
        InputStream is = getInputStream(file);
        if (is == null) {
            return null;
        }
        Properties properties = new Properties();
        try {
            properties.load(is);
            return properties;
        } catch (IOException e) {
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static InputStream getInputStream(VirtualFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public static IdeView getView(DataContext dataContext) {
        return LangDataKeys.IDE_VIEW.getData(dataContext);
    }

    public static boolean isJavaDirectory(PsiDirectory directory) {
        return directory != null && JavaDirectoryService.getInstance().getPackage(directory) != null;
    }

    public static VirtualFile writeTextFile(final VirtualFile location, final String fileName, final String context) {
        if (location == null) {
            return null;
        }
        LOG.printMethodInfoWithNamesAndValues(true, "location", location, "fileName", fileName, "context", context);
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(FileDocumentManager.getInstance());
                    stream.write(context.getBytes(Charset.forName("UTF-8")));
                } catch (IOException ignored) {
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }, true);
        LOG.printMethodInfoWithNamesAndValues(false, "location", location, "fileName", fileName, "context", context, "result", location.findChild(fileName));
        return location.findChild(fileName);
    }

    /**
     * detectable creation
     *
     * @param project
     * @param directory
     * @param fileName
     * @return
     */
    public static PsiFile writeTextFile(final Project project, final PsiDirectory directory, final String fileName, final String context) {
        if (directory == null) {
            return null;
        }
        LOG.printMethodInfoWithNamesAndValues(true, "directory", directory, "fileName", fileName, "fileContent", context);
        ExecuteUtils.executeWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                _writeTextFile(project, directory, fileName, context);
            }
        }, true);
        LOG.printMethodInfoWithNamesAndValues(false, "directory", directory, "fileName", fileName, "context", context, "result", directory.findFile(fileName));
        return directory.findFile(fileName);
    }

    private static void _writeTextFile(Project project, PsiDirectory directory, String fileName, String context) {
        LOG.printMethodInfoWithNamesAndValues(true, "directory", directory, "fileName", fileName, "fileContent", context);
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        PsiFile psiFile = directory.findFile(fileName);
        FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName);
        if (psiFile == null) {
            if (type.isBinary()) {
                type = UnknownFileType.INSTANCE;
            }
            psiFile = factory.createFileFromText(fileName, type, context);
            directory.add(psiFile);
        } else {
            VirtualFile vFile = psiFile.getVirtualFile();
            try {
                BufferedOutputStream out = new BufferedOutputStream(vFile.getOutputStream(FileDocumentManager.getInstance()));
                out.write(context.getBytes("UTF-8"));
                out.close();
            } catch (IOException ignore) {
            }
        }
        LOG.printMethodInfoWithNamesAndValues(false, "directory", directory, "fileName", fileName, "fileContent", context);
    }

    public static boolean isValidDirectory(Project project, String location) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        return !(directory == null || !directory.isValid());
    }

    public static String readTextFile(VirtualFile file) {
        try {
            return EncodingUtils.reformatLineSeparator(VfsUtil.loadText(file));
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isValidClass(Project project, String clazz) {
        return MainFileTemplate.getClass(project, clazz) != null;
    }

    public static String getRelativePath(VirtualFile baseDir, VirtualFile file) {
        if (file == null) {
            return null;
        }
        if (baseDir == null) {
            return file.getPath();
        }
        if (!isChild(baseDir, file)) {
            return null;
        }
        String basePath = baseDir.getPath();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        String filePath = file.getPath();
        return filePath.substring(Math.min(filePath.length(), basePath.length()));
    }

    public static String getPackage(PsiDirectory directory) {
        if (directory == null)
            return null;
        PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
        String packageName = null;
        if (aPackage != null) {
            packageName = aPackage.getQualifiedName();
        }
        return packageName;
    }

    public static String getFQN(PsiDirectory directory, String name) {
        String packageName = getPackage(directory);
        if (packageName == null || packageName.length() == 0) {
            return name;
        }
        return packageName + "." + name;
    }

    public static VirtualFile getFile(Project project, String location) {
        if (location == null) return null;
        VirtualFile file = project.getBaseDir();
        if (file == null) {
            return null;
        }
        return file.findFileByRelativePath(location);
    }

    public static PsiFile getPsiFile(Project project, String location) {
        if (location == null) return null;
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return CompatibilityUtils.getPsiFile(project, file);
    }

    public static PsiDirectory getDirectory(DataContext dataContext) {
        IdeView view = getView(dataContext);
        if (view == null) {
            return null;
        }
        PsiDirectory[] directories = view.getDirectories();
        if (directories.length != 1) {
            return null;
        }
        return directories[0];
    }

    public static PsiDirectory getPsiDirectory(Project project, String location) {
        if (location == null) return null;
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return PsiManager.getInstance(project).findDirectory(file);
    }

    public static VirtualFile createDirectoryIfMissing(final Project project, final String location) {
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    return;
                }
                try {
                    VfsUtil.createDirectoryIfMissing(baseDir, location);
                } catch (IOException ignored) {
                }
            }
        }, true);
        return getFile(project, location);
    }

    public static PsiDirectory createPsiDirectoryIfMissing(final Project project, final String location) {
        createDirectoryIfMissing(project, location);
        return getPsiDirectory(project, location);
    }

    public static void synchronizeFile(VirtualFile file) {
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) return;
        FileDocumentManager.getInstance().saveDocument(doc);
    }

    public static Task readTask(Project project, String fileName) {
        VirtualFile vFile = getFile(project, fileName);
        return readTask(vFile);
    }

    public static Task readTask(VirtualFile vFile) {
        if (vFile == null) {
            return null;
        }
        return Task.load(new InputReader(getInputStream(vFile)));
    }

    public static TopCoderTask readTopCoderTask(Project project, String fileName) {
        VirtualFile vFile = getFile(project, fileName);
        if (vFile == null) {
            return null;
        }
        return TopCoderTask.load(new InputReader(getInputStream(vFile)));
    }

    public static void saveConfiguration(final String locationName, final String fileName, final TaskBase configuration, final Project project) {
        if (locationName == null) {
            return;
        }
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                PsiDirectory locationFile = FileUtils.getPsiDirectory(project, locationName);
                if (locationFile == null) {
                    return;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                configuration.saveTask(new OutputWriter(out));
                String content;
                try {
                    content = out.toString("UTF-8");
                } catch (UnsupportedEncodingException ignore) {
                    content = out.toString();
                }
                _writeTextFile(project, locationFile, fileName, content);
            }
        }, false);
    }

    public static boolean isChild(VirtualFile parent, VirtualFile child) {
        if (!parent.isDirectory()) {
            return false;
        }
        String parentPath = parent.getPath();
        if (!parentPath.endsWith("/")) {
            parentPath += "/";
        }
        String childPath = child.getPath();
        if (child.isDirectory() && !childPath.endsWith("/")) {
            childPath += "/";
        }
        return childPath.startsWith(parentPath);
    }

    public static boolean isValidClassName(String name) {
        if (name == null) return false;
        return name.matches("[a-zA-Z_$][a-zA-Z\\d_$]*");
    }

    public static String createTaskClass(Project project, Task task, String name, String location) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        String mainClass = CodeGenerationUtils.createStub(project, task, location, name);
        if (directory.findFile(name + ".java") == null) {
            writeTextFile(project, directory, name + ".java", mainClass);
        }
        return getTaskClassName(project, name, location);
    }

    public static String getTaskClassName(Project project, String name, String location) {
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        return aPackage + "." + name;
    }

    public static String createCheckerClass(Project project, String location, String name, Task task) {
        String mainClass = CodeGenerationUtils.createCheckerStub(project, task, location, name);
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        ProjectUtils.openElement(project, ProjectUtils.getPsiElementByFQN(project, fqn));
        return fqn;
    }

    public static String createTestClass(Project project, String location, String name, Task task) {
        String mainClass = CodeGenerationUtils.createTestStub(project, task, location, name);
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        ProjectUtils.openElement(project, ProjectUtils.getPsiElementByFQN(project, fqn));
        return fqn;
    }

    public static String createTopCoderTestClass(Project project, String location, String name) {
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        String mainClass = CodeGenerationUtils.createTopCoderTestStub(project, aPackage, name);
        writeTextFile(directory, name + ".java", mainClass);
        String fqn = aPackage + "." + name;
        ProjectUtils.openElement(project, ProjectUtils.getPsiElementByFQN(project, fqn));
        return fqn;
    }

    /**
     * create taskClass if {@param taskClass} not valid.
     *
     * @param project
     * @param task
     * @param taskClass
     * @param location
     * @return
     */
    public static String createIfNeeded(Project project, Task task, String taskClass, String location) {
        if (!taskClass.contains(".")) {
            taskClass = createTaskClass(project, task, taskClass, location);
        }
        return taskClass;
    }

    public static VirtualFile getFileByFQN(Project project, String fqn) {
        PsiFile main = getPsiFileByFQN(project, fqn);
        return main == null ? null : main.getVirtualFile();
    }

    public static PsiFile getPsiFileByFQN(Project project, String fqn) {
        if (fqn == null)
            return null;
        PsiClass main = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        return main == null ? null : main.getContainingFile();
    }
}
