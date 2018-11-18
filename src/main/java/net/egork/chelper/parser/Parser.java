package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.TestType;

import javax.swing.*;
import java.util.Collection;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public interface Parser {
    Parser[] PARSERS = {new CodeforcesParser(), new TimusParser(),
        new RCCParser()};

    Icon getIcon();

    String getName();

    void getContests(Project project, DescriptionReceiver receiver);

    void parseContest(Project project, String id, DescriptionReceiver receiver);

    /**
     * Returns task with only test cases(bases on default task).
     *
     * @param project
     * @param description
     * @param receiver
     * @return
     */
    Task parseTask(Project project, Description description, DescriptionReceiver receiver);

    TestType defaultTestType();

    Collection<Task> parseTaskFromHTML(String html);
}
