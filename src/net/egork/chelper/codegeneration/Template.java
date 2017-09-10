package net.egork.chelper.codegeneration;

import net.egork.chelper.task.Task;

/**
 * @author egor@egork.net
 */
public class Template {
    protected final String template;

    public Template(String template) {
        this.template = template;
    }

    public String apply(String... replacement) {
        if (replacement.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        String result = template;
        for (int i = 0; i < replacement.length; i += 2) {
//            if (replacement[i] == null || replacement[i + 1] == null) continue;
            result = result.replace("%" + replacement[i] + "%", replacement[i + 1]);
        }
        result = result.replace("%date%", Task.getDateString('/'));
        return result;
    }
}
