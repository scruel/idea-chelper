package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;

/**
 * @author egorku@yandex-team.ru
 */
public class FacebookParser implements Parser {
    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return "Facebook Hacker Cup";
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
        return TestType.MULTI_NUMBER;
    }

    @Override
    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            parser.advance(true, "<h2 class=\"uiHeaderTitle\"");
            parser.advance(true, ">");
            String contestName = parser.advance(false, "</h2>");
            parser.advance(true, "<div class=\"clearfix\">");
            parser.advance(true, "<span class");
            parser.advance(true, ">");
            String taskName = parser.advance(false, "</span>");
            String taskClass = CodeChefParser.getTaskID(taskName);
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < taskName.length(); i++) {
                if (Character.isLetter(taskName.charAt(i)))
                    regex.append(Character.toLowerCase(taskName.charAt(i)));
                else
                    regex.append(".*");
            }
            regex.append(".*[.]txt");
            StreamConfiguration input = new StreamConfiguration(StreamConfiguration.StreamType.LOCAL_REGEXP,
                regex.toString());
            StreamConfiguration output = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM,
                taskName.toLowerCase().replaceAll(" ", "") + ".out");
            parser.advance(true, "<span class=\"fsm\">Example input</span>");
            parser.advance(true, "<pre");
            parser.advance(true, ">");
            String testInput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
            parser.advance(true, "<pre");
            parser.advance(true, ">");
            String testOutput = StringEscapeUtils.unescapeHtml(parser.advance(false, "</pre>"));
            return Collections.singleton(new Task(taskName, defaultTestType(), input, output, new Test[]{new Test(testInput, testOutput)},
                null, "-Xmx1024M", "Main", taskClass, PEStrictChecker.class.getCanonicalName(), "",
                new String[0], null, contestName, true, null, null, true, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }
}
