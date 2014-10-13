package fr.adrienbrault.idea.symfony2plugin.remote.httpHandler;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import fr.adrienbrault.idea.symfony2plugin.remote.util.HttpExchangeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProjectActionHandler implements HttpHandler {

    public void handle(HttpExchange xchg) throws IOException {

        String path = xchg.getRequestURI().getPath();
        if(path.equals("/project")) {
            HttpExchangeUtil.sendResponse(xchg, "Projects: " + StringUtils.join(getProjectNames(), " "));
            return;
        }

        String[] pathElements = StringUtils.strip(path, "/").split("/");
        if(pathElements.length < 2) {
            HttpExchangeUtil.sendResponse(xchg, "invalid project name");
            return;
        }

        String projectName = pathElements[1];

        Project project = getProject(projectName);
        if(project == null) {
            HttpExchangeUtil.sendResponse(xchg, "invalid project");
            return;
        }

        if(xchg.getRequestMethod().equals("GET")) {
            new ProjectGetActionHandler().handleProject(project, xchg, pathElements);
            return;
        }

        new ProjectPostActionHandler().handleProject(project, xchg);

    }

    @Nullable
    private Project getProject(String name) {

        for(Project project: ProjectManager.getInstance().getOpenProjects()) {
            if(name.equals(project.getName())) {
                return project;
            }
        }

        return null;
    }

    @Nullable
    private Collection<String> getProjectNames() {

        Collection<String> projectNames = new HashSet<String>();

        for(Project project: ProjectManager.getInstance().getOpenProjects()) {
            projectNames.add(project.getName());
        }

        return projectNames;
    }
}
