package net.egork.chelper.util;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Scruel on 2017/9/15.
 * Github : https://github.com/scruel
 * just for convince. aspect will be better.
 */
public class StackTraceLogger extends Logger {
    private Logger LOG;

    public StackTraceLogger(@NotNull @NonNls String category) {
        LOG = Logger.getInstance(category);
    }

    public StackTraceLogger(@NotNull Class cl) {
        LOG = Logger.getInstance(cl);
    }

    public String getCurrentMethodInfo() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        sb.append(String.format("%s(%s:%d)", stacks[2].getMethodName(), stacks[2].getFileName(), stacks[2].getLineNumber()));
        return sb.toString();
    }

    public void printMethodInfoWithValues(boolean enter, Object... values) {
        StringBuilder sb = new StringBuilder();
        sb.append(getCurrentMethodInfo());
        sb.append(getEnterSymbol(enter));
        List<String> res = processObjectsToStringList(values);
        for (int i = 0; i < res.size(); i++) {
            sb.append(String.format("\"%s\"", res.get(i)));
            if (i != res.size() - 1) {
                sb.append(", ");
            } else {
                sb.append(".");
            }
        }
        info(sb.toString());
    }

    /**
     * usage: printMethodInfoWithNamesAndValues(true, "value1Name", value1, "value2Name", value2)
     *
     * @param enter
     * @param namesAndValues
     * @return
     */
    public void printMethodInfoWithNamesAndValues(boolean enter, Object... namesAndValues) {
        if ((namesAndValues.length & 1) != 0) throw new RuntimeException("改改吧。。。");
        StringBuilder sb = new StringBuilder();
        sb.append(getCurrentMethodInfo());
        sb.append(getEnterSymbol(enter));

        List<String> res = new LinkedList<String>();
        for (Object obj : namesAndValues) {
            if (obj == null) {
                res.add(null);
                continue;
            }
            res.add(obj.toString());
        }
        for (int i = 0; i < res.size(); i += 2) {
            if (null == (res.get(i + 1))) continue;
            res.set(i + 1, res.get(i + 1)
                .replace("\n", "\\n")
                .replace("\t", "\\t"));
            if (res.get(i + 1).length() > 100) {
                res.set(i + 1, res.get(i + 1).substring(0, 100) + " ...");
            }
        }

        for (int i = 0; i < res.size(); i += 2) {
            sb.append(String.format("%s: \"%s\"", res.get(i), res.get(i + 1)));
            if (i != res.size() - 2) {
                sb.append(", ");
            } else {
                sb.append(".");
            }
        }
        info(sb.toString());
    }

    private List<String> processObjectsToStringList(Object[] objs) {
        List<String> res = new LinkedList<String>();
        for (Object obj : objs) {
            if (obj == null) {
                res.add(null);
                continue;
            }
            res.add(obj.toString());
        }
        for (int i = 0; i < objs.length; i++) {
            if (null == (res.get(i))) continue;
            res.set(i, res.get(i)
                .replace("\n", "\\n")
                .replace("\t", "\\t"));
            if (res.get(i).length() > 100) {
                res.set(i, res.get(i).substring(0, 100));
            }
        }
        return res;
    }

    private String getEnterSymbol(boolean enter) {
        return enter ? "-> " : "<- ";
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String message) {
        LOG.error(message);
    }

    @Override
    public void debug(@Nullable Throwable t) {
        LOG.debug(t);
    }

    @Override
    public void debug(String message, @Nullable Throwable t) {
        LOG.debug(message, t);
    }

    @Override
    public void info(String message) {
        LOG.info(message);
    }

    @Override
    public void info(String message, @Nullable Throwable t) {
        LOG.info(message, t);
    }

    @Override
    public void warn(String message, @Nullable Throwable t) {
        LOG.warn(message, t);
    }

    @Override
    public void error(String message, @Nullable Throwable t, @NotNull String... details) {
        LOG.error(message, t, details);
    }

    @Override
    public void setLevel(Level level) {
        LOG.setLevel(level);
    }
}
