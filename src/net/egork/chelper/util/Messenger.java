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
    //要不要整理一下message。。。233
    private static final NotificationGroup NOTIFICATIONS = new NotificationGroup(ProjectUtils.PROJECT_NAME, NotificationDisplayType.BALLOON, false);

    private Messenger() {
    }

    public static void publishMessage(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification(ProjectUtils.PROJECT_NAME, ProjectUtils.PROJECT_NAME, message, type));
//        Messages.showInfoMessage( ... );
    }

    /**
     * wan't display until user click close.
     *
     * @param message
     * @param type
     */
    public static void publishHoledMessage(String message, NotificationType type) {
        NOTIFICATIONS.createNotification("CHelper", message, type, null).notify(null);
//        Messages.showInfoMessage( ... );
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
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, type, null)
            .setFadeoutTime(5000)
            .createBalloon()
            .show(RelativePoint.getCenterOf(component),
                Balloon.Position.atRight);
    }
}
