package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.VerticalFlowLayout;
import net.egork.chelper.ProjectData;
import net.egork.chelper.codegeneration.MainFileTemplate;
import net.egork.chelper.util.FileUtils;
import net.egork.chelper.util.Messenger;
import net.egork.chelper.util.ProjectUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Egor Kulikov (egorku@yandex-team.ru)
 */
public class ProjectDataDialog extends JDialog {
    private ProjectData data;
    private boolean isOk = false;
    private final DirectorySelector defaultDirectory;
    private final DirectorySelector archiveDirectory;
    private final DirectorySelector outputDirectory;
    private final JCheckBox enableUnitTests;
    private final JCheckBox smartTesting;
    private final JCheckBox failOnIntegerOverflowForNewTasks;
    private final DirectorySelector testDirectory;
    private final ClassSelector inputClass;
    private final ClassSelector outputClass;
    private final JTextField excludePackages;
    private final JTextField author;
    private final JLabel testDirectoryLabel;
    private final int width = new JTextField(20).getPreferredSize().width;

    public ProjectDataDialog(final Project project, ProjectData data) {
        super(null, "Project settings", Dialog.ModalityType.APPLICATION_MODAL);
        setAlwaysOnTop(true);
        setResizable(false);
        this.data = data;
        defaultDirectory = new DirectorySelector(project, data.defaultDirectory);
        archiveDirectory = new DirectorySelector(project, data.archiveDirectory);
        outputDirectory = new DirectorySelector(project, data.outputDirectory);
        enableUnitTests = new JCheckBox("Enable unit tests", data.enableUnitTests);
        failOnIntegerOverflowForNewTasks = new JCheckBox("Fail on integer overflow for new tasks", data.failOnIntegerOverflowForNewTasks);
        testDirectory = new DirectorySelector(project, data.testDirectory);
        inputClass = new ClassSelector(project, data.inputClass);
        outputClass = new ClassSelector(project, data.outputClass);
        excludePackages = new JTextField(ProjectData.join(data.excludedPackages));
        author = new JTextField(data.author);
        smartTesting = new JCheckBox("Use smart testing", data.smartTesting);
        OkCancelPanel main = new OkCancelPanel(new VerticalFlowLayout()) {
            @Override
            public void onOk() {
                if (!ProjectDataDialog.this.isValidData(project)) return;
                onChange();
                isOk = true;
                ProjectDataDialog.this.setVisible(false);
            }

            @Override
            public void onCancel() {
                ProjectDataDialog.this.data = null;
                ProjectDataDialog.this.setVisible(false);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension dimension = super.getPreferredSize();
                dimension.width = width;
                return dimension;
            }
        };
        JPanel okCancelPanel = new JPanel(new GridLayout(1, 2));
        okCancelPanel.add(main.getOkButton());
        okCancelPanel.add(main.getCancelButton());
        main.add(new JLabel("Default directory:"));
        main.add(defaultDirectory);
        main.add(new JLabel("Archive directory:"));
        main.add(archiveDirectory);
        main.add(new JLabel("Output directory:"));
        main.add(outputDirectory);
        main.add(enableUnitTests);
        testDirectoryLabel = new JLabel("Test directory:");
        main.add(testDirectoryLabel);
        main.add(testDirectory);
        main.add(new JLabel("Input class:"));
        main.add(inputClass);
        main.add(new JLabel("Output class:"));
        main.add(outputClass);
        main.add(new JLabel("Exclude packages:"));
        main.add(excludePackages);
        main.add(new JLabel("Author:"));
        main.add(author);
        main.add(failOnIntegerOverflowForNewTasks);
        main.add(smartTesting);
        main.add(okCancelPanel);
        testDirectory.setVisible(enableUnitTests.isSelected());
        testDirectoryLabel.setVisible(enableUnitTests.isSelected());
        enableUnitTests.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testDirectory.setVisible(enableUnitTests.isSelected());
                testDirectoryLabel.setVisible(enableUnitTests.isSelected());
                pack();
            }
        });
        setContentPane(main);
        onChange();
        pack();
        Point center = ProjectUtils.getLocation(project, main.getSize());
        setLocation(center);
    }

    private boolean isValidData(Project project) {
        if (!FileUtils.isValidClass(project, ProjectDataDialog.this.inputClass.getText())) {
            Messenger.publishMessageWithBalloon(project, this.inputClass, "Class '" + ProjectDataDialog.this.inputClass.getText() + "'\n not found", MessageType.ERROR);
            return false;
        }

        if (!FileUtils.isValidClass(project, ProjectDataDialog.this.outputClass.getText())) {
            Messenger.publishMessageWithBalloon(project, this.outputClass, "Class '" + ProjectDataDialog.this.outputClass.getText() + "'\n not found", MessageType.ERROR);
            return false;
        }

        if (!MainFileTemplate.isValidInputClass(project, ProjectDataDialog.this.inputClass.getText())) {
            Messenger.publishMessageWithBalloon(project, this.inputClass, "not all mandatory methods in\n" +
                "input class '" + ProjectDataDialog.this.inputClass.getText() + "'\n" +
                "are defined.", MessageType.ERROR);
            return false;
        }

        if (!MainFileTemplate.isValidOutputClass(project, ProjectDataDialog.this.outputClass.getText())) {
            Messenger.publishMessageWithBalloon(project, this.outputClass, "not all mandatory methods in\n" +
                "output class '" + ProjectDataDialog.this.outputClass.getText() + "\n'" +
                "are defined.", MessageType.ERROR);
            return false;
        }

        return true;
    }

    private void onChange() {
        data = new ProjectData(inputClass.getText(), outputClass.getText(), excludePackages.getText().split(","), outputDirectory.getText(), author.getText(), archiveDirectory.getText(), defaultDirectory.getText(), testDirectory.getText(), enableUnitTests.isSelected(), failOnIntegerOverflowForNewTasks.isSelected(), smartTesting.isSelected(), true);
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            author.requestFocusInWindow();
            author.setSelectionStart(0);
            author.setSelectionEnd(author.getText().length());
        } else if (!isOk)
            data = null;
        super.setVisible(b);
    }

    public static ProjectData edit(Project project, ProjectData data) {
        ProjectDataDialog dialog = new ProjectDataDialog(project, data == null ? ProjectData.DEFAULT : data);
        dialog.setVisible(true);
        return dialog.data;
    }
}
