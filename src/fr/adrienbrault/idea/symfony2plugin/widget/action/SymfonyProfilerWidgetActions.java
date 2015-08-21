package fr.adrienbrault.idea.symfony2plugin.widget.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class SymfonyProfilerWidgetActions {

    public static class TemplateAction extends AnAction {

        private String templateName;
        private Project project;

        public TemplateAction(Project project, @Nullable String text) {
            super(TwigUtil.getFoldingTemplateNameOrCurrent(text), "Open Template", TwigIcons.TwigFileIcon);
            this.templateName = text;
            this.project = project;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {

            List<PsiFile> psiFiles = Arrays.asList(TwigHelper.getTemplatePsiElements(project, templateName));

            // @TODO: multiple targets?
            if(psiFiles.size() > 0) {
                IdeHelper.navigateToPsiElement(psiFiles.get(0));
            }

        }
    }

    public static class RouteAction extends AnAction {

        private String routeName;
        private Project project;

        public RouteAction(Project project, @Nullable String text) {
            super(text, "Open Route", Symfony2Icons.ROUTE);
            this.routeName = text;
            this.project = project;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            PsiElement[] targets = RouteHelper.getMethods(project, this.routeName);
            if(targets.length > 0) {
                IdeHelper.navigateToPsiElement(targets[0]);
            }
        }

    }

    public static class MethodAction extends AnAction {

        private String methodShortcut;
        private Project project;

        public MethodAction(Project project, @Nullable String text) {
            super(text, "Open Method", com.jetbrains.php.PhpIcons.METHOD);
            this.methodShortcut = text;
            this.project = project;
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            PsiElement[] method = RouteHelper.getMethodsOnControllerShortcut(project, this.methodShortcut);
            if(method.length > 0) {
                IdeHelper.navigateToPsiElement(method[0]);
            }
        }
    }

    public static class UrlAction extends AnAction {

        private ProfilerRequest profilerRequest;
        private Project project;
        private URL url;
        private String panel;

        public UrlAction(Project project, ProfilerRequest profilerRequest, String statusCode) {
            super(String.format("(%s) %s", profilerRequest.getHash(), profilerRequest.getUrl()), "Open Url", Symfony2Icons.PROFILER_LINK);
            this.profilerRequest = profilerRequest;
            this.project = project;


            try {
                this.url = new URL(profilerRequest.getUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return;
            }

            Presentation presentation = getTemplatePresentation();
            presentation.setText(String.format("(%s) %s", statusCode == null ? "n/a" : statusCode, StringUtils.abbreviate(this.url.getPath(), 35)));

        }

        public UrlAction withPanel(String panel) {
            this.panel = panel;
            return this;
        }

        public UrlAction withIcon(Icon icon) {
            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(icon);
            return this;
        }

        @Override
        public void actionPerformed(AnActionEvent event) {

            if(this.url == null) {
                return;
            }

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {

                String portValue = "";
                int port = url.getPort();
                if(port != -1 && port != 80) {
                    portValue = ":" + port;
                }

                String urlProfiler = url.getProtocol() + "://" + url.getHost() + portValue + "/_profiler/" + profilerRequest.getHash();
                if(panel != null) {
                    urlProfiler = urlProfiler + "?panel=" + panel;
                }

                try {
                    desktop.browse(new URL(urlProfiler).toURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

}

