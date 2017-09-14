package net.egork.chelper.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by Scruel on 2017/9/8.
 * Github : https://github.com/scruel
 */
public class ExecuteUtils {
    private static final Logger LOG = Logger.getInstance(ExecuteUtils.class);

    private ExecuteUtils() {
    }

    public static void executeWriteCommandAction(final Project project, final Runnable runnable, boolean blocking) {
        final ApplicationEx application = ApplicationManagerEx.getApplicationEx();

        if (application.isDispatchThread()) {
            WriteCommandAction.runWriteCommandAction(project, runnable);
            return;
        }
        Runnable _runnable = new Runnable() {
            @Override
            public void run() {
                WriteCommandAction.runWriteCommandAction(project, runnable);
            }
        };
        if (blocking && !application.holdsReadLock()) {
            application.invokeAndWait(_runnable, application.getDefaultModalityState());
            return;
        }
        application.invokeLater(_runnable, application.getDefaultModalityState());
    }

    public static void executeWriteAction(final Runnable runnable, boolean blocking) {
        final ApplicationEx application = ApplicationManagerEx.getApplicationEx();

        if (application.isDispatchThread()) {
            application.runWriteAction(runnable);
            return;
        }
        Runnable _runnable = new Runnable() {
            @Override
            public void run() {
                application.runWriteAction(runnable);
            }
        };
        if (blocking && !application.holdsReadLock()) {
            application.invokeAndWait(_runnable, application.getDefaultModalityState());
            return;
        }
        application.invokeLater(_runnable, application.getDefaultModalityState());
    }

    public static void executeReadAction(final Runnable runnable) {
        final Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            runnable.run();
            return;
        }
        application.runReadAction(runnable);
//        application.invokeLater(
//            new Runnable() {
//                @Override
//                public void run() {
//                    application.runReadAction(runnable);
//                }
//            }
//        );
    }


    private static <T> T executeWriteAction(final Project project, final Callable<T> callable) {
        return new WriteCommandAction<T>(project) {
            @Override
            protected void run(@NotNull Result<T> result) throws Throwable {
                try {
                    result.setResult(callable.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    ((PsiModificationTrackerImpl) PsiManager.getInstance(getProject()).getModificationTracker()).incCounter();
                }
            }
        }.execute().getResultObject();
    }

    public static <E> E executeOnPooledThreadWriteCommandActionWithResult(final Project project, final Callable<E> callable) {
        LOG.info("START executeOnPooledThreadWriteCommandActionWithResult");
        final Application application = ApplicationManager.getApplication();
        Future<E> task = application.executeOnPooledThread(
            new Callable<E>() {
                @Override
                public E call() throws Exception {
                    LOG.info("START executeOnPooledThreadWriteCommandActionWithResult-> executeOnPooledThread");
                    E res = executeWriteAction(project, callable);
                    LOG.info("END executeOnPooledThreadWriteCommandActionWithResult-> executeOnPooledThread");
                    return res;
                }
            }
        );
        try {
            LOG.info("END executeOnPooledThreadWriteCommandActionWithResult");
            return task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        LOG.info("Failed executeOnPooledThreadWriteCommandActionWithResult");
        return null;
    }


}
