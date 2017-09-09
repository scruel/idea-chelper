package net.egork.chelper.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public class ExecuteUtils {
    private ExecuteUtils() {
    }

    public static void executeStrictWriteAction(final Runnable action) {
        executeStrictWriteAction(null, action);
    }

    public static void executeStrictWriteAction(final Project project, final Runnable runnable) {
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
        executeStrictWriteActionAndWait(null, action);
    }

    public static void executeStrictWriteActionAndWait(final Project project, final Runnable runnable) {
        final Application application = ApplicationManager.getApplication();

        application.invokeAndWait(
            new Runnable() {
                @Override
                public void run() {
                    application.runWriteAction(runnable);
                }
            },
            ModalityState.NON_MODAL
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
