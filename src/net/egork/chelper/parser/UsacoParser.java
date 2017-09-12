package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;

import javax.swing.*;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

/**
 * @author egorku@yandex-team.ru
 */
public class UsacoParser implements Parser {
    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "USACO";
    }

    @Override
    public void getContests(Project project, DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void parseContest(Project project, String id, DescriptionReceiver receiver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task parseTask(Project project, DescriptionReceiver receiver, Description description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    @Override
    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            parser.advance(true, "<h2>");
            String contestName = parser.advance(false, "</h2>").trim();
            parser.advance(true, "<h2>");
            String taskName = parser.advance(false, "</h2>").trim();
            parser.advance(true, "INPUT FORMAT (file ");
            String taskId = parser.advance(false, ".in").trim();
            String taskClass = Character.toUpperCase(taskId.charAt(0)) + taskId.substring(1);
            StreamConfiguration input = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, taskId + ".in");
            StreamConfiguration output = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, taskId + ".out");
            parser.advance(true, "SAMPLE INPUT");
            parser.advance(true, "<pre class=\"in\">");
            String testInput = parser.advance(false, "</pre>").trim() + "\n";
            parser.advance(true, "SAMPLE OUTPUT");
            parser.advance(true, "<pre class=\"out\">");
            String testOutput = parser.advance(false, "</pre>").trim() + "\n";
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, new Test[]{new Test(testInput, testOutput)},
                null, "-Xmx1024M", taskClass, taskClass, PEStrictChecker.class.getCanonicalName(), "",
                new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }
}
