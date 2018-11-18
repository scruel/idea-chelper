package net.egork.chelper.util;

import com.intellij.notification.*;
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
    private static String currentMessage;
    private static final NotificationGroup NOTIFICATIONS = new NotificationGroup(ProjectUtils.PROJECT_NAME, NotificationDisplayType.BALLOON, false);

    private Messenger() {
    }

    public static void publishMessage(String message, NotificationType type) {
        if (currentMessage == null) {
            currentMessage = message;
        } else {
            if (message.equals(currentMessage)) return;
            currentMessage = message;
        }
        Notifications.Bus.notify(new Notification(ProjectUtils.PROJECT_NAME, ProjectUtils.PROJECT_NAME, message, type));
    }

    public static void publishMessageWithBalloon(Project project, String message, MessageType type) {
        StatusBar statusBar = WindowManager.getInstance()
            .getStatusBar(project);
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setFadeoutTime(5000)
            .createBalloon()
            .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                Balloon.Position.atRight);
    }

    public static void publishMessageWithBalloon(Project project, JComponent component, String message, MessageType type) {
        if (currentMessage == null) {
            currentMessage = message;
        } else {
            if (message.equals(currentMessage)) return;
            currentMessage = message;
        }
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setFadeoutTime(5000)
            .createBalloon()
            .show(RelativePoint.getCenterOf(component),
                Balloon.Position.atRight);
    }
}
