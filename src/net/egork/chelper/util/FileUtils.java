package net.egork.chelper.util;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
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
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class FileUtils {
    private FileUtils() {
    }

    public static boolean deleteTaskIfExists(final PsiFile psiFile) {
        if (psiFile == null) {
            return false;
        }
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            @Override
            public void run() {
                PsiFile _psiFile = psiFile;
//            res = Files.deleteIfExists(file.toPath());
                String nameWithOutExtension = _psiFile.getName();
                int i = StringUtilRt.lastIndexOf(nameWithOutExtension, '.', 0, nameWithOutExtension.length());
                nameWithOutExtension = i < 0 ? nameWithOutExtension : nameWithOutExtension.substring(0, i);
                PsiDirectory parentFile = _psiFile.getParent();
                if (parentFile != null) {
                    _psiFile = parentFile.findFile(nameWithOutExtension + ".java");
                    if (_psiFile != null) {
                        ProjectUtils.removeConfiguration(TaskUtils.GetConfSettingsBySourceFile(psiFile.getProject(), _psiFile.getVirtualFile()));
                        _psiFile.delete();
                    }
                    _psiFile = parentFile.findFile(nameWithOutExtension + ".task");
                    if (_psiFile != null) {
                        _psiFile.delete();
                    }
                }
//                Messenger.publishMessage("Unable to delete file " + _psiFile.getVirtualFile().getPath(),NotificationType.ERROR);


            }
        });
        return true;
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

    public static IdeView getView(DataContext dataContext) {
        return LangDataKeys.IDE_VIEW.getData(dataContext);
    }

    public static boolean isJavaDirectory(PsiDirectory directory) {
        return directory != null && JavaDirectoryService.getInstance().getPackage(directory) != null;
    }

    public static VirtualFile writeTextFile(final VirtualFile location, final String fileName, final String fileContent) {
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            @Override
            public void run() {
                if (location == null) {
                    return;
                }
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(null);
                    stream.write(fileContent.getBytes(Charset.forName("UTF-8")));
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
        });
        return location.findChild(fileName);
    }

    public static boolean isValiDirectory(Project project, String location) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        return !(directory == null || !directory.isValid());
    }

    public static String readTextFile(VirtualFile file) {
        try {
            return VfsUtil.loadText(file);
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean isValidClass(Project project, String clazz) {
        try {
            Class.forName(clazz).newInstance();
        } catch (IllegalAccessException ignore) {
        } catch (InstantiationException ignore) {
        } catch (ClassNotFoundException igonre) {
            if (MainFileTemplate.getClass(project, clazz) == null)
                return false;
        }
        return true;
    }

    public static PsiDirectory getPsiDirectory(Project project, String location) {
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return PsiManager.getInstance(project).findDirectory(file);
    }

    public static VirtualFile getFile(Project project, String location) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }
        return baseDir.findFileByRelativePath(location);
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

    public static PsiFile getPsiFile(Project project, String location) {
        VirtualFile file = getFile(project, location);
        if (file == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(file);
    }

    public static VirtualFile createDirectoryIfMissing(final Project project, final String location) {
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
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
        });
        return getFile(project, location);
    }

    public static void synchronizeFile(VirtualFile file) {
        Document doc = FileDocumentManager.getInstance().getDocument(file);
        if (doc == null) return;
        FileDocumentManager.getInstance().saveDocument(doc);
    }

    public static String getWebPageContent(String address) {
        return getWebPageContent(address, "UTF-8");
    }

    public static String getWebPageContent(String address, String charset) {
        for (int i = 0; i < 10; i++) {
            try {
                URL url = new URL(address);
                InputStream input = url.openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(charset)));
                StringBuilder builder = new StringBuilder();
                String s;
                while ((s = reader.readLine()) != null) {
                    builder.append(s).append('\n');
                }
                reader.close();
                return new String(builder.toString().getBytes("UTF-8"), "UTF-8");
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    public static Task readTask(Project project, String fileName) {
        VirtualFile vFile = getFile(project, fileName);
        if (vFile == null) {
            return null;
        }
        return Task.loadTask(new InputReader(getInputStream(vFile)));
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
        ExecuteUtils.executeStrictWriteAction(new Runnable() {
            @Override
            public void run() {
                VirtualFile location = FileUtils.getFile(project, locationName);
                if (location == null) {
                    return;
                }
                OutputStream stream = null;
                try {
                    VirtualFile file = location.findOrCreateChildData(null, fileName);
                    stream = file.getOutputStream(null);
                    configuration.saveTask(new OutputWriter(stream));
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
        });
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
        return name.matches("[a-zA-Z_$][a-zA-Z\\d_$]*");
    }

    public static String createTaskClass(Project project, Task task, String location, String name) {
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        String mainClass = CodeGenerationUtils.createStub(project, task, location, name);
        if (directory.findChild(name + ".java") == null) {
            writeTextFile(directory, name + ".java", mainClass);
        }
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
        ProjectUtils.openElement(project, ProjectUtils.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createTestClass(Project project, String location, String name, Task task) {
        String mainClass = CodeGenerationUtils.createTestStub(project, task, location, name);
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        writeTextFile(directory, name + ".java", mainClass);
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        String fqn = aPackage + "." + name;
        ProjectUtils.openElement(project, ProjectUtils.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createTopCoderTestClass(Project project, String location, String name) {
        VirtualFile directory = FileUtils.createDirectoryIfMissing(project, location);
        PsiDirectory psiDirectory = getPsiDirectory(project, location);
        String aPackage = getPackage(psiDirectory);
        String mainClass = CodeGenerationUtils.createTopCoderTestStub(project, aPackage, name);
        writeTextFile(directory, name + ".java", mainClass);
        String fqn = aPackage + "." + name;
        ProjectUtils.openElement(project, ProjectUtils.getPsiElement(project, fqn));
        return fqn;
    }

    public static String createIfNeeded(Project project, Task task, String taskClass, String location) {
        if (taskClass.indexOf('.') == -1) {
            taskClass = createTaskClass(project, task, location, taskClass);
        }
        return taskClass;
    }

    /**
     * @param project
     * @param fqn     className
     * @return
     */
    public static VirtualFile getFileByFQN(Project project, String fqn) {
        if (fqn == null)
            return null;
        PsiElement main = JavaPsiFacade.getInstance(project).findClass(fqn, GlobalSearchScope.allScope(project));
        return main == null ? null : main.getContainingFile() == null ? null : main.getContainingFile().getVirtualFile();
    }
}
