package net.egork.chelper.exception;

/**
 * Created by Scruel on 2017/9/9.
 * Github : https://github.com/scruel
 */
public class TaskCorruptException extends RuntimeException {
    public TaskCorruptException() {
        super("CHelper could not continue because the associated data file is missing or corrupt");
    }

    public TaskCorruptException(String message) {
        super(message);
    }
}