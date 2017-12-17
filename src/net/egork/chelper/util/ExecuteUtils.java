package net.egork.chelper.util;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Scruel Tao
 */
public class ExecuteUtils {
    private static final StackTraceLogger LOG = new StackTraceLogger(ExecuteUtils.class);

    private ExecuteUtils() {
    }

    private static boolean holdsReadLock(ApplicationEx applicationEx) {
        try {
            return applicationEx.holdsReadLock();
        } catch (Exception e) {
            return true;
        }
    }

    public static void executeWriteCommandAction(final Project project, final Runnable runnable, boolean blocking) {
        LOG.debugMethodInfo(true, true, "thread", Thread.currentThread().getName(), "project", project, "blocking", blocking);
        final ApplicationEx application = ApplicationManagerEx.getApplicationEx();
        Runnable writeRunnable = new Runnable() {
            @Override
            public void run() {
                WriteCommandAction.runWriteCommandAction(project, runnable);
            }
        };

        if (!blocking) {
            application.invokeLater(writeRunnable, ModalityState.NON_MODAL);
            LOG.debugMethodInfo(false, false);
            return;
        }
        if (application.isDispatchThread()) {
            if (application.isWriteAccessAllowed())
                runnable.run();
            else
                writeRunnable.run();
            LOG.debugMethodInfo(false, false);
            return;
        }
        if (!application.isReadAccessAllowed()) {
            application.invokeAndWait(writeRunnable, ModalityState.NON_MODAL);
            LOG.debugMethodInfo(false, false);
            return;
        }
        LOG.debugMethodInfo(false, false);
        LOG.error("Could not run write action with wait.");
        throw new IllegalStateException("Could not run write action with wait.");
    }

    public static void executeWriteAction(final Runnable runnable, boolean blocking) {
        LOG.debugMethodInfo(true, true, "blocking", blocking);
        final ApplicationEx application = ApplicationManagerEx.getApplicationEx();
        Runnable writeRunnable = new Runnable() {
            @Override
            public void run() {
                application.runWriteAction(runnable);
            }
        };
        if (!blocking) {
            application.invokeLater(writeRunnable, ModalityState.NON_MODAL);
            LOG.debugMethodInfo(false, false);
            return;
        }
        if (application.isDispatchThread()) {
            if (application.isWriteAccessAllowed())
                runnable.run();
            else
                writeRunnable.run();
            LOG.debugMethodInfo(false, false);
            return;
        }
        if (!holdsReadLock(application)) {
            application.invokeAndWait(writeRunnable, ModalityState.NON_MODAL);
            LOG.debugMethodInfo(false, false);
            return;
        }
        LOG.debugMethodInfo(false, false);
        LOG.error("Could not run write action with wait.");
        throw new IllegalStateException("Could not run write action with wait.");
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

    //TODO
    public static <E> E executeOnPooledThreadWriteCommandActionWithResult(final Project project, final Callable<E> callable) {
        final Application application = ApplicationManager.getApplication();
        Future<E> task = application.executeOnPooledThread(
            new Callable<E>() {
                @Override
                public E call() throws Exception {
                    E res = executeWriteAction(project, callable);
                    return res;
                }
            }
        );
        try {
            return task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
