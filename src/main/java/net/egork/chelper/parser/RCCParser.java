package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import net.egork.chelper.checkers.PEStrictChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.test.Test;
import net.egork.chelper.task.test.TestType;
import org.apache.commons.lang.StringEscapeUtils;

import javax.swing.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class RCCParser implements Parser {
    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/rcc.png");
    }

    @Override
    public String getName() {
        return "Russian CodeCup";
    }

    @Override
    public void getContests(Project project, DescriptionReceiver receiver) {
        int currentRound;
        String currentPage = ParseProgresser.getWebPageContent(project, receiver, "http://russiancodecup.ru/en/championship/", "UTF-8");
        StringParser parser = new StringParser(currentPage);
        List<Integer> championshipIDs = new ArrayList<Integer>();
        try {
            parser.advance(true, "<div class=\"subMenu fontLarge\">");
            while (parser.advanceIfPossible(true, "<li><a href=\"/championship/round/") != null)
                championshipIDs.add(Integer.parseInt(parser.advance(false, "/")));
            parser.advance(true, "var cur_round_id =");
            String id = parser.advance(false, ";");
            currentRound = Integer.parseInt(id.trim());
            processChampionshipPage(receiver, currentRound, currentPage);
        } catch (ParseException ignored) {
        } catch (NumberFormatException ignored) {
        }
        for (int id : championshipIDs)
            processChampionshipPage(receiver, id, ParseProgresser.getWebPageContent(project, receiver, "http://russiancodecup.ru/en/championship/round/" + id + "/problem/A/", "UTF-8"));
        for (int i = 70; i > 0; i--) {
            processArchivePage(project, receiver, i);
        }
    }

    private void processChampionshipPage(DescriptionReceiver receiver, int id, String page) {
        StringParser parser = new StringParser(page);
        try {
            parser.advance(true, "<title>");
            String title = parser.advance(false, "</title>").trim();
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(Collections.singleton(new Description(Integer.toString(id), title)));
            }
        } catch (ParseException ignored) {
        }
    }

    private void processArchivePage(Project project, DescriptionReceiver receiver, int id) {
        String page;
        page = ParseProgresser.getWebPageContent(project, receiver, "http://russiancodecup.ru/ru/tasks/round/" + id, "UTF-8");
        if (page == null || page.contains("<title>RCC | 404</title>")) {
            return;
        }
        StringParser parser = new StringParser(page);
        try {
            parser.advance(true, "<span>/");
            String title = parser.advance(false, "</span>").trim();
            if (!receiver.isStopped()) {
                receiver.receiveDescriptions(Collections.singleton(new Description(Integer.toString(id), title)));
            }
        } catch (ParseException ignored) {
        }
    }

    @Override
    public void parseContest(Project project, String id, DescriptionReceiver receiver) {
        String url = "http://russiancodecup.ru/championship/round/" + id + "/problem/A/";
        String page = ParseProgresser.getWebPageContent(project, receiver, url, "UTF-8");
        if (page == null || page.contains("<title>RCC | 404</title>")) {
            url = "http://www.russiancodecup.ru/tasks/round/" + id + "/A/";
            page = ParseProgresser.getWebPageContent(project, receiver, url, "UTF-8");
        }
        List<Description> descriptions = new ArrayList<Description>();
        char taskID = 'A';
        while (true) {
            if (page == null || page.contains("<title>RCC | 404</title>")) {
                receiver.receiveDescriptions(descriptions);
                return;
            }
            StringParser parser = new StringParser(page);
            try {
                parser.advance(true, "<div class=\"blueBlock hTask\">");
                parser.advance(true, "<div class=\"container\">");
                String name = parser.advance(false, "</div>", "<span>").trim();
                if (name.length() < 4) {
                    receiver.receiveDescriptions(descriptions);
                    return;
                }
                char letter = name.charAt(1);
                if (letter != taskID) {
                    receiver.receiveDescriptions(descriptions);
                    return;
                }
                descriptions.add(new Description(url, letter + " - " + name.substring(4)));
                taskID = ((char) (url.charAt(url.length() - 2) + 1));
                url = url.substring(0, url.length() - 2) + ((char) (url.charAt(url.length() - 2) + 1)) + "/";
                page = ParseProgresser.getWebPageContent(project, receiver, url, "UTF-8");
            } catch (ParseException e) {
                receiver.receiveDescriptions(descriptions);
                return;
            }
        }
    }

    @Override
    public Task parseTask(Project project, Description description, DescriptionReceiver receiver) {
        String page = ParseProgresser.getWebPageContent(project, receiver, description.id, "UTF-8");
        if (page == null)
            return null;
        StringParser parser = new StringParser(page);
        try {
            parser.advance(true, "<div class=\"blueBlock hTask\">");
            parser.advance(true, "<div class=\"container\">");
            String name = parser.advance(false, "</div>", "<span>").trim();
            char letter = name.charAt(1);
            parser.advance(true, "<span class=\"iconSmall isMemory\">");
            parser.advance(true, "<td>");
            String memoryLimit = parser.advance(false, " ");
            List<Test> tests = new ArrayList<Test>();
            while (parser.advanceIfPossible(true, "<div class=\"fiftyBox\">\n") != null) {
                parser.advance(true, "<pre class=\"colorBlue\">");
                String input = StringEscapeUtils.unescapeHtml(parser.advanceIfPossible(false, "</pre>")).trim() + "\n";
                parser.advance(true, "<pre class=\"colorBlue\">");
                String output = StringEscapeUtils.unescapeHtml(parser.advanceIfPossible(false, "</pre>")).trim() + "\n";
                tests.add(new Test(input, output, tests.size()));
            }
            return new Task(description.description, null, StreamConfiguration.STANDARD, StreamConfiguration.STANDARD,
                tests.toArray(new Test[tests.size()]), null, "-Xmx" + memoryLimit + "M -Xss64M", "Main", "Task" + letter,
                PEStrictChecker.class.getCanonicalName(), "", new String[0], null, null, true, null, null, false, false);
        } catch (ParseException e) {
            return null;
        }

    }

    @Override
    public TestType defaultTestType() {
        return TestType.MULTI_NUMBER;
    }

    @Override
    public Collection<Task> parseTaskFromHTML(String html) {
        throw new UnsupportedOperationException();
    }
}
