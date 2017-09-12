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

    Task parseTask(Project project, DescriptionReceiver receiver, Description description);

    TestType defaultTestType();

    Collection<Task> parseTaskFromHTML(String html);
}
