package fr.adrienbrault.idea.symfony2plugin.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

public class Symfony2SearchPane {

    private Project project;
    protected Symfony2SearchPane(Project project) {
        this.project = project;
    }

    public void setup(ToolWindowEx toolWindow) {

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(new Symfoyn2SearchForm(this.project).createComponent(), null, true);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content, true);

    }

}
