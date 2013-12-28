package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

public class NewXmlServiceAction  extends AbstractProjectDumbAwareAction {

    public NewXmlServiceAction() {
        super("Xml Service", "Create new Xml File", AllIcons.FileTypes.Xml);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        final Project project = event.getData(PlatformDataKeys.PROJECT);
        ServiceActionUtil.buildFile(event, project, "/resources/fileTemplates/container.xml");
    }


}
