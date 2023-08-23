package fr.adrienbrault.idea.symfony2plugin.profiler.widget.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProfilerWidgetActions {

    public static class TemplateAction extends AnAction {
        private final String templateName;
        private final Project project;

        public TemplateAction(Project project, @NotNull String templateName) {
            super(templateName, "Open Template", TwigIcons.TwigFileIcon);
            this.templateName = templateName;
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Collection<PsiFile> psiFiles = TwigUtil.getTemplatePsiElements(project, templateName);

            // @TODO: multiple targets?
            if(!psiFiles.isEmpty()) {
                IdeHelper.navigateToPsiElement(psiFiles.iterator().next());
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

        @NotNull
        private final ProfilerIndexInterface profilerIndex;

        @NotNull
        private final ProfilerRequestInterface profilerRequest;

        private String panel;

        public UrlAction(@NotNull ProfilerIndexInterface profilerIndex, @NotNull ProfilerRequestInterface profilerRequest) {
            super(String.format("(%s) %s", profilerRequest.getHash(), profilerRequest.getUrl()), "Open Url", Symfony2Icons.PROFILER_LINK);
            this.profilerIndex = profilerIndex;
            this.profilerRequest = profilerRequest;

            Presentation presentation = getTemplatePresentation();
            presentation.setText(ProfilerUtil.formatProfilerRow(profilerRequest));
        }

        public UrlAction(@NotNull ProfilerIndexInterface profilerIndex, @NotNull ProfilerRequestInterface profilerRequest, @NotNull String panel) {
            this(profilerIndex, profilerRequest);
            this.panel = panel;
        }

        public UrlAction withIcon(Icon icon) {
            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(icon);
            return this;
        }

        public UrlAction withText(String text) {
            Presentation presentation = getTemplatePresentation();
            presentation.setText(text);

            return this;
        }

        @Override
        public void actionPerformed(AnActionEvent event) {
            String urlForRequest = profilerIndex.getUrlForRequest(profilerRequest);
            if(urlForRequest == null) {
                return;
            }

            if (this.panel != null) {
                urlForRequest += "?panel=" + this.panel;
            }

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop == null || !desktop.isSupported(Desktop.Action.BROWSE)) {
                return;
            }

            try {
                desktop.browse(new URL(urlForRequest).toURI());
            } catch (Exception ignored) {
            }
        }
    }
}

