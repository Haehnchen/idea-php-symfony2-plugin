package fr.adrienbrault.idea.symfony2plugin.profiler.widget;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.MailCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.widget.action.SymfonyProfilerWidgetActions;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProfilerWidget extends EditorBasedStatusBarPopup {
    public static final String ID = "symfony2.profiler";

    public SymfonyProfilerWidget(@NotNull Project project) {
        super(project, false);
    }

    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        return new WidgetState("Symfony Profiler", "Symfony", true);
    }

    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new SymfonyProfilerWidget(project);
    }

    @Override
    protected @Nullable ListPopup createPopup(@NotNull DataContext context) {
        ActionGroup actionGroup = getActions();
        return JBPopupFactory.getInstance()
                .createActionGroupPopup("Symfony Profiler", actionGroup, context,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);
    }

    //constructs the actions for the widget popup
    private DefaultActionGroup getActions() {
        ProfilerIndexInterface index = ProfilerFactoryUtil.createIndex(getProject());
        if(index == null) {
            return new DefaultActionGroup("Symfony.Profiler", false);
        }

        return buildActions(getProject(), index, index.getRequests());
    }

    static DefaultActionGroup buildActions(@NotNull Project project, @NotNull ProfilerIndexInterface index, @NotNull Collection<ProfilerRequestInterface> requests) {
        DefaultActionGroup actionGroup = new DefaultActionGroup("Symfony.Profiler", false);

        Collection<AnAction> urlActions = new ConcurrentLinkedQueue<>();
        Collection<AnAction> mailActions = new ConcurrentLinkedQueue<>();

        Map<String, AnAction> knownFormTypes = new ConcurrentHashMap<>();
        Map<String, AnAction> knownController = new ConcurrentHashMap<>();
        Map<String, AnAction> knownRoutes = new ConcurrentHashMap<>();
        Map<String, AnAction> knownTemplates = new ConcurrentHashMap<>();

        requests.parallelStream().forEach(profilerRequest -> {
            urlActions.add(new SymfonyProfilerWidgetActions.UrlAction(index, profilerRequest));
            DefaultDataCollectorInterface collector = profilerRequest.getCollector(DefaultDataCollectorInterface.class);
            if (collector != null) {
                String route = collector.getRoute();
                if (route != null) {
                    knownRoutes.putIfAbsent(route, new SymfonyProfilerWidgetActions.RouteAction(project, route));
                }

                String template = collector.getTemplate();
                if (template != null) {
                    knownTemplates.putIfAbsent(template, new SymfonyProfilerWidgetActions.TemplateAction(project, template));
                }

                String controller = collector.getController();
                if (controller != null) {
                    knownController.putIfAbsent(controller, new SymfonyProfilerWidgetActions.MethodAction(project, controller));
                }

                Collection<String> formTypes = collector.getFormTypes();
                if (formTypes != null) {
                    for (String formType : formTypes) {
                        if (formType == null || knownFormTypes.containsKey(formType)) {
                            continue;
                        }

                        knownFormTypes.putIfAbsent(formType, new SymfonyProfilerWidgetActions.FormTypeAction(project, formType));
                    }
                }
            }
            MailCollectorInterface collectorMail = profilerRequest.getCollector(MailCollectorInterface.class);
            if (collectorMail != null) {
                for (MailMessage message : collectorMail.getMessages()) {
                    String title = message.title();
                    if (title != null && !title.isBlank()) {
                        mailActions.add(new SymfonyProfilerWidgetActions.UrlAction(index, profilerRequest, message.panel())
                            .withIcon(Symfony2Icons.MAIL)
                            .withText(String.format("(%s) %s", profilerRequest.getHash(), StringUtils.abbreviate(title, 40))));
                    }
                }
            }
        });

        // routes
        if(!urlActions.isEmpty()) {
            actionGroup.addSeparator("Debug-Url");
            actionGroup.addAll(urlActions);
        }

        // mails send by request
        if(!mailActions.isEmpty()) {
            actionGroup.addSeparator("E-Mail");
            actionGroup.addAll(mailActions);
        }

        // form types
        if(!knownFormTypes.isEmpty()) {
            actionGroup.addSeparator("Forms");
            actionGroup.addAll(knownFormTypes.values());
        }

        // routes
        if(!knownRoutes.isEmpty()) {
            actionGroup.addSeparator("Routes");
            actionGroup.addAll(knownRoutes.values());
        }

        // controller methods
        if(!knownController.isEmpty()) {
            actionGroup.addSeparator("Controller");
            actionGroup.addAll(knownController.values());
        }

        // template should be most use case; so keep it in cursor range
        if(!knownTemplates.isEmpty()) {
            actionGroup.addSeparator("Template");
            actionGroup.addAll(knownTemplates.values());
        }

        return actionGroup;
    }

    @NotNull
    @Override
    public String ID() {
        return ID;
    }
}
