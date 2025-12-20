package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.RouteActionParameterIntention;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

/**
 * Handles popup actions for Symfony Route attributes
 */
public class RouteActionPopupHandler {

    private static final String IS_GRANTED_ATTRIBUTE_FQN = "\\Symfony\\Component\\Security\\Http\\Attribute\\IsGranted";

    public static void showPopup(@NotNull PhpAttribute attribute, @NotNull MouseEvent event, @NotNull Editor editor) {
        Project project = attribute.getProject();
        Method method = PsiTreeUtil.getParentOfType(attribute, Method.class);

        if (method == null) {
            return;
        }

        // Create action group
        DefaultActionGroup actionGroup = new DefaultActionGroup("Actions for URL", true);

        boolean added = false;

        // Add "Add Parameter to Action" if available
        if (!RouteActionParameterIntention.getAvailableParameterFqns(method).isEmpty()) {
            actionGroup.add(new AddParameterToActionAction(method, editor));
            added = true;
        }

        // Add "Add IsGranted Attribute" if not already present
        if (!hasIsGrantedAttribute(method)) {
            actionGroup.add(new AddIsGrantedAttributeAction(method, editor));
            added = true;
        }

        if (added) {
            actionGroup.addSeparator();
        }

        // Add "Generate Request in HTTP Client" action
        actionGroup.add(new GenerateHttpRequestAction(attribute, method));

        // Add "Copy Route Path" action
        actionGroup.add(new CopyRoutePathAction(attribute));

        // Add separator
        actionGroup.addSeparator();

        // Add "Disable Inlay Hints" action
        actionGroup.add(new DisableInlayHintsAction(project));

        // Create and show popup
        ListPopup popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(
                        "Actions",
                        actionGroup,
                        DataContext.EMPTY_CONTEXT,
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                        true
                );

        popup.showInBestPositionFor(DataManager.getInstance().getDataContext(event.getComponent()));
    }

    /**
     * Extracts the route path from the Route attribute
     */
    @Nullable
    private static String getRoutePath(@NotNull PhpAttribute attribute) {
        return PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "path");
    }

    /**
     * Checks if the method already has an IsGranted attribute
     */
    private static boolean hasIsGrantedAttribute(@NotNull Method method) {
        return !method.getAttributes(IS_GRANTED_ATTRIBUTE_FQN).isEmpty();
    }

    /**
     * Action to add a parameter to the route action
     */
    private static class AddParameterToActionAction extends AnAction {
        private final Method method;
        private final Editor editor;

        public AddParameterToActionAction(@NotNull Method method, @NotNull Editor editor) {
            super("Add Parameter to Action", "Add a parameter (Request, UserInterface, etc.) to this route action", Symfony2Icons.ROUTE);
            this.method = method;
            this.editor = editor;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            RouteActionParameterIntention.showParameterChooserPopup(method.getProject(), editor, method);
        }
    }

    /**
     * Action to add IsGranted attribute to the route action
     */
    private static class AddIsGrantedAttributeAction extends AnAction {
        private final Method method;
        private final Editor editor;

        public AddIsGrantedAttributeAction(@NotNull Method method, @NotNull Editor editor) {
            super("Add IsGranted Attribute", "Add #[IsGranted()] attribute to protect this route", Symfony2Icons.SYMFONY_ATTRIBUTE);
            this.method = method;
            this.editor = editor;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = method.getProject();

            WriteCommandAction.runWriteCommandAction(project, "Add IsGranted Attribute", null, () -> {
                var phpClass = method.getContainingClass();
                if (phpClass == null) {
                    return;
                }

                var document = PsiDocumentManager.getInstance(project).getDocument(method.getContainingFile());
                if (document == null) {
                    return;
                }

                PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

                // Add use statement if necessary
                String importedName = PhpElementsUtil.insertUseIfNecessary(phpClass, IS_GRANTED_ATTRIBUTE_FQN);
                if (importedName == null) {
                    importedName = "IsGranted";
                }

                psiDocManager.doPostponedOperationsAndUnblockDocument(document);

                // Insert attribute before method
                int methodStartOffset = method.getTextRange().getStartOffset();
                String attributeText = "#[" + importedName + "(\"\")]\n";

                document.insertString(methodStartOffset, attributeText);

                psiDocManager.commitDocument(document);
                psiDocManager.doPostponedOperationsAndUnblockDocument(document);

                // Reformat the added attribute
                CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);

                // Position cursor inside the quotes: #[IsGranted("<cursor>")]
                int cursorOffset = methodStartOffset + attributeText.indexOf("\"\"") + 1;
                editor.getCaretModel().moveToOffset(cursorOffset);
            });
        }
    }

    /**
     * Action to generate HTTP request in the HTTP Client
     */
    private static class GenerateHttpRequestAction extends AnAction {
        private final PhpAttribute attribute;
        private final Method method;

        public GenerateHttpRequestAction(@NotNull PhpAttribute attribute, @NotNull Method method) {
            super("Generate Request in HTTP Client", "Generate an HTTP request file for this route", Symfony2Icons.ROUTE);
            this.attribute = attribute;
            this.method = method;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = attribute.getProject();

            // Extract route path
            String routePath = getRoutePath(attribute);
            if (routePath == null) {
                routePath = "/";
            }

            // Extract HTTP method from Route attribute (defaulting to GET)
            String httpMethod = extractHttpMethod(attribute);

            // Generate the HTTP request content
            String requestContent = generateHttpRequestContent(httpMethod, routePath);

            // Create an HTTP request file (simplified version)
            RouteHttpClientGenerator.generateHttpRequest(project, method, requestContent);
        }

        @NotNull
        private String extractHttpMethod(@NotNull PhpAttribute attribute) {
            // Try to extract the methods parameter from Route attribute
            // This is a simplified version - in production, you'd parse the attribute properly
            String text = attribute.getText();
            if (text.contains("methods:") || text.contains("'methods'")) {
                if (text.contains("'POST'") || text.contains("\"POST\"")) {
                    return "POST";
                } else if (text.contains("'PUT'") || text.contains("\"PUT\"")) {
                    return "PUT";
                } else if (text.contains("'DELETE'") || text.contains("\"DELETE\"")) {
                    return "DELETE";
                } else if (text.contains("'PATCH'") || text.contains("\"PATCH\"")) {
                    return "PATCH";
                }
            }
            return "GET";
        }

        @NotNull
        private String generateHttpRequestContent(@NotNull String httpMethod, @NotNull String routePath) {
            return String.format("""
                    ### %s request to %s
                    %s http://localhost:8000%s
                    Accept: application/json

                    ###
                    """, httpMethod, routePath, httpMethod, routePath);
        }
    }

    /**
     * Action to copy route path to clipboard
     */
    private static class CopyRoutePathAction extends AnAction {
        private final PhpAttribute attribute;

        public CopyRoutePathAction(@NotNull PhpAttribute attribute) {
            super("Copy Route Path", "Copy the route path to clipboard", Symfony2Icons.ROUTE);
            this.attribute = attribute;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // Extract route path
            String routePath = getRoutePath(attribute);
            if (routePath == null) {
                routePath = "";
            }

            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(routePath);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

            if (e.getProject() != null) {
                com.intellij.notification.Notifications.Bus.notify(
                        new com.intellij.notification.Notification(
                                "Symfony Plugin",
                                "Route Path Copied",
                                "Copied to clipboard: " + routePath,
                                com.intellij.notification.NotificationType.INFORMATION
                        ),
                        e.getProject()
                );
            }
        }
    }

    /**
     * Action to disable route inlay hints
     */
    private static class DisableInlayHintsAction extends AnAction {
        private final Project project;

        public DisableInlayHintsAction(@NotNull Project project) {
            super("Disable Route Inlay Hints", "Disable inline route action hints", com.intellij.icons.AllIcons.Actions.Cancel);
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // Disable route inlay hints in settings
            fr.adrienbrault.idea.symfony2plugin.Settings settings =
                fr.adrienbrault.idea.symfony2plugin.Settings.getInstance(project);
            settings.routeInlayHintsEnabled = false;

            // Trigger a refresh of inlay hints by restarting code analysis
            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart();

            // Notify user
            com.intellij.notification.Notifications.Bus.notify(
                new com.intellij.notification.Notification(
                    "Symfony Plugin",
                    "Route Inlay Hints Disabled",
                    "Route inlay hints have been disabled. You can re-enable them in Settings > Languages & Frameworks > PHP > Symfony",
                    com.intellij.notification.NotificationType.INFORMATION
                ),
                project
            );
        }
    }
}
