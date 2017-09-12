package net.egork.chelper.parser;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by Scruel on 2017/9/12.
 * Github : https://github.com/scruel
 */
public class ParseProgresser {
    private static final Logger LOG = Logger.getInstance(ParseProgresser.class);
    private static String requestURL;

    public static String getWebPageContent(Project project, final DescriptionReceiver receiver, String url) {
        LOG.info("getWebPageContent: " + url);
        return getWebPageContent(project, receiver, url, "UTF-8");
    }

    public static String getWebPageContent(final Project project, final DescriptionReceiver receiver, final String url, final String charset) {
        LOG.info("START getWebPageContent: " + url);
        ParseProgresser.requestURL = url;
        final boolean[] flag = {false};
        final StringBuilder resultBuilder = new StringBuilder();
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, "Parse Pages", true) {
                @Override
                public void onCancel() {
                    receiver.isStopped();
                }

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setText("Parsing pages form '" + ParseProgresser.requestURL + "'");
                    for (int i = 0; i < 10; i++) {
                        indicator.setFraction((i + 1) / 10);
                        try {
                            URL url = new URL(ParseProgresser.requestURL);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setReadTimeout(10000);
                            conn.setRequestProperty("User-Agent", "GoogleBot");
                            conn.connect();

                            InputStream input = conn.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(input, Charset.forName(charset)));
                            StringBuilder builder = new StringBuilder();
                            String s;
                            while ((s = reader.readLine()) != null) {
                                builder.append(s).append('\n');
                            }
                            reader.close();
                            resultBuilder.append(builder);
                            flag[0] = true;
                            return;
                        } catch (IOException ignored) {
                        }
                    }
                    indicator.setText("finished");
                }
            }
        );
        while (!flag[0]) {
            if (receiver.isStopped()) {
                return null;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (resultBuilder.length() == 0) {
            return null;
        }
        LOG.info("END getWebPageContent: " + url);
        return resultBuilder.toString();
    }
}
