package fr.adrienbrault.idea.symfonyplugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractProjectDumbAwareAction extends DumbAwareAction {

    public AbstractProjectDumbAwareAction(String text, String description, Icon phpFile) {
        super(text, description, phpFile);
    }

    public void update(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            setStatus(event, false);
        }
    }

    protected void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }


}
