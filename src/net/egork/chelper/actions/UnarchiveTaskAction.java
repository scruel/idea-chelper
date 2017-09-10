package net.egork.chelper.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.exception.TaskCorruptException;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.util.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class UnarchiveTaskAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        if (!ProjectUtils.isEligible(e.getDataContext()))
            return;
        final Project project = ProjectUtils.getProject(e.getDataContext());
        FileChooserDialog dialog = FileChooserFactory.getInstance().createFileChooser(
            new FileChooserDescriptor(true, false, false, false, false, true) {
                @Override
                public boolean isFileSelectable(VirtualFile file) {
                    return super.isFileSelectable(file) && ("task".equals(file.getExtension()) || "tctask".equals(file.getExtension()));
                }

                @Override
                public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
                    return super.isFileVisible(file, showHiddenFiles) &&
                        (file.isDirectory() || "task".equals(file.getExtension()) || "tctask".equals(file.getExtension()));
                }
            }, project, null);
        final VirtualFile[] files = dialog.choose(project, FileUtils.getFile(project, ProjectUtils.getData(project).archiveDirectory));
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            public void run() {
                try {
                    for (VirtualFile taskFile : files) {
                        try {
                            if ("task".equals(taskFile.getExtension())) {
                                Task task = Task.load(new InputReader(taskFile.getInputStream()));
                                if (task == null) {
                                    throw new TaskCorruptException();
                                }
                                PsiDirectory baseDirectory = FileUtils.getPsiDirectory(project, task.location);
                                if (baseDirectory == null) {
                                    Messenger.publishMessage("Directory where task was located is no longer exists",
                                        NotificationType.ERROR);
                                    return;
                                }
                                task.saveTask(new OutputWriter(
                                    baseDirectory.createFile(ArchiveAction.canonize(task.name) + ".task").getVirtualFile().getOutputStream(null)));
                                Collection<String> toCopy = new ArrayList<String>();
                                Collections.addAll(toCopy, task.taskClass, task.checkerClass);
                                String aPackage = FileUtils.getPackage(FileUtils.getPsiDirectory(project, task.location));
                                if (aPackage == null || aPackage.isEmpty()) {
                                    int result = JOptionPane.showOptionDialog(null, "Task location is not under source or in default" +
                                            "package, do you want to put it in default directory instead?", "Restore task",
                                        JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
                                        IconLoader.getIcon("/icons/restore.png"), null, null);
                                    if (result == JOptionPane.YES_OPTION) {
                                        String defaultDirectory = ProjectUtils.getData(project).defaultDirectory;
                                        baseDirectory = FileUtils.getPsiDirectory(project, defaultDirectory);
                                        aPackage = FileUtils.getPackage(FileUtils.getPsiDirectory(project, defaultDirectory));
                                        task = task.setLocation(defaultDirectory);
                                    }
                                }

                                for (String className : toCopy) {
                                    String fullClassName = className;
                                    int position = className.lastIndexOf('.');
                                    if (position != -1)
                                        className = className.substring(position + 1);
                                    VirtualFile file = taskFile.getParent().findChild(className + ".java");
                                    if (file != null) {
                                        Document fileContent = FileUtils.readDocumentFile(file);
                                        if (aPackage != null && !aPackage.isEmpty()) {
                                            fileContent = CodeGenerationUtils.changeDocPackage(fileContent, aPackage);
                                            String fqn = aPackage + "." + className;
                                            if (task.taskClass.equals(fullClassName))
                                                task = task.setTaskClass(fqn);
                                            else if (task.checkerClass.equals(fullClassName))
                                                task = task.setCheckerClass(fqn);
                                            else {
                                                for (int i = 0; i < task.testClasses.length; i++) {
                                                    if (task.testClasses[i].equals(fqn)) {
                                                        task.testClasses[i] = fqn;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        FileUtils.writeTextFile(project, baseDirectory, className + ".java", fileContent);
                                    }
                                }
                                ProjectUtils.createConfiguration(project, task, true);
                            } else if ("tctask".equals(taskFile.getExtension())) {
                                TopCoderTask task = TopCoderTask.load(new InputReader(taskFile.getInputStream()));
                                PsiDirectory baseDirectory = FileUtils.getPsiDirectory(project, ProjectUtils.getData(project).defaultDirectory);
                                if (baseDirectory == null) {
                                    Messenger.publishMessage("Directory where task was located is no longer exists",
                                        NotificationType.ERROR);
                                    return;
                                }
                                task.saveTask(new OutputWriter(baseDirectory.createFile(task.name + ".tctask").getVirtualFile().
                                    getOutputStream(null)));
                                List<String> toCopy = new ArrayList<String>();
                                VirtualFile sourceFile = taskFile.getParent().findChild(task.name + ".java");
                                if (sourceFile != null)
                                    FileUtils.writeTextFile(project, baseDirectory, task.name + ".java", FileUtils.readDocumentFile(sourceFile));
                                Collections.addAll(toCopy, task.testClasses);
                                for (String className : toCopy) {
                                    int position = className.lastIndexOf('.');
                                    if (position != -1)
                                        className = className.substring(position + 1);
                                    VirtualFile file = taskFile.getParent().findChild(className + ".java");
                                    if (file != null)
                                        FileUtils.writeTextFile(project, baseDirectory, className + ".java", FileUtils.readDocumentFile(file));
                                }
                                ProjectUtils.createConfiguration(project, task, true);
                            }
                            FileUtils.deleteTaskIfExists(project, PsiUtil.getPsiFile(project, taskFile), false);
                        } catch (IncorrectOperationException e) {
                            Messenger.publishMessage("Error unarchiving file '" + taskFile.getPath() +
                                "' caused by " + e.getMessage(), NotificationType.ERROR);
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
