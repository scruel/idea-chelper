package net.egork.chelper.util;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import net.egork.chelper.codegeneration.MainFileTemplate;
import org.jetbrains.annotations.NotNull;

/**
 * @author Scruel Tao
 */
public class ClassUtils {
    private ClassUtils() {
    }

    public static void checkClass(Project project, String clazzName) throws RuntimeConfigurationException {
        PsiClass clazz = MainFileTemplate.getClass(project, clazzName);
        if (clazz == null) {
            throw new RuntimeConfigurationWarning("Class '" + clazzName + "' not found.");
        }
    }

    public static void compileFile(@NotNull final Project project, @NotNull final VirtualFile[] files) {
        ExecuteUtils.executeWriteAction(new Runnable() {
            @Override
            public void run() {
                CompilerManager.getInstance(project).compile(files, null);
            }
        }, true);
    }

    public static void compileFile(@NotNull final Project project, @NotNull final VirtualFile file) {
        VirtualFile[] files = new VirtualFile[]{file};
        compileFile(project, files);
    }
}
