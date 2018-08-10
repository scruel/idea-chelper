package net.egork.chelper.codegeneration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import net.egork.chelper.ProjectData;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TaskBase;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.task.test.TestBase;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.OutputWriter;
import net.egork.chelper.util.ProjectUtils;
import net.egork.chelper.util.TaskUtils;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeGenerationUtils {
    private static final Logger LOG = Logger.getInstance(CodeGenerationUtils.class);

    private CodeGenerationUtils() {
    }

    public static String createCheckerStub(Project project, Task task, String location, String name) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtils.getPackage(directory);
        String template = createCheckerClassTemplateIfNeeded(project);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
            inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "CheckerClass", name);
    }

    public static String createStub(Project project, Task task, String location, String name) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtils.getPackage(directory);
        String template = createTaskClassTemplateIfNeeded(project, task.template);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
            inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "TaskClass", name);
    }

    public static String createTaskClassTemplateIfNeeded(Project project, String templateName, String inputTemplate) {
        String taskClassFileName = "TaskClass.template";
        VirtualFile file = FileUtils.getFile(project, templateName == null ? taskClassFileName : templateName);
        String result = null;
        if (file != null) {
            result = FileUtils.readTextFile(file);
        }
        if (result == null && templateName != null) {
            file = FileUtils.getFile(project, taskClassFileName);
            if (file != null) {
                result = FileUtils.readTextFile(file);
            }
        }
        if (inputTemplate == null && result != null) {
            return result;
        }
        String author = ProjectUtils.getData(project).author;
        if (author.isEmpty()) {
            author = "unknown";
        }
        String template = inputTemplate != null ? inputTemplate : "package %package%;\n" +
            "\n" +
            "import %InputClassFQN%;\n" +
            "import %OutputClassFQN%;\n" +
            "\n" +
            "/**\n" +
            " * @author " + author + "\n" +
            " */\n" +
            "public class %TaskClass% {\n" +
            "    public void solve(int testNumber, %InputClass% in, %OutputClass% out) {\n" +
            "    }\n" +
            "}\n";
        FileUtils.writeTextFile(project, PsiManager.getInstance(project).findDirectory(project.getBaseDir()), taskClassFileName, template);
        return template;
    }

    public static String createTaskClassTemplateIfNeeded(Project project, String templateName) {
        return createTaskClassTemplateIfNeeded(project, templateName, null);
    }

    public static String createCheckerClassTemplateIfNeeded(Project project, String inputTemplate) {
        String checkerClassFileName = "CheckerClass.template";
        VirtualFile file = FileUtils.getFile(project, checkerClassFileName);
        if (inputTemplate == null && file != null)
            return FileUtils.readTextFile(file);
        String author = ProjectUtils.getData(project).author;
        if (author.isEmpty()) {
            author = "unknown";
        }
        String template = inputTemplate != null ? inputTemplate : "package %package%;\n" +
            "\n" +
            "import net.egork.chelper.tester.Verdict;\n" +
            "import net.egork.chelper.checkers.Checker;\n" +
            "\n" +
            "/**\n" +
            " * @author " + author + "\n" +
            " */\n" +
            "public class %CheckerClass% implements Checker {\n" +
            "    public %CheckerClass%(String parameters) {\n" +
            "    }\n" +
            "\n" +
            "    public Verdict check(String input, String expectedOutput, String actualOutput) {\n" +
            "        return Verdict.UNDECIDED;\n" +
            "    }\n" +
            "}\n";
        FileUtils.writeTextFile(project, PsiManager.getInstance(project).findDirectory(project.getBaseDir()), checkerClassFileName, template);

        return template;
    }

    public static String createCheckerClassTemplateIfNeeded(Project project) {
        return createCheckerClassTemplateIfNeeded(project, null);
    }

    public static String createTestCaseClassTemplateIfNeeded(Project project, String inputTemplate) {

        String testCaseClassFileName = "TestCaseClass.template";
        VirtualFile file = FileUtils.getFile(project, testCaseClassFileName);
        if (inputTemplate == null && file != null)
            return FileUtils.readTextFile(file);
        String author = ProjectUtils.getData(project).author;
        if (author.isEmpty()) {
            author = "unknown";
        }
        String template = inputTemplate != null ? inputTemplate : "package %package%;\n" +
            "\n" +
            "import net.egork.chelper.task.test.Test;\n" +
            "import net.egork.chelper.tester.TestCase;\n" +
            "\n" +
            "import java.util.Collection;\n" +
            "import java.util.Collections;\n" +
            "\n" +
            "/**\n" +
            " * @author " + author + "\n" +
            " */\n" +
            "public class %TestCaseClass% {\n" +
            "    @TestCase\n" +
            "    public Collection<Test> createTests() {\n" +
            "        return Collections.emptyList();\n" +
            "    }\n" +
            "}\n";
        FileUtils.writeTextFile(project, PsiManager.getInstance(project).findDirectory(project.getBaseDir()), testCaseClassFileName, template);
        return template;
    }

    public static String createTestCaseClassTemplateIfNeeded(Project project) {
        return createTestCaseClassTemplateIfNeeded(project, null);
    }

    public static String createTopCoderTestCaseClassTemplateIfNeeded(Project project, String inputTemplate) {
        String topCoderTestCaseClassFileName = "TopCoderTestCaseClass.template";
        VirtualFile file = FileUtils.getFile(project, topCoderTestCaseClassFileName);
        if (inputTemplate == null && file != null)
            return FileUtils.readTextFile(file);
        String author = ProjectUtils.getData(project).author;
        if (author.isEmpty()) {
            author = "unknown";
        }
        String template = inputTemplate != null ? inputTemplate : "package %package%;\n" +
            "\n" +
            "import net.egork.chelper.task.test.NewTopCoderTest;\n" +
            "import net.egork.chelper.tester.TestCase;\n" +
            "\n" +
            "import java.util.Collection;\n" +
            "import java.util.Collections;\n" +
            "\n" +
            "/**\n" +
            " * @author " + author + "\n" +
            " */\n" +
            "public class %TestCaseClass% {\n" +
            "    @TestCase\n" +
            "    public Collection<NewTopCoderTest> createTests() {\n" +
            "        return Collections.emptyList();\n" +
            "    }\n" +
            "}\n";
        FileUtils.writeTextFile(project, PsiManager.getInstance(project).findDirectory(project.getBaseDir()), topCoderTestCaseClassFileName, template);
        return template;
    }

    public static String createTopCoderTestCaseClassTemplateIfNeeded(Project project) {
        return createTopCoderTestCaseClassTemplateIfNeeded(project, null);
    }

    public static String createTopCoderTaskTemplateIfNeeded(Project project, String inputTemplate) {

        String topCoderTaskClass = "TopCoderTaskClass.template";
        VirtualFile file = FileUtils.getFile(project, topCoderTaskClass);
        if (inputTemplate == null && file != null)
            return FileUtils.readTextFile(file);
        String author = ProjectUtils.getData(project).author;
        if (author.isEmpty()) {
            author = "unknown";
        }
        String template = inputTemplate != null ? inputTemplate : "package %package%;\n" +
            "\n" +
            "/**\n" +
            " * @author " + author + "\n" +
            " */\n" +
            "public class %TaskClass% {\n" +
            "    public %Signature% {\n" +
            "        return %DefaultValue%;\n" +
            "    }\n" +
            "}\n";
        FileUtils.writeTextFile(project, PsiManager.getInstance(project).findDirectory(project.getBaseDir()), topCoderTaskClass, template);
        return template;
    }

    public static String createTopCoderTaskTemplateIfNeeded(Project project) {
        return createTopCoderTaskTemplateIfNeeded(project, null);
    }

    public static void createTemplatesIfNeeded(final Project project) {
        DumbService.getInstance(project).runWhenSmart(new Runnable() {
            @Override
            public void run() {
                createTaskClassTemplateIfNeeded(project, null);
                createCheckerClassTemplateIfNeeded(project);
                createTestCaseClassTemplateIfNeeded(project);
                createTopCoderTaskTemplateIfNeeded(project);
                createTopCoderTestCaseClassTemplateIfNeeded(project);
            }
        });
    }

    enum TemplateType {
        TASK_CLASS(0),
        CHECKER_CLASS(1),
        TOP_CODER_TASK_CLASS(2),
        TEST_CASE_CLASS(3),
        TOP_CODER_TEST_CASE_CLASS(4);

        TemplateType(int i) {
        }
    }

    /**
     * get all template in a Map, use this method to change all template with same pattern.
     *
     * @param project
     * @return
     */
    public static Map<TemplateType, String> getAllTemplate(Project project) {
        Map<TemplateType, String> allTemplates = new HashMap<TemplateType, String>();
        allTemplates.put(TemplateType.TASK_CLASS, createTaskClassTemplateIfNeeded(project, null));
        allTemplates.put(TemplateType.CHECKER_CLASS, createCheckerClassTemplateIfNeeded(project));
        allTemplates.put(TemplateType.TEST_CASE_CLASS, createTestCaseClassTemplateIfNeeded(project));
        allTemplates.put(TemplateType.TOP_CODER_TASK_CLASS, createTopCoderTaskTemplateIfNeeded(project));
        allTemplates.put(TemplateType.TOP_CODER_TEST_CASE_CLASS, createTopCoderTestCaseClassTemplateIfNeeded(project));
        return allTemplates;
    }

    /**
     * refresh all template files by new ProjectData.
     *
     * @param project
     * @param oldData
     * @param newData
     */
    public static void refreshAllTemplate(Project project, ProjectData oldData, ProjectData newData) {
        Map<TemplateType, String> allTemplates = getAllTemplate(project);
        for (Map.Entry<TemplateType, String> entry : allTemplates.entrySet()) {
            if (newData.author != null) {
                allTemplates.put(entry.getKey(), entry.getValue().replaceAll("@author (.*)+", "@author " + newData.author));
            }
        }
        writeAllTemplate(project, allTemplates);
    }

    private static void writeAllTemplate(Project project, Map<TemplateType, String> allTemplates) {
        for (Map.Entry<TemplateType, String> entry : allTemplates.entrySet()) {
            if (entry.getKey().equals(TemplateType.TASK_CLASS)) {
                createTaskClassTemplateIfNeeded(project, null, entry.getValue());
            }
            if (entry.getKey().equals(TemplateType.CHECKER_CLASS)) {
                createCheckerClassTemplateIfNeeded(project, entry.getValue());
            }
            if (entry.getKey().equals(TemplateType.TEST_CASE_CLASS)) {
                createTestCaseClassTemplateIfNeeded(project, entry.getValue());
            }
            if (entry.getKey().equals(TemplateType.TOP_CODER_TASK_CLASS)) {
                createTopCoderTaskTemplateIfNeeded(project, entry.getValue());
            }
            if (entry.getKey().equals(TemplateType.TOP_CODER_TEST_CASE_CLASS)) {
                createTopCoderTestCaseClassTemplateIfNeeded(project, entry.getValue());
            }
        }
    }

    public static void createUnitTest(final Project project, TaskBase task) {
        if (!ProjectUtils.getData(project).enableUnitTests)
            return;
        TestBase[] tests = task.tests;
        for (int i = 0, testsLength = tests.length; i < testsLength; i++)
            tests[i] = tests[i].setActive(true);
        String path = ProjectUtils.getData(project).testDirectory + "/on" + canonize(firstPart(task.date), false) + "/on" + canonize(task.date, false) + "_" + canonize(task.contestName, false) + "/" +
            canonize(task.name, true);
        task = task.setTests(tests);
        String originalPath = path;
        int index = 0;
        while (FileUtils.getFile(project, path) != null)
            path = originalPath + (index++);
        final VirtualFile directory = FileUtils.createDirectoryIfMissing(project, path);
        final String packageName = FileUtils.getPackage(FileUtils.getPsiDirectory(project, path));
        if (packageName == null) {
            JOptionPane.showMessageDialog(null, "testDirectory should be under project source");
            return;
        }
        if (task instanceof Task) {
            PsiElement main = JavaPsiFacade.getInstance(project).findClass(((Task) task).taskClass, GlobalSearchScope.allScope(project));
            VirtualFile sourceFile = main == null ? null : main.getContainingFile() == null ? null : main.getContainingFile().getVirtualFile();
            String mainContent = FileUtils.readTextFile(sourceFile);
            mainContent = changePackage(mainContent, packageName);
            String taskClassSimple = ProjectUtils.getSimpleName(((Task) task).taskClass);
            FileUtils.writeTextFile(directory, taskClassSimple + ".java", mainContent);
            task = ((Task) task).setTaskClass(packageName + "." + taskClassSimple);
            PsiElement checker = JavaPsiFacade.getInstance(project).findClass(((Task) task).checkerClass, GlobalSearchScope.allScope(project));
            VirtualFile checkerFile = checker == null ? null : checker.getContainingFile() == null ? null : checker.getContainingFile().getVirtualFile();
            if (checkerFile != null && sourceFile != null && checkerFile.getParent().equals(sourceFile.getParent())) {
                String checkerContent = FileUtils.readTextFile(checkerFile);
                checkerContent = changePackage(checkerContent, packageName);
                String checkerClassSimple = ProjectUtils.getSimpleName(((Task) task).checkerClass);
                FileUtils.writeTextFile(directory, checkerClassSimple + ".java", checkerContent);
                task = ((Task) task).setCheckerClass(packageName + "." + checkerClassSimple);
            }

        } else if (task instanceof TopCoderTask) {
            VirtualFile sourceFile = FileUtils.getFile(project, ProjectUtils.getData(project).defaultDirectory + "/" + task.name + ".java");
            String mainContent = FileUtils.readTextFile(sourceFile);
            mainContent = changePackage(mainContent, packageName);
            String taskClassSimple = task.name;
            FileUtils.writeTextFile(directory, taskClassSimple + ".java", mainContent);
            task = ((TopCoderTask) task).setFQN(packageName + "." + taskClassSimple);
        }

        String[] testClasses = Arrays.copyOf(task.testClasses, task.testClasses.length);
        for (int i = 0; i < testClasses.length; i++) {
            PsiElement test = JavaPsiFacade.getInstance(project).findClass(task.testClasses[i], GlobalSearchScope.allScope(project));
            VirtualFile testFile = test == null ? null : test.getContainingFile() == null ? null : test.getContainingFile().getVirtualFile();
            String testContent = FileUtils.readTextFile(testFile);
            testContent = changePackage(testContent, packageName);
            String testClassSimple = ProjectUtils.getSimpleName(testClasses[i]);
            FileUtils.writeTextFile(directory, testClassSimple + ".java", testContent);
            testClasses[i] = packageName + "." + testClassSimple;
        }
        task = task.setTestClasses(testClasses);
        final TaskBase finalTask = task;

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            public void run() {
                String taskFilePath;
                try {
                    VirtualFile taskFile;
                    if (finalTask instanceof Task) {
                        taskFile = directory.findOrCreateChildData(null, TaskUtils.canonize(finalTask.name) + ".task");
                    } else {
                        taskFile = directory.findOrCreateChildData(null, finalTask.name + ".tctask");
                    }
                    OutputStream outputStream = taskFile.getOutputStream(null);
                    finalTask.saveTask(new OutputWriter(outputStream));
                    outputStream.close();
                    taskFilePath = FileUtils.getRelativePath(project.getBaseDir(), taskFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String tester;
                if (finalTask instanceof Task) {
                    tester = generateTester(taskFilePath);
                } else {
                    tester = generateTopCoderTester(taskFilePath);
                }
                tester = changePackage(tester, packageName);
                FileUtils.writeTextFile(directory, "Main.java", tester);
            }
        });
    }

    private static String canonize(String token, boolean firstIsLetter) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            if (firstIsLetter && i == 0 && Character.isDigit(token.charAt(0)))
                result.append('_');
            if (Character.isLetterOrDigit(token.charAt(i)) && token.charAt(i) < 128)
                result.append(token.charAt(i));
            else
                result.append('_');
        }
        return result.toString();
    }

    private static String firstPart(String date) {
        int position = date.indexOf('.');
        if (position != -1)
            position = date.indexOf('.', position + 1);
        if (position != -1)
            return date.substring(0, position);
        return date;
    }

    private static String generateTopCoderTester(String taskPath) {
        StringBuilder builder = new StringBuilder();
        builder.append("import net.egork.chelper.tester.NewTopCoderTester;\n\n");
        builder.append("import org.junit.Assert;\n");
        builder.append("import org.junit.Test;\n\n");
        builder.append("public class Main {\n");
        builder.append("  @Test\n");
        builder.append("  public void test() throws Exception {\n");
        builder.append("    if (!NewTopCoderTester.test(\"").append(taskPath).append("\"))\n");
        builder.append("      Assert.fail();\n");
        builder.append("  }\n");
        builder.append("}\n");
        return builder.toString();
    }

    private static String generateTester(String taskPath) {
        StringBuilder builder = new StringBuilder();
        builder.append("import net.egork.chelper.tester.NewTester;\n\n");
        builder.append("import org.junit.Assert;\n");
        builder.append("import org.junit.Test;\n\n");
        builder.append("public class Main {\n");
        builder.append("  @Test\n");
        builder.append("  public void test() throws Exception {\n");
        builder.append("    if (!NewTester.test(\"").append(taskPath).append("\"))\n");
        builder.append("      Assert.fail();\n");
        builder.append("  }\n");
        builder.append("}\n");
        return builder.toString();
    }

    public static String changePackage(String source, String packageName) {
        if (source.startsWith("package ")) {
            int index = source.indexOf(';');
            if (index == -1)
                return source;
            source = source.substring(index + 1);
        }
        while (source.startsWith("\n")) {
            source = source.substring(1);
        }
        if (packageName.length() == 0)
            return source;
        source = "package " + packageName + ";\n\n" + source;
        return source;
    }

    public static String createTestStub(Project project, Task task, String location, String name) {
        PsiDirectory directory = FileUtils.getPsiDirectory(project, location);
        String inputClass = task.inputClass;
        String inputClassShort = inputClass.substring(inputClass.lastIndexOf('.') + 1);
        String outputClass = task.outputClass;
        String outputClassShort = outputClass.substring(outputClass.lastIndexOf('.') + 1);
        String packageName = FileUtils.getPackage(directory);
        String template = createTestCaseClassTemplateIfNeeded(project);
        return new Template(template).apply("package", packageName, "InputClass", inputClassShort, "InputClassFQN",
            inputClass, "OutputClass", outputClassShort, "OutputClassFQN", outputClass, "TestCaseClass", name);
    }

    public static String createTopCoderStub(Project project, TopCoderTask task, String packageName) {
        String template = createTopCoderTaskTemplateIfNeeded(project);
        StringBuilder signature = new StringBuilder();
        signature.append(task.signature.result.getSimpleName()).append(" ").append(task.signature.name).append("(");
        for (int i = 0; i < task.signature.arguments.length; i++) {
            if (i != 0)
                signature.append(", ");
            signature.append(task.signature.arguments[i].getSimpleName()).append(' ').append(task.signature.argumentNames[i]);
        }
        signature.append(')');
        return new Template(template).apply("package", packageName, "TaskClass", task.name, "Signature",
            signature.toString(), "DefaultValue", task.defaultValue());
    }

    public static String createTopCoderTestStub(Project project, String aPackage, String name) {
        String template = createTopCoderTestCaseClassTemplateIfNeeded(project);
        return new Template(template).apply("package", aPackage, "TestCaseClass", name);
    }
}
