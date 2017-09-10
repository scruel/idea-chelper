package net.egork.chelper.task;

import net.egork.chelper.task.test.TestBase;
import net.egork.chelper.util.OutputWriter;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public abstract class TaskBase<E extends TestBase> {
    //Basic
    public final String name;
    public final E[] tests;

    //Advanced
    public final String date;
    public final String contestName;
    public final String[] testClasses;

    public TaskBase(String name, E[] tests, String date, String contestName, String[] testClasses) {
        this.name = name;
        this.tests = tests;
        this.date = date;
        this.contestName = contestName;
        this.testClasses = testClasses;
    }


    public abstract TaskBase setTests(E[] tests);

    public abstract void saveTask(OutputWriter out);

    public abstract TaskBase setTestClasses(String[] testClasses);

    @Override
    public String toString() {
        return "taskName: " + this.name;
    }
}
