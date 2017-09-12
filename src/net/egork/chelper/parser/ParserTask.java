package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;

/**
 * @author Egor Kulikov (egor@egork.net)
 */
public class ParserTask {
    public static void parserTask(final Project project, final String id, final DescriptionReceiver receiver, final Parser parser) {
        new Thread(new Runnable() {
            public void run() {
                if (id == null)
                    parser.getContests(project, receiver);
                else
                    parser.parseContest(project, id, receiver);
            }
        }).start();
    }
}
