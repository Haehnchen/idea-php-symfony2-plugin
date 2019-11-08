package fr.adrienbrault.idea.symfonyplugin.profiler.widget;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Consumer;
import fr.adrienbrault.idea.symfonyplugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfonyplugin.profiler.collector.DefaultDataCollectorInterface;
import fr.adrienbrault.idea.symfonyplugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfonyplugin.profiler.factory.ProfilerFactoryUtil;
import fr.adrienbrault.idea.symfonyplugin.profiler.widget.action.SymfonyProfilerWidgetActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
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
    public StatusBarWidget copy() {
        return new SymfonyProfilerWidget(getProject());
    }

    private enum ProfilerTarget {
        TEMPLATE, ROUTE, CONTROLLER
    }

    //constructs the actions for the widget popup
    public DefaultActionGroup getActions(){
        DefaultActionGroup actionGroup = new DefaultActionGroup(null, false);

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

    @Nullable
    @Override
    public ListPopup getPopupStep() {
        ActionGroup popupGroup = getActions();
        return new PopupFactoryImpl.ActionGroupPopup("Symfony Profiler", popupGroup, SimpleDataContext.getProjectContext(getProject()), false, false, false, true, null, -1, null, null);
    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getMaxValue() {
        return "";
    }

    @NotNull
    @Override
    public String ID() {
        return ID;
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType platformType) {
        return this;
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "Symfony Profiler";
    }

    @Nullable
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        update(event.getManager().getProject());
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        update(source.getProject());
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        update(source.getProject());
    }

    public void update(final Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if ((getProject() == null) || getProject().isDisposed()) {
                return;
            }

            if (!isDisposed() && myStatusBar != null) {
                myStatusBar.updateWidget(ID());
            }
        });
    }
}
