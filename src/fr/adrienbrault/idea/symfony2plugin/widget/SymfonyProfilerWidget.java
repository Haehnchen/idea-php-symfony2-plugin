package fr.adrienbrault.idea.symfony2plugin.widget;

import com.intellij.openapi.actionSystem.*;
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
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndex;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.DefaultDataCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.MailMessage;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequest;
import fr.adrienbrault.idea.symfony2plugin.widget.action.SymfonyProfilerWidgetActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

public class SymfonyProfilerWidget extends EditorBasedWidget implements StatusBarWidget.MultipleTextValuesPresentation,  StatusBarWidget.Multiframe {

    private Project project;

    public static String ID = "symfony2.profiler";

    public SymfonyProfilerWidget(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public StatusBarWidget copy() {
        return new SymfonyProfilerWidget(this.project);
    }

    private enum ProfilerTarget {
        TEMPLATE, ROUTE, CONTROLLER
    }

    //constructs the actions for the widget popup
    public ActionGroup getActions(){

        DefaultActionGroup actionGroup = new DefaultActionGroup(null, false);
        File profilerCsv = ProfilerUtil.findProfilerCsv(project);
        if(profilerCsv == null) {
            return actionGroup;
        }

        ProfilerIndex profilerIndex = new ProfilerIndex(profilerCsv);

        List<ProfilerRequest> requests = profilerIndex.getRequests();
        Collections.reverse(requests);

        if(requests.size() > 10) {
            requests = requests.subList(0, 10);
        }

        Collection<AnAction> templateActions = new ArrayList<AnAction>();
        Map<String, Integer> templateActionsMap = new HashMap<String, Integer>();

        Collection<AnAction> routeActions = new ArrayList<AnAction>();
        Map<String, Integer> routeActionsMap = new HashMap<String, Integer>();

        Collection<AnAction> controllerActions = new ArrayList<AnAction>();
        Map<String, Integer> controllerActionsMap = new HashMap<String, Integer>();

        Collection<AnAction> urlActions = new ArrayList<AnAction>();

        Collection<AnAction> mailActions = new ArrayList<AnAction>();

        for(ProfilerRequest profilerRequest : requests) {
            DefaultDataCollector collector = profilerRequest.getCollector(DefaultDataCollector.class);

            String statusCode = collector.getStatusCode();
            urlActions.add(new SymfonyProfilerWidgetActions.UrlAction(this.project, profilerRequest, statusCode));

            // regular expression fails on current version (because of multiple mailer)
            // ArrayList<MailMessage> messages = profilerRequest.getCollector(MailCollector.class).getMessages();

            // @TODO: use collector
            String content = profilerRequest.getContent();
            if(content != null && content.contains("Swift_Mime_Headers_MailboxHeader")) {
                mailActions.add(new SymfonyProfilerWidgetActions.UrlAction(this.project, profilerRequest, statusCode).withPanel("swiftmailer").withIcon(Symfony2Icons.MAIL));
            }

            attachProfileItem(templateActions, templateActionsMap, collector.getTemplate(), ProfilerTarget.TEMPLATE);
            attachProfileItem(routeActions, routeActionsMap, collector.getRoute(), ProfilerTarget.ROUTE);
            attachProfileItem(controllerActions, controllerActionsMap, collector.getController(), ProfilerTarget.CONTROLLER);


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
                controllerActions.add(new SymfonyProfilerWidgetActions.MethodAction(project, collectString));
            }

            if(profilerTarget == ProfilerTarget.ROUTE) {
                controllerActions.add(new SymfonyProfilerWidgetActions.RouteAction(project, collectString));
            }

            if(profilerTarget == ProfilerTarget.TEMPLATE) {
                controllerActions.add(new SymfonyProfilerWidgetActions.TemplateAction(project, collectString));
            }

        }
    }

    @Nullable
    @Override
    public ListPopup getPopupStep() {

        ActionGroup popupGroup = getActions();
        ListPopup listPopup = new PopupFactoryImpl.ActionGroupPopup("Symfony2 Profiler", popupGroup, SimpleDataContext.getProjectContext(project), false, false, false, true, null, -1, null, null);

        return listPopup;


    }

    @Nullable
    @Override
    public String getSelectedValue() {
        return "Symfony2";
    }

    @NotNull
    @Override
    public String getMaxValue() {
        return null;
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
        return "Symfony2 Profiler";
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
        this.project = project;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if ((project == null) || project.isDisposed()) {
                    return;
                }

                if (!isDisposed() && myStatusBar != null) {
                    myStatusBar.updateWidget(ID());
                }

            }
        });
    }


}
