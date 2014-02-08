package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import fr.adrienbrault.idea.symfony2plugin.toolwindow.Symfony2SearchForm;

public class SymfonyWebProfilerPane {

    private Project project;
    protected SymfonyWebProfilerPane(Project project) {
        this.project = project;
    }

    public void setup(ToolWindowEx toolWindow) {

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(new Symfony2WebProfilerForm(this.project).createComponent(), null, true);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content, true);

    }

}
