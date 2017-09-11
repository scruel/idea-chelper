package net.egork.chelper.task.test;


import net.egork.chelper.util.InputReader;
import net.egork.chelper.util.OutputWriter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class Test implements TestBase {
    private final int index;
    private final String input;
    private final String output;
    private final boolean active;

    public Test(String input) {
        this(input, null);
    }

    public Test(String input, String output) {
        this(input, output, -1);
    }

    public Test(String input, String output, int index) {
        this(input, output, index, true);
    }

    public Test(String input, String output, int index, boolean active) {
        this.input = input;
        this.output = output;
        this.active = active;
        this.index = index;
    }

    public Test(InputReader in) {
        this.index = in.readInt();
        this.input = in.readString();
        this.output = in.readString();
        this.active = in.readBoolean();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    @Override
    public Test setIndex(int index) {
        return new Test(input, output, index, active);
    }

    public String getInput() {
        return input;
    }

    public String getOutput() {
        return output;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @NotNull
    @Override
    public Test setActive(boolean active) {
        return new Test(input, output, index, active);
    }

    public void saveTest(OutputWriter out) {
        out.printLine(index);
        out.printString(input);
        out.printString(output);
        out.printBoolean(active);
    }

    @Override
    public String toString() {
        String inputRepresentation = input.replace('\n', ' ');
        inputRepresentation = inputRepresentation.length() > 15 ? inputRepresentation.substring(0, 12) + "..." :
            inputRepresentation;
        return "Test #" + index + ": " + inputRepresentation;
    }
}
