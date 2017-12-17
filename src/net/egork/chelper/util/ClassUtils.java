package net.egork.chelper.util;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import net.egork.chelper.codegeneration.MainFileTemplate;

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
}
