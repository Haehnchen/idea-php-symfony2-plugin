package fr.adrienbrault.idea.symfony2plugin.profiler;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import javax.swing.*;

public class SymfonyWebProfilerWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {

    private static final Icon TOOLWINDOW_ICON = Symfony2Icons.SYMFONY_TOOL_WINDOW;

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        SymfonyWebProfilerPane symfony2SearchPane = new SymfonyWebProfilerPane(project);
        setUpContent((ToolWindowEx)toolWindow, symfony2SearchPane);
        toolWindow.setIcon(TOOLWINDOW_ICON);
    }

    private static void setUpContent(ToolWindowEx toolWindow, SymfonyWebProfilerPane symfony2SearchPane) {
        symfony2SearchPane.setup(toolWindow);
    }

    @Override
    public boolean value(Project project) {
        return Symfony2ProjectComponent.isEnabled(project);
    }

}
