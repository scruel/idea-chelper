package net.egork.chelper.task.test;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public abstract class TestBase {
    public final int index;

    public TestBase(int index) {
        this.index = index;
    }

    public abstract TestBase setActive(boolean active);

}
