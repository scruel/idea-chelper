package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author egorku@yandex-team.ru
 */
public class CSAcademyParser implements Parser {
    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "CS Academy";
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
    public Task parseTask(Project project, Description description, DescriptionReceiver receiver) {
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
            parser.advance(true, "<div class=\"text-center\"><h1>");
            String taskName = parser.advance(false, "</h1>");
            parser.advance(true, "<br>Memory limit: <em>");
            String memoryLimit = parser.advance(false, " ");
            memoryLimit += "M";
            StreamConfiguration input = StreamConfiguration.STANDARD;
            StreamConfiguration output = StreamConfiguration.STANDARD;
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<td><pre>") != null) {
                String testInput = parser.advance(false, "</pre></td>");
                parser.advance(true, "<td><pre>");
                String testOutput = parser.advance(false, "</pre></td>");
                tests.add(new Test(testInput, testOutput, tests.size()));
            }
            parser.advance(true, "\"contest\":");
            String contestName = null;
            parser.advance(true, "\"longName\":\"");
            contestName = parser.advance(false, "\"");
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, tests.toArray(new Test[tests.size()]), null,
                "-Xmx" + memoryLimit, "Main", CodeChefParser.getTaskID(taskName), PEStrictChecker.class.getCanonicalName(), "",
                new String[0], null, contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }

}
