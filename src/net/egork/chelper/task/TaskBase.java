package net.egork.chelper.task;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public class TaskBase {
    //for task map
    public static final String TASK_KEY = "TASK_KEY";
    public static final String TASK_SOURCE_KEY = "TASK_SOURCE_KEY";
    public static final String TASK_DATA_KEY = "TASK_DATA_KEY";

    //Basic
    public final String name;

    //Advanced
    public final String date;
    public final String contestName;

    public TaskBase(String name, String date, String contestName) {
        this.name = name;
        this.date = date;
        this.contestName = contestName;
    }

    @Override
    public String toString() {
        return "taskName: " + this.name;
    }
}
