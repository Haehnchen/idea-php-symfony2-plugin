package fr.adrienbrault.idea.symfony2plugin.profiler.widget;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.ui.popup.PopupFactoryImpl;
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

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProfilerWidget extends EditorBasedWidget implements StatusBarWidget.MultipleTextValuesPresentation,  StatusBarWidget.Multiframe {
    public static final String ID = "symfony2.profiler";

    public SymfonyProfilerWidget(@NotNull Project project) {
        super(project);
    }

    @Override
    public @NotNull StatusBarWidget copy() {
        return new SymfonyProfilerWidget(getProject());
    }

    //constructs the actions for the widget popup
    public DefaultActionGroup getActions(){
        DefaultActionGroup actionGroup = new DefaultActionGroup("Symfony.Profiler", false);

        ProfilerIndexInterface index = ProfilerFactoryUtil.createIndex(getProject());
        if(index == null) {
            return actionGroup;
        }

        List<ProfilerRequestInterface> requests = index.getRequests();

        Collection<AnAction> urlActions = new ArrayList<>();
        Collection<AnAction> mailActions = new ArrayList<>();

        Map<String, AnAction> knownFormTypes = new HashMap<>();
        Map<String, AnAction> knownController = new HashMap<>();
        Map<String, AnAction> knownRoutes = new HashMap<>();
        Map<String, AnAction> knownTemplates = new HashMap<>();

        requests.parallelStream().forEach(profilerRequest -> {
            urlActions.add(new SymfonyProfilerWidgetActions.UrlAction(index, profilerRequest));
            DefaultDataCollectorInterface collector = profilerRequest.getCollector(DefaultDataCollectorInterface.class);
            if (collector != null) {
                String route = collector.getRoute();
                if (route != null && !knownRoutes.containsKey(route)) {
                    knownRoutes.put(route, new SymfonyProfilerWidgetActions.RouteAction(getProject(), route));
                }

                String template = collector.getTemplate();
                if (template != null && !knownTemplates.containsKey(template)) {
                    knownTemplates.put(template, new SymfonyProfilerWidgetActions.TemplateAction(getProject(), template));
                }

                String controller = collector.getController();
                if (controller != null && !knownController.containsKey(controller)) {
                    knownController.put(controller, new SymfonyProfilerWidgetActions.MethodAction(getProject(), controller));
                }

                for (String formType : collector.getFormTypes()) {
                    if (knownFormTypes.containsKey(formType)) {
                        continue;
                    }

                    knownFormTypes.put(formType, new SymfonyProfilerWidgetActions.FormTypeAction(getProject(), formType));
                }
            }
            MailCollectorInterface collectorMail = profilerRequest.getCollector(MailCollectorInterface.class);
            if (collectorMail != null) {
                for (MailMessage message : collectorMail.getMessages()) {
                    String title = message.title();
                    if (title.isBlank()) {
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

    public @Nullable JBPopup getPopup() {
        if (isDisposed()) {
            return null;
        }

        ActionGroup popupGroup = getActions();

        DataContext dataContext = SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, getProject())
                .add(PlatformDataKeys.CONTEXT_COMPONENT, IdeFocusManager.getInstance(getProject()).getFocusOwner())
                .build();

        return new PopupFactoryImpl.ActionGroupPopup(
                "Symfony Profiler",
                popupGroup,
                dataContext,
                false,
                false,
                false,
                true,
                null,
                -1,
                null,
                null
        );
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String ID() {
        return ID;
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "Symfony Profiler";
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);

        myConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                statusBar.updateWidget(ID());
            }

            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                statusBar.updateWidget(ID());
            }
        });
    }
}
