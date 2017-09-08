package net.egork.chelper.task.test;

import java.util.Arrays;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTest extends TestBase {
    public final String[] arguments;
    public final String result;
    public final boolean active;

    public TopCoderTest(String[] arguments, String result, int index) {
        this(arguments, result, index, true);
    }

    public TopCoderTest(String[] arguments, String result, int index, boolean active) {
        super(index);
        this.arguments = arguments;
        this.result = result;
        this.active = active;
    }

    public String toString() {
        String representation = Arrays.toString(arguments);
        if (representation.length() > 15)
            representation = representation.substring(0, 12) + "...";
        return "Test #" + index + ": " + representation;
    }

    public TopCoderTest setIndex(int index) {
        return new TopCoderTest(arguments, result, index, active);
    }

    @Override
    public TopCoderTest setActive(boolean active) {
        return new TopCoderTest(arguments, result, index, active);
    }
}
