package net.egork.chelper.task;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.task.test.NewTopCoderTest;
import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.OutputWriter;

import java.util.InputMismatchException;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTask extends TaskBase<NewTopCoderTest> {
    public final MethodSignature signature;
    public final String fqn;
    public final boolean failOnOverflow;
    public final String memoryLimit;
    public final NewTopCoderTest[] tests; // hide

    public TopCoderTask(String name, MethodSignature signature, NewTopCoderTest[] tests, String date, String contestName, String[] testClasses, String fqn, boolean failOnOverflow, String memoryLimit) {
        super(name, tests, date, contestName, testClasses);
        this.tests = tests;
        this.signature = signature;
        this.fqn = fqn;
        this.failOnOverflow = failOnOverflow;
        this.memoryLimit = memoryLimit;
    }

    @Override
    public void saveTask(OutputWriter out) {
        out.printString(name);
        out.printString(signature.name);
        out.printString(signature.result.getCanonicalName());
        out.printLine(signature.arguments.length);
        for (int i = 0; i < signature.argumentNames.length; i++) {
            out.printString(signature.arguments[i].getCanonicalName());
            out.printString(signature.argumentNames[i]);
        }
        out.printLine(tests.length);
        for (NewTopCoderTest test : tests)
            test.saveTest(out);
        out.printString(date);
        out.printString(contestName);
        out.printLine(testClasses.length);
        for (String testClass : testClasses)
            out.printString(testClass);
        out.printString(fqn);
        out.printBoolean(failOnOverflow);
        out.printString(memoryLimit);
    }

    public static TopCoderTask load(InputReader in) {
        try {
            String name = in.readString();
            String methodName = in.readString();
            Class result = forName(in.readString());
            int argumentCount = in.readInt();
            Class[] arguments = new Class[argumentCount];
            String[] argumentNames = new String[argumentCount];
            for (int i = 0; i < argumentCount; i++) {
                arguments[i] = forName(in.readString());
                argumentNames[i] = in.readString();
            }
            MethodSignature signature = new MethodSignature(methodName, result, arguments, argumentNames);
            int testCount = in.readInt();
            NewTopCoderTest[] tests = new NewTopCoderTest[testCount];
            for (int i = 0; i < testCount; i++)
                tests[i] = new NewTopCoderTest(in);
            String date = in.readString();
            String contestName = in.readString();
            int testClassCount = in.readInt();
            String[] testClasses = new String[testClassCount];
            for (int i = 0; i < testClassCount; i++)
                testClasses[i] = in.readString();
            String fqn = in.readString();
            boolean failOnOverflow = false;
            String memoryLimit = "64M";
            try {
                failOnOverflow = in.readBoolean();
                memoryLimit = in.readString();
            } catch (InputMismatchException ignored) {
            }
            return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
        } catch (InputMismatchException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String defaultValue() {
        Class returnType = signature.result;
        if (returnType == int.class)
            return "0";
        if (returnType == long.class)
            return "0L";
        if (returnType == double.class)
            return "0D";
        if (returnType == String.class)
            return "\"\"";
        if (returnType == int[].class)
            return "new int[0]";
        if (returnType == long[].class)
            return "new long[0]";
        if (returnType == double[].class)
            return "new double[0]";
        if (returnType == String[].class)
            return "new String[0]";
        Messenger.publishMessage("Task " + name + " has unrecognized return type - " +
            signature.result.getSimpleName(), NotificationType.ERROR);
        return "";
    }

    private static Class forName(String s) throws ClassNotFoundException {
        if ("int".equals(s))
            return int.class;
        if ("long".equals(s))
            return long.class;
        if ("double".equals(s))
            return double.class;
        if ("java.lang.String".equals(s))
            return String.class;
        if ("int[]".equals(s))
            return int[].class;
        if ("long[]".equals(s))
            return long[].class;
        if ("double[]".equals(s))
            return double[].class;
        if ("java.lang.String[]".equals(s))
            return String[].class;
        throw new ClassNotFoundException(s);
    }

    public PsiFile getTaskDataPsiFile() {
        return null;
    }

    public PsiFile getTaskSourcePsiFile() {
        return null;
    }

    public TopCoderTask setFQN(String fqn) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    //hiding
    @Override
    public TopCoderTask setTests(NewTopCoderTest[] tests) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    public TopCoderTask setTestClasses(String[] testClasses) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }

    public TopCoderTask setFailOnOverflow(boolean failOnOverflow) {
        return new TopCoderTask(name, signature, tests, date, contestName, testClasses, fqn, failOnOverflow, memoryLimit);
    }


    public PsiMethod getMethod(Project project) {
        String[] arguments = new String[signature.arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = signature.arguments[i].getCanonicalName();
        }
        return MainFileTemplate.getMethod(project, fqn, signature.name, signature.result.getCanonicalName(), arguments);
    }
}
