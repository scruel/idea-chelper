package net.egork.chelper.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public class ExecuteUtils {
    private ExecuteUtils() {
    }

    public static void executeStrictWriteAction(final Runnable action) {
        executeStrictWriteAction(action, null);
    }

    public static void executeStrictWriteAction(final Runnable runnable, final Project project) {
        final Application application = ApplicationManager.getApplication();

        if (application.isDispatchThread() && application.isWriteAccessAllowed()) {
            runnable.run();
        } else {
            application.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        application.runWriteAction(runnable);
                    }
                }
//                , ModalityState.NON_MODAL
            );
        }
    }

    public static void executeStrictWriteActionAndWait(final Runnable action) {
        executeStrictWriteActionAndWait(action, null);
    }

    public static void executeStrictWriteActionAndWait(final Runnable runnable, final Project project) {
        final Application application = ApplicationManager.getApplication();

        application.invokeAndWait(
            new Runnable() {
                @Override
                public void run() {
                    application.runWriteAction(runnable);
                }
            }
//                , ModalityState.NON_MODAL
        );
    }

//    TODO
//    public static void executeStrictReadActionAndWait(final Runnable action) {
//        executeStrictReadAction(action, null);
//    }
//
//    private static void executeStrictReadAction(Runnable action, Object o) {
//
//    }
}
