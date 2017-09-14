package net.egork.chelper.parser;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.ProjectUtils;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by Scruel on 2017/9/13.
 * Github : https://github.com/scruel
 */
public abstract class TaskProcessReceiver {
    private int totalSum = -1;
    private int currentCount = 0;
    private boolean firstConfiguration = true;
    private PsiElement firstElement = null;
    private Collection<Task> result = new HashSet<Task>();
    private Project project;
//    private boolean canceled;

    public TaskProcessReceiver(Project project) {
        this.project = project;
    }

    public boolean isFirstConfiguration() {
        return firstConfiguration;
    }

    public void setFirstConfiguration(boolean firstConfiguration) {
        this.firstConfiguration = firstConfiguration;
    }

    public PsiElement getFirstElement() {
        return firstElement;
    }

    public void setFirstElement(PsiElement firstElement) {
        this.firstElement = firstElement;
    }

    public Project getProject() {
        return project;
    }

    public int getTotalSum() {
        return totalSum;
    }

    public void setTotalSum(int totalSum) {
        this.totalSum = totalSum;
    }

    private void processAllTasks() {
        for (Task task : result) {
            processTask(task);
        }
    }

    public Task getFirstTask() {
        if (isEmpty()) return null;
        return result.iterator().next();
    }

    private void increaseTotalSum() {
        this.currentCount++;
        if (totalSum != -1 && totalSum == currentCount) {
            processAllTasks();
            if (firstElement != null) {
                ProjectUtils.openElement(project, firstElement);
            }
        }
    }

    protected abstract void processTask(Task task);

    public void receiveTask(Task task) {
        result.add(task);
        increaseTotalSum();
    }

    public boolean isEmpty() {
        return result.isEmpty();
    }
}
