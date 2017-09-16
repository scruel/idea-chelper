package net.egork.chelper.parser;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import net.egork.chelper.util.StackTraceLogger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Scruel on 2017/9/12.
 * Github : https://github.com/scruel
 */
public class ParseProgresser {
    private static final StackTraceLogger LOG = new StackTraceLogger(ParseProgresser.class);
    private static String requestURL;

    public static String getWebPageContent(Project project, final DescriptionReceiver receiver, String url) {
        return getWebPageContent(project, receiver, url, "UTF-8");
    }

    public static String getWebPageContent(final Project project, final DescriptionReceiver receiver, final String urlstr, final String charset) {
        ParseProgresser.requestURL = urlstr;
        final BlockingQueue<StringBuilder> resBlockingQueue = new ArrayBlockingQueue<StringBuilder>(1);
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, "Parse Pages", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText("Parsing pages form '" + ParseProgresser.requestURL + "'");
                    for (int i = 0; i < 10; i++) {
                        if (receiver.isStopped()) return;
                        indicator.setFraction((i + 1) / 10);
                        try {
                            URL url = new URL(ParseProgresser.requestURL);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setReadTimeout(10000);
                            conn.connect();

                            InputStream input = conn.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(charset)));
                            StringBuilder builder = new StringBuilder();
                            String s;
                            while ((s = reader.readLine()) != null) {
                                builder.append(s).append('\n');
                            }
                            reader.close();
                            resBlockingQueue.add(builder);
                            return;
                        } catch (IOException ignored) {
                        }
                    }
                    indicator.setText("finished");
                }
            }
        );
        try {
            StringBuilder result = resBlockingQueue.take();
            if (receiver.isStopped() || result.length() == 0) {
                LOG.printMethodInfoWithNamesAndValues(false, "receiver", receiver, "url", urlstr, "stop", receiver.isStopped());
                return null;
            }
            return result.toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOG.printMethodInfoWithNamesAndValues(false, "receiver", receiver, "url", urlstr);
        return null;
    }
}
