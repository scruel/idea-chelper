package net.egork.chelper.exception;

/**
 * Created by Scruel on 2017/9/9.
 * Github : https://github.com/scruel
 */
public class TaskCorruptException extends RuntimeException {
    private static final String defauleMessage = "CHelper could not continue because the associated data file '%file%' is missing or corrupt.";

    //TODO replace to bundle
    public static String getDefaultMessage(String filename) {
        return defauleMessage.replace("%file%", filename);
    }

    public TaskCorruptException() {
        super();
    }

    public TaskCorruptException(String message) {
        super(message);
    }
}
