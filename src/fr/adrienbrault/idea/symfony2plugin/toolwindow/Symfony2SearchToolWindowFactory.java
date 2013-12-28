package fr.adrienbrault.idea.symfony2plugin.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import com.intellij.openapi.util.Condition;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;

import javax.swing.*;

public class Symfony2SearchToolWindowFactory implements ToolWindowFactory, Condition<Project>, DumbAware {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        Symfony2SearchPane symfony2SearchPane = new Symfony2SearchPane(project);
        setUpContent((ToolWindowEx)toolWindow, symfony2SearchPane);
    }

    private static void setUpContent(ToolWindowEx toolWindow, Symfony2SearchPane symfony2SearchPane) {
        symfony2SearchPane.setup(toolWindow);
    }

    @Override
    public boolean value(Project project) {
        return Symfony2ProjectComponent.isEnabled(project);
    }

}
