package net.egork.chelper;

import com.intellij.openapi.components.ApplicationComponent;
import net.egork.chelper.util.ProjectUtils;
import net.egork.chelper.util.SSLUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class CHelperMain implements ApplicationComponent {
    public CHelperMain() {
    }

    @Override
    public void initComponent() {
        ProjectUtils.addListeners();
        SSLUtils.trustAllHostnames();
        SSLUtils.trustAllHttpsCertificates();
    }

    @Override
    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "CHelperMain";
    }
}
