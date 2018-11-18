package net.egork.chelper.task.test;

import net.egork.chelper.util.OutputWriter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class TopCoderTest implements TestBase {
    //base
    private final int index;
    private final boolean active;

    //advance
    private final String[] arguments;
    private final String result;

    public TopCoderTest(String[] arguments, String result, int index) {
        this(arguments, result, index, true);
    }

    public TopCoderTest(String[] arguments, String result, int index, boolean active) {
        this.arguments = arguments;
        this.result = result;
        this.active = active;
        this.index = index;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @NotNull
    public TopCoderTest setIndex(int index) {
        return new TopCoderTest(arguments, result, index, active);
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @NotNull
    @Override
    public TopCoderTest setActive(boolean active) {
        return new TopCoderTest(arguments, result, index, active);
    }

    @Override
    public void saveTest(OutputWriter out) {
    }

    @Override
    public String toString() {
        String representation = Arrays.toString(arguments);
        if (representation.length() > 15)
            representation = representation.substring(0, 12) + "...";
        return "Test #" + index + ": " + representation;
    }

    public String[] getArguments() {
        return arguments;
    }

    public String getResult() {
        return result;
    }
}
