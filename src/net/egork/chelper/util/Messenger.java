package net.egork.chelper.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import javax.swing.*;

/**
 * @author egorku@yandex-team.ru
 */
public class Messenger {
    private Messenger() {
    }

    public static void publishMessage(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("chelper", "CHelper", message, type));
    }

    public static void publishMessageWithBalloon(Project project, String message, MessageType type) {
        StatusBar statusBar = WindowManager.getInstance()
            .getStatusBar(project);
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setFadeoutTime(7500)
            .createBalloon()
            .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                Balloon.Position.atRight);
    }

    public static void publishMessageWithBalloon(Project project, JComponent component, String message, MessageType type) {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setFadeoutTime(7500)
            .createBalloon()
            .show(RelativePoint.getCenterOf(component),
                Balloon.Position.atRight);
    }
}
