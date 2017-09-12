package net.egork.chelper.util;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.ui.UIUtil;
import net.egork.chelper.ChromeParser;
import net.egork.chelper.ProjectData;
import net.egork.chelper.actions.TopCoderAction;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.codegeneration.CodeGenerationUtils;
import net.egork.chelper.configurations.TaskConfiguration;
import net.egork.chelper.configurations.TaskConfigurationType;
import net.egork.chelper.configurations.TopCoderConfiguration;
import net.egork.chelper.configurations.TopCoderConfigurationType;
import net.egork.chelper.parser.Parser;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;
import net.egork.chelper.tester.NewTester;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ProjectUtils {
    private ProjectUtils() {
    }

    public static final String PROJECT_NAME = "CHelper";

    private static Map<Project, ProjectData> eligibleProjects = new HashMap<Project, ProjectData>();
    // TODO: The existence of non-persistent defaultConfiguration together with persistent ProjectData is a bit weird.
    // It would be natural for everything to be persistent.
    private static Task defaultConfiguration = new Task(null, TestType.SINGLE, StreamConfiguration.STANDARD,
        StreamConfiguration.STANDARD, new Test[0], null, "-Xmx256m -Xss64m", "Main", null,
        PEStrictChecker.class.getCanonicalName(), "", new String[0], null, "", true, null, null, false, false,
        "TaskClass.template");
    private static Parser defaultParser = Parser.PARSERS[0];

    public static void addListeners() {
        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
            @Override
            public void projectOpened(final Project project) {
                final ProjectData configuration = ProjectData.load(project);
                if (configuration != null) {
                    eligibleProjects.put(project, configuration);
                    TopCoderAction.start(project);
                    ensureLibraryAndData(project);
                    CodeGenerationUtils.createTemplatesIfNeeded(project);
                    DumbService.getInstance(project).smartInvokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ChromeParser.checkInstalled(project, configuration);
                        }
                    });
                    VirtualFileManager.getInstance().addVirtualFileListener(
                        new VirtualFileAdapter() {
                            @Override
                            public void fileDeleted(@NotNull final VirtualFileEvent event) {
                                DumbService.getInstance(project).smartInvokeLater(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            VirtualFile vFile = event.getFile();
                                            if ("java".equals(vFile.getExtension()) || "task".equals(vFile.getExtension()) || "tctask".equals(vFile.getExtension())) {
                                                RunConfiguration runConfiguration = TaskUtils.GetConfSettingsBySourceFile(project, vFile);
                                                ProjectUtils.removeConfigurationIfExists(runConfiguration);
                                            }
                                        }
                                    }
                                );
                            }
                        }
                    );
                }
            }

            @Override
            public void projectClosed(Project project) {
                eligibleProjects.remove(project);
            }
        });
    }

    public static PsiElement getPsiElement(Project project, String classFQN) {
        return JavaPsiFacade.getInstance(project).findClass(classFQN, GlobalSearchScope.allScope(project));
    }

    public static void ensureLibraryAndData(final Project project) {
        ExecuteUtils.executeStrictWriteActionAndWait(new Runnable() {
            @Override
            public void run() {
                ProjectUtils.ensureLibrary(project);
            }
        });
    }

    public static void ensureLibrary(Project project) {
        LibraryTable table = ProjectLibraryTable.getInstance(project);
        String path = TopCoderAction.getJarPathForClass(NewTester.class);
        if (path == null) {
            throw new RuntimeException("Could not find " + ProjectUtils.PROJECT_NAME + " jar!");
        }
        VirtualFile jar;
        jar = VirtualFileManager.getInstance().findFileByUrl(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, path) + JarFileSystem.JAR_SEPARATOR);

        if (jar == null) {
            jar = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        }
        if (jar == null) {
            throw new RuntimeException("Could not find " + ProjectUtils.PROJECT_NAME + " jar!");
        }
        Library library = table.getLibraryByName(PROJECT_NAME);
        if (library != null) {
            table.removeLibrary(library);
        }
        library = table.createLibrary(PROJECT_NAME);
        Library.ModifiableModel libraryModel = library.getModifiableModel();
        libraryModel.addRoot(jar, OrderRootType.CLASSES);
        libraryModel.commit();
        addLibray(project, library);
    }

    private static void addLibray(Project project, Library library) {
        final boolean[] res = new boolean[1];
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager.getInstance(module).orderEntries().forEachLibrary(new Processor<Library>() {
                @Override
                public boolean process(Library library) {
                    if (PROJECT_NAME.equals(library.getName()))
                        res[0] = true;
                    return true;
                }
            });
            if (!res[0]) {
                ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                model.addLibraryEntry(library);
                model.commit();
            }
        }
    }

    public static boolean isEligible(DataContext dataContext) {
        return eligibleProjects.containsKey(getProject(dataContext));
    }

    public static boolean isEligible(Project project) {
        return eligibleProjects.containsKey(project);
    }

    public static Project getProject(DataContext dataContext) {
        return PlatformDataKeys.PROJECT.getData(dataContext);
    }

    public static void updateDefaultTask(Task task) {
        if (task != null) {
            defaultConfiguration = new Task(null, task.testType, task.input, task.output, new Test[0], null,
                task.vmArgs, task.mainClass, null, PEStrictChecker.class.getCanonicalName(), "", new String[0], null,
                task.contestName, task.truncate, null, null, task.includeLocale, task.failOnOverflow, task.template);
        }
    }

    public static Task getDefaultTask() {
        return defaultConfiguration;
    }

    public static ProjectData getData(Project project) {
        return eligibleProjects.get(project);
    }

    public static void openElement(Project project, PsiElement element) {
        if (element instanceof PsiFile) {
            VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
            if (virtualFile == null)
                return;
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        } else if (element instanceof PsiClass) {
            FileEditorManager.getInstance(project).openFile(FileUtils.getFile(project, String.format("%s/%s.java", getData(project).defaultDirectory, ((PsiClass) element).getName())
            ), true);
        }
    }

    public static Point getLocation(Project project, Dimension size) {
        JComponent component = WindowManager.getInstance().getIdeFrame(project).getComponent();
        Point center = component.getLocationOnScreen();
        center.x += component.getWidth() / 2;
        center.y += component.getHeight() / 2;
        center.x -= size.getWidth() / 2;
        center.y -= size.getHeight() / 2;
        return center;
    }


    public static RunnerAndConfigurationSettings createConfiguration(Project project, TaskBase task, boolean setActive) {
        RunManagerImpl manager = RunManagerImpl.getInstanceImpl(project);
        RunnerAndConfigurationSettings old = manager.findConfigurationByName(task.name);
        if (old != null) {
            manager.removeConfiguration(old);
        }
        RunnerAndConfigurationSettings configuration = null;
        if (task instanceof Task) {
            configuration = new RunnerAndConfigurationSettingsImpl(manager,
                new TaskConfiguration(project, task.name, (Task) task,
                    TaskConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
        } else if (task instanceof TopCoderTask) {
            configuration = new RunnerAndConfigurationSettingsImpl(manager,
                new TopCoderConfiguration(project, task.name, (TopCoderTask) task,
                    TopCoderConfigurationType.INSTANCE.getConfigurationFactories()[0]), false);
        }
        manager.addConfiguration(configuration, false);
        if (setActive)
            manager.setSelectedConfiguration(configuration);
        return configuration;
    }

    public static boolean removeConfigurationIfExists(RunConfiguration taskConfiguration) {
        if (taskConfiguration == null) return true;
        RunManagerImpl manager = RunManagerImpl.getInstanceImpl(taskConfiguration.getProject());
        RunnerAndConfigurationSettings configuration = manager.findConfigurationByName(taskConfiguration.getName());
        if (configuration != null) {
            //TODO auto refresh Run/Debug Configuration panel when remove configuration.
            manager.removeConfiguration(configuration);
            setOtherConfiguration(manager, null);
        }
        return true;
    }

    public static void setOtherConfiguration(RunManagerImpl manager, TaskBase task) {
        List<RunConfiguration> allConfigurations = manager.getAllConfigurationsList();
        for (RunConfiguration configuration : allConfigurations) {
            if (configuration instanceof TopCoderConfiguration) {
                TopCoderTask other = ((TopCoderConfiguration) configuration).getConfiguration();
                if (other == null || !(task == null || task.contestName.equals(other.contestName))) {
                    continue;
                }
                manager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(manager, configuration, false));
                return;
            } else if (configuration instanceof TaskConfiguration) {
                Task other = ((TaskConfiguration) configuration).getConfiguration();
                if (other == null || !(task == null || task.contestName.equals(other.contestName))) {
                    continue;
                }
                manager.setSelectedConfiguration(new RunnerAndConfigurationSettingsImpl(manager, configuration, false));
                return;
            }
        }
    }

    public static Parser getDefaultParser() {
        return defaultParser;
    }

    public static void setDefaultParser(Parser defaultParser) {
        ProjectUtils.defaultParser = defaultParser;
    }

    public static void putProjectData(Project project, ProjectData data) {
        eligibleProjects.put(project, data);
    }

    public static Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            BufferedImage image = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }


    public static String getSimpleName(String className) {
        if (className == null) return null;
        int position = className.lastIndexOf('.');
        if (position != -1)
            className = className.substring(position + 1);
        return className;
    }

    public static boolean isSupported(RunConfiguration configuration) {
        return configuration instanceof TaskConfiguration || configuration instanceof TopCoderConfiguration;
    }

    /**
     * if {@param configuration} not valid, then delete it and return false.
     *
     * @param configuration
     * @return
     */
    public static boolean isValidConfigurationOrDeleteIfNot(RunConfiguration configuration) {
        boolean isValid = isValidConfiguration(configuration);
        if (!isValid) {
            ProjectUtils.removeConfigurationIfExists(configuration);
        }
        return isValid;
    }

    /**
     * if {@param configuration} not valid, then return false.
     *
     * @param configuration
     * @return
     */
    public static boolean isValidConfiguration(RunConfiguration configuration) {
        if (configuration == null) return false;
        boolean isValid = true;
        if (configuration instanceof TaskConfiguration) {
            isValid = ((TaskConfiguration) configuration).getConfiguration() != null;
        } else if (configuration instanceof TopCoderConfiguration) {
            isValid = ((TopCoderConfiguration) configuration).getConfiguration() != null;
        }
        return isValid;
    }
}
