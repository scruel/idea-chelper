package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.StackTraceLogger;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CodeforcesParser implements Parser {
    private static final StackTraceLogger LOG = new StackTraceLogger(ExecuteUtils.class);

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/codeforces.png");
    }

    @Override
    public String getName() {
        return "Codeforces";
    }

    @Override
    public void getContests(Project project, DescriptionReceiver receiver) {
        String contestsPage = ParseProgresser.getWebPageContent(project, receiver, "http://codeforces.com/contests?complete=true");
        if (contestsPage == null)
            return;
        List<Description> contests = new ArrayList<Description>();
        StringParser parser = new StringParser(contestsPage);
        try {
            parser.advance(true, "<div class=\"contestList\">");
            parser.advance(true, "</tr>");
            while (parser.advanceIfPossible(true, "data-contestId=\"") != null) {
                String id = parser.advance(false, "\"");
                parser.advance(true, "<td>");
                String name = parser.advance(false, "</td>", "<br/>").trim();
                contests.add(new Description(id, name));
            }
        } catch (ParseException ignored) {
        }
        if (!receiver.isStopped())
            receiver.receiveDescriptions(contests);
        else
            return;
        //noinspection StatementWithEmptyBody
        while (parser.advanceIfPossible(true, "<span class=\"page-index\" pageIndex=\"") != null) ;
        String lastPage = parser.advanceIfPossible(false, "\"");
        if (lastPage == null)
            return;
        int additionalPagesCount;
        try {
            additionalPagesCount = Integer.parseInt(lastPage);
        } catch (NumberFormatException e) {
            return;
        }
        for (int i = 2; i <= additionalPagesCount; i++) {
            if (receiver.isStopped()) return;
            String page = ParseProgresser.getWebPageContent(project, receiver, "http://codeforces.com/contests/page/" + i);
            if (page == null)
                continue;
            parser = new StringParser(page);
            List<Description> descriptions = new ArrayList<Description>();
            try {
                parser.advance(true, "Contest history");
                parser.advance(true, "</tr>");
                while (parser.advanceIfPossible(true, "data-contestId=\"") != null) {
                    String id = parser.advance(false, "\"");
                    parser.advance(true, "<td>");
                    String name = parser.advance(false, "</td>", "<br/>").trim();
                    descriptions.add(new Description(id, name));
                }
            } catch (ParseException e) {
                continue;
            }
            if (receiver.isStopped()) {
                return;
            }
            receiver.receiveDescriptions(descriptions);
        }
    }

    @Override
    public void parseContest(Project project, String id, DescriptionReceiver receiver) {
        String mainPage = ParseProgresser.getWebPageContent(project, receiver, "http://codeforces.com/contest/" + id);
        if (mainPage == null)
            return;
        List<Description> ids = new ArrayList<Description>();
        StringParser parser = new StringParser(mainPage);
        try {
            parser.advance(true, "<table class=\"problems\">");
            while (parser.advanceIfPossible(true, "<a href=\"/contest/" + id + "/problem/") != null) {
                String taskID = parser.advance(false, "\">");
                parser.advance(true, "<a href=\"/contest/" + id + "/problem/" + taskID + "\">");
                String title = parser.advance(false, "</a>");
                while (title.contains("<!--")) {
                    int start = title.indexOf("<!--");
                    int end = title.indexOf("-->");
                    title = title.substring(0, start) + title.substring(end + 3);
                }
                String name = taskID + " - " + title.trim();
                ids.add(new Description(id + " " + taskID, name));
            }
        } catch (ParseException ignored) {
        }
        if (!receiver.isStopped()) {
            receiver.receiveDescriptions(ids);
        }
    }

    @Override
    public Task parseTask(Project project, Description description, DescriptionReceiver receiver) {
        LOG.debugMethodInfo(true, true, "description", description, "receiver", receiver);
        String id = description.id;
        String[] tokens = id.split(" ");
        if (tokens.length != 2)
            return null;
        String contestId = tokens[0];
        id = tokens[1];
        String text = ParseProgresser.getWebPageContent(project, receiver, "http://codeforces.com/contest/" + contestId + "/problem/" + id);
        if (text == null)
            return null;
        Collection<Task> tasks = parseTaskFromHTML(text);
        if (!tasks.isEmpty())
            return tasks.iterator().next();
        LOG.debugMethodInfo(true, true, "description", description, "receiver", receiver);
        return null;
    }

    @Override
    public TestType defaultTestType() {
        return TestType.SINGLE;
    }

    @Override
    public Collection<Task> parseTaskFromHTML(String html) {
        StringParser parser = new StringParser(html);
        try {
            parser.advance(true, "<table class=\"rtable \">");
            parser.advance(true, "href");
            parser.advance(true, "\">");
            String contestName = parser.advance(false, "</a>");
            parser.advance(true, "<div class=\"title\">");
            String letter = parser.advance(true, ". ");
            String taskName = parser.advance(false, "</div>");
            parser.advance(false, "<div class=\"memory-limit\">", "<DIV class=\"memory-limit\">");
            parser.advance(true, "</div>", "</DIV>");
            String heapMemory = parser.advance(false, "</div>", "</DIV>").split(" ")[0] + "M";
            parser.advance(false, "<div class=\"input-file\">", "<DIV class=\"input-file\">");
            parser.advance(true, "</div>", "</DIV>");
            String inputFileName = parser.advance(false, "</div>", "</DIV>");
            StreamConfiguration inputType;
            if (inputFileName.indexOf(' ') != -1)
                inputType = StreamConfiguration.STANDARD;
            else
                inputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, inputFileName);
            parser.advance(false, "<div class=\"output-file\">", "<DIV class=\"output-file\">");
            parser.advance(true, "</div>", "</DIV>");
            String outputFileName = parser.advance(false, "</div>", "</DIV>");
            StreamConfiguration outputType;
            if (outputFileName.indexOf(' ') != -1)
                outputType = StreamConfiguration.STANDARD;
            else
                outputType = new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, outputFileName);
            List<Test> tests = new ArrayList<Test>();
            while (true) {
                try {
                    parser.advance(false, "<div class=\"input\">", "<DIV class=\"input\">");
                    parser.advance(true, "<pre>", "<PRE>");
                    String testInput = parser.advance(false, "</pre>", "</PRE>").replace("<br />", "\n").replace("<br>", "\n").replace("<BR/>", "\n");
                    parser.advance(false, "<div class=\"output\">", "<DIV class=\"output\">");
                    parser.advance(true, "<pre>", "<PRE>");
                    String testOutput = parser.advance(false, "</pre>", "</PRE>").replace("<br />", "\n").replace("<br>", "\n").replace("<BR/>", "\n");
                    tests.add(new Test(StringEscapeUtils.unescapeHtml(testInput),
                        StringEscapeUtils.unescapeHtml(testOutput), tests.size()));
                } catch (ParseException e) {
                    break;
                }
            }
            String taskClass = "Task" + letter;
            String name = letter + " - " + taskName;
            return Collections.singleton(new Task(name, defaultTestType(), inputType, outputType, tests.toArray(new Test[tests.size()]), null,
                "-Xmx" + heapMemory, "Main", taskClass, PEStrictChecker.class.getCanonicalName(), "", new String[0], null,
                contestName, true, null, null, false, false));
        } catch (ParseException e) {
            return Collections.emptyList();
        }
    }
}
