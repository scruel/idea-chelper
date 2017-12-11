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
        LOG.setLevel(Level.DEBUG);
    }

    public StackTraceLogger(@NotNull Class cl) {
        this("#" + cl.getName());
    }

    public String getClickableMethodString() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        sb.append(String.format("%s(%s:%d)", stacks[2].getMethodName(), stacks[2].getFileName(), stacks[2].getLineNumber()));
        return sb.toString();
    }

    /**
     * This method is for debug using. If {@param tuple} is true
     * <p>
     * usage: debugMethodInfo(true, true, "valueName1", value1, "valueName2", value2);
     * usage: debugMethodInfo(true, true, value1, value2);
     *
     * @param enter   determines the direction of arrow string.
     * @param tuple   determines the style of message.
     *                if <code>true</code> and the length of param 'objects' is even, this method will log debug message like <b>valueName1: "value1", valueName2: "value2", ...</b>.
     *                if <code>false</code>, message will like <b>"obj1", "obj2", ...</b>.
     * @param objects object values.
     */
    public void debugMethodInfo(boolean enter, boolean tuple, Object... objects) {
        if (tuple && (objects.length & 1) != 0)
            throw new RuntimeException("The length of 'objects' must be even.");
        StringBuilder message = new StringBuilder();
        message.append(getClickableMethodString());
        message.append(getEnterSymbol(enter));

        for (int i = 0; i < objects.length; i++) {
            String str;
            if (null == objects[i]) {
                str = "null";
            } else {
                str = objects[i].toString();
                str = str.replace("\n", "\\n").replace("\t", "\\t");
                if (str.length() > 100) {
                    str = str.substring(0, 100);
                }
            }

            if (tuple && (i & 1) == 0) {
                message.append(str).append(": ");
            } else {
                message.append("\"").append(objects[i]).append("\"");
                if (i != objects.length - 1) {
                    message.append(", ");
                } else {
                    message.append(".");
                }
            }
        }
        this.debug(message.toString());
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
        return enter ? " -> " : " <- ";
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String message) {
        LOG.debug(message);
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
