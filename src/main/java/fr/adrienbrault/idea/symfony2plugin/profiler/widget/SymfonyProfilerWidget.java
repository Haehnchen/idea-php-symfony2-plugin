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
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.widget.action.SymfonyProfilerWidgetActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProfilerWidget extends EditorBasedWidget implements StatusBarWidget.MultipleTextValuesPresentation,  StatusBarWidget.Multiframe {
    public static String ID = "symfony2.profiler";

    public SymfonyProfilerWidget(@NotNull Project project) {
        super(project);
    }

    @Override
    public @NotNull StatusBarWidget copy() {
        return new SymfonyProfilerWidget(getProject());
    }

    private enum ProfilerTarget {
        TEMPLATE, ROUTE, CONTROLLER
    }

    //constructs the actions for the widget popup
    public DefaultActionGroup getActions(){
        DefaultActionGroup actionGroup = new DefaultActionGroup("Symfony.Profiler", false);

        ProfilerIndexInterface index = ProfilerFactoryUtil.createIndex(getProject());
        if(index == null) {
            return actionGroup;
        }

        List<ProfilerRequestInterface> requests = index.getRequests();

        Collection<AnAction> templateActions = new ArrayList<>();
        Map<String, Integer> templateActionsMap = new HashMap<>();

        Collection<AnAction> routeActions = new ArrayList<>();
        Map<String, Integer> routeActionsMap = new HashMap<>();

        Collection<AnAction> controllerActions = new ArrayList<>();
        Map<String, Integer> controllerActionsMap = new HashMap<>();

        Collection<AnAction> urlActions = new ArrayList<>();
        Collection<AnAction> mailActions = new ArrayList<>();

        for(ProfilerRequestInterface profilerRequest : requests) {
            urlActions.add(new SymfonyProfilerWidgetActions.UrlAction(index, profilerRequest));

            DefaultDataCollectorInterface collector = profilerRequest.getCollector(DefaultDataCollectorInterface.class);
            if(collector != null) {
                attachProfileItem(templateActions, templateActionsMap, collector.getTemplate(), ProfilerTarget.TEMPLATE);
                attachProfileItem(routeActions, routeActionsMap, collector.getRoute(), ProfilerTarget.ROUTE);
                attachProfileItem(controllerActions, controllerActionsMap, collector.getController(), ProfilerTarget.CONTROLLER);
            }

            // @TODO: use collector
            //String content = profilerRequest.getContent();
            //if(content != null && content.contains("Swift_Mime_Headers_MailboxHeader")) {
            //    mailActions.add(new SymfonyProfilerWidgetActions.UrlAction(getProject(), profilerRequest, statusCode).withPanel("swiftmailer").withIcon(Symfony2Icons.MAIL));
            //}
        }

        // routes
        if(urlActions.size() > 0) {
            actionGroup.addSeparator("Debug-Url");
            actionGroup.addAll(urlActions);
        }

        // mails send by request
        if(mailActions.size() > 0) {
            actionGroup.addSeparator("E-Mail");
            actionGroup.addAll(mailActions);
        }

        // routes
        if(routeActions.size() > 0) {
            actionGroup.addSeparator("Routes");
            actionGroup.addAll(routeActions);
        }

        // controller methods
        if(controllerActions.size() > 0) {
            actionGroup.addSeparator("Controller");
            actionGroup.addAll(controllerActions);
        }

        // template should be most use case; so keep it in cursor range
        if(templateActions.size() > 0) {
            actionGroup.addSeparator("Template");
            actionGroup.addAll(templateActions);
        }

        return actionGroup;
    }

    private void attachProfileItem(Collection<AnAction> controllerActions, Map<String, Integer> controllerActionsMap, @Nullable String collectString, ProfilerTarget profilerTarget) {
        if(collectString == null) {
            return;
        }

        if(controllerActionsMap.containsKey(collectString)) {
            controllerActionsMap.put(collectString, controllerActionsMap.get(collectString));
        } else {
            controllerActionsMap.put(collectString, 0);

            if(profilerTarget == ProfilerTarget.CONTROLLER) {
                controllerActions.add(new SymfonyProfilerWidgetActions.MethodAction(getProject(), collectString));
            }

            if(profilerTarget == ProfilerTarget.ROUTE) {
                controllerActions.add(new SymfonyProfilerWidgetActions.RouteAction(getProject(), collectString));
            }

            if(profilerTarget == ProfilerTarget.TEMPLATE) {
                controllerActions.add(new SymfonyProfilerWidgetActions.TemplateAction(getProject(), collectString));
            }
        }
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
