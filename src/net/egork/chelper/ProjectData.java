package net.egork.chelper;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.ProjectUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class ProjectData {
    public static final ProjectData DEFAULT = new ProjectData(
        "java.util.Scanner", "java.io.PrintWriter", "java.,javax.,com.sun.".split(","), "output", "",
        "archive/unsorted", "main", "lib/test", false, false, true, false);

    public final String inputClass;
    public final String outputClass;
    public final String[] excludedPackages;
    public final String outputDirectory;
    public final String author;
    public final String archiveDirectory;
    public final String defaultDirectory;
    public final String testDirectory;
    public final boolean enableUnitTests;
    public final boolean failOnIntegerOverflowForNewTasks;
    public final boolean smartTesting;
    public final boolean extensionProposed;

    public ProjectData(String inputClass, String outputClass, String[] excludedPackages, String outputDirectory, String author, String archiveDirectory, String defaultDirectory, String testDirectory, boolean enableUnitTests, boolean failOnIntegerOverflowForNewTasks, boolean smartTesting, boolean extensionProposed) {
        this.extensionProposed = extensionProposed;
        this.inputClass = inputClass.trim();
        this.outputClass = outputClass.trim();
        this.excludedPackages = excludedPackages;
        this.outputDirectory = outputDirectory.trim();
        this.author = author.trim();
        this.archiveDirectory = archiveDirectory.trim();
        this.defaultDirectory = defaultDirectory.trim();
        this.testDirectory = testDirectory.trim();
        this.enableUnitTests = enableUnitTests;
        this.failOnIntegerOverflowForNewTasks = failOnIntegerOverflowForNewTasks;
        this.smartTesting = smartTesting;
    }

    public ProjectData(Properties properties) {
        inputClass = properties.getProperty("inputClass", DEFAULT.inputClass);
        outputClass = properties.getProperty("outputClass", DEFAULT.outputClass);
        excludedPackages = properties.getProperty("excludePackages", join(DEFAULT.excludedPackages)).split(",");
        outputDirectory = properties.getProperty("outputDirectory", DEFAULT.outputDirectory);
        author = properties.getProperty("author", DEFAULT.author);
        archiveDirectory = properties.getProperty("archiveDirectory", DEFAULT.archiveDirectory);
        defaultDirectory = properties.getProperty("defaultDirectory", DEFAULT.defaultDirectory);
        testDirectory = properties.getProperty("testDirectory", DEFAULT.testDirectory);
        enableUnitTests = Boolean.valueOf(properties.getProperty("enableUnitTests", Boolean.toString(DEFAULT.enableUnitTests)));
        failOnIntegerOverflowForNewTasks = Boolean.valueOf(properties.getProperty("failOnIntegerOverflowForNewTasks", Boolean.toString(DEFAULT.enableUnitTests)));
        smartTesting = Boolean.valueOf(properties.getProperty("smartTesting", Boolean.toString(DEFAULT.smartTesting)));
        extensionProposed = Boolean.valueOf(properties.getProperty("extensionProposed", Boolean.toString(DEFAULT.extensionProposed)));
    }

    public static ProjectData load(Project project) {
        if (project == null)
            return null;
        VirtualFile root = project.getBaseDir();
        if (root == null)
            return null;
        VirtualFile config = root.findChild("chelper.properties");
        if (config == null)
            return null;
        Properties properties = FileUtils.loadProperties(config);
        if (properties == null)
            return null;
        return new ProjectData(properties);
    }

    public void save(final Project project) {
        ExecuteUtils.executeWriteCommandAction(project, new Runnable() {
            public void run() {
                if (project == null)
                    return;
                final VirtualFile root = project.getBaseDir();
                if (root == null)
                    return;
                final Properties properties = new Properties();
                properties.setProperty("inputClass", inputClass);
                properties.setProperty("outputClass", outputClass);
                properties.setProperty("excludePackages", join(excludedPackages));
                properties.setProperty("outputDirectory", outputDirectory);
                properties.setProperty("author", author);
                properties.setProperty("archiveDirectory", archiveDirectory);
                properties.setProperty("defaultDirectory", defaultDirectory);
                properties.setProperty("testDirectory", testDirectory);
                properties.setProperty("enableUnitTests", Boolean.toString(enableUnitTests));
                properties.setProperty("failOnIntegerOverflowForNewTasks", Boolean.toString(failOnIntegerOverflowForNewTasks));
                properties.setProperty("smartTesting", Boolean.toString(smartTesting));
                properties.setProperty("extensionProposed", Boolean.toString(extensionProposed));

                final String name = "chelper.properties";
                VirtualFile config = root.findChild(name);
                if (config == null) {
                    PsiDirectory directory = PsiManager.getInstance(project).findDirectory(project.getBaseDir());
                    FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(name);
                    PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText(name, type, "");
                    directory.add(psiFile);
                    config = root.findChild(name);
                }
                OutputStream out;
                try {
                    out = config.getOutputStream(this);
                    properties.store(out, "");
                    out.close();
                } catch (IOException ignore) {
                }
            }
        });
    }

    public static String join(String[] excludedPackages) {
        StringBuilder result = new StringBuilder();
        for (String aPackage : excludedPackages) {
            if (result.length() > 0)
                result.append(',');
            result.append(aPackage);
        }
        return result.toString();
    }

    public void completeExtensionProposal(Project project) {
        ProjectData newData = new ProjectData(inputClass, outputClass, excludedPackages, outputDirectory, author,
            archiveDirectory, defaultDirectory, testDirectory, enableUnitTests, failOnIntegerOverflowForNewTasks, smartTesting, true);
        newData.save(project);
        ProjectUtils.putProjectData(project, newData);
    }
}
