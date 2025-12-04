package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * Intention action to add parameters to route action methods (e.g., Request).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteActionParameterIntention extends PsiElementBaseIntentionAction implements Iconable {

    /**
     * Map of FQN (without leading backslash) to variable name
     */
    private static final Map<String, String> AVAILABLE_PARAMETERS = new LinkedHashMap<>() {{
        put("Symfony\\Component\\HttpFoundation\\Request", "request");
        put("Symfony\\Component\\Security\\Core\\User\\UserInterface", "user");
    }};

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return;
        }

        List<String> availableFqns = getAvailableParameterFqns(method);
        if (availableFqns.isEmpty()) {
            return;
        }

        // Build display names: "ClassName (namespace)"
        List<String> displayNames = new ArrayList<>();
        Map<String, String> displayToFqn = new LinkedHashMap<>();
        for (String fqn : availableFqns) {
            String displayName = formatDisplayName(fqn);
            displayNames.add(displayName);
            displayToFqn.put(displayName, fqn);
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(displayNames)
            .setTitle("Symfony: Add Parameter to Route Action")
            .setItemChosenCallback(selectedDisplay -> WriteCommandAction.writeCommandAction(project)
                .withName("Add Route Action Parameter")
                .run(() -> {
                    String fqn = displayToFqn.get(selectedDisplay);
                    String variableName = AVAILABLE_PARAMETERS.get(fqn);
                    if (variableName != null && fqn != null) {
                        PhpElementsUtil.addParameterToMethod(method, "\\" + fqn, variableName);
                    }
                }))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    /**
     * Formats a FQN for display as "ClassName (namespace)"
     */
    @NotNull
    private static String formatDisplayName(@NotNull String fqn) {
        int lastBackslash = fqn.lastIndexOf('\\');
        if (lastBackslash == -1) {
            return fqn;
        }
        String className = fqn.substring(lastBackslash + 1);
        String namespace = fqn.substring(0, lastBackslash);
        return className + " (" + namespace + ")";
    }

    /**
     * Returns the list of FQNs (without leading backslash) that are available to be added to the given method.
     */
    @NotNull
    public static List<String> getAvailableParameterFqns(@NotNull Method method) {
        Set<String> existingParameterTypes = getExistingParameterTypes(method);

        List<String> availableFqns = new ArrayList<>();
        for (String fqn : AVAILABLE_PARAMETERS.keySet()) {
            if (!existingParameterTypes.contains("\\" + fqn)) {
                availableFqns.add(fqn);
            }
        }

        return availableFqns;
    }

    @NotNull
    private static Set<String> getExistingParameterTypes(@NotNull Method method) {
        Set<String> types = new HashSet<>();
        for (Parameter parameter : method.getParameters()) {
            types.addAll(parameter.getDeclaredType().getTypes());
        }
        return types;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false;
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return false;
        }

        if (method.getAccess() != PhpModifier.Access.PUBLIC) {
            return false;
        }

        if (!isMethodARouteAction(method)) {
            return false;
        }

        return !getAvailableParameterFqns(method).isEmpty();
    }

    /**
     * Checks if the method is a route action.
     * A method is a route action if:
     * - It has a Route attribute/annotation directly on the method, OR
     * - The method is __invoke and the class has a Route attribute/annotation
     */
    private static boolean isMethodARouteAction(@NotNull Method method) {
        // Check if method has Route attribute or annotation
        if (hasRouteAnnotationOrAttribute(method)) {
            return true;
        }

        // For __invoke methods, also check if class has Route attribute/annotation
        if ("__invoke".equals(method.getName())) {
            PhpClass phpClass = method.getContainingClass();
            if (phpClass != null && hasRouteAnnotationOrAttribute(phpClass)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasRouteAnnotationOrAttribute(@NotNull PhpAttributesOwner method) {
        // Check for Route attributes
        for (String route : RouteHelper.ROUTE_ANNOTATIONS) {
            if (!method.getAttributes(route).isEmpty()) {
                return true;
            }
        }

        // Check for Route annotations in PHPDoc
        if (method instanceof PhpNamedElement phpNamedElement) {
            PhpDocComment docComment = phpNamedElement.getDocComment();
            if (docComment != null) {
                return AnnotationUtil.getPhpDocCommentAnnotationContainer(docComment).getFirstPhpDocBlock(RouteHelper.ROUTE_ANNOTATIONS) != null;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony: Add parameter to route action";
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public Icon getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
