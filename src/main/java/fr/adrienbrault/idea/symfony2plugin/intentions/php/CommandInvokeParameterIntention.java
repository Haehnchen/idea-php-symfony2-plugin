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
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * Intention action to add parameters to the __invoke method of an invokable Symfony Command.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CommandInvokeParameterIntention extends PsiElementBaseIntentionAction implements Iconable {

    private static final String AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";

    /**
     * Map of FQN (without leading backslash) to variable name
     */
    private static final Map<String, String> AVAILABLE_PARAMETERS = new LinkedHashMap<>() {{
        put("Symfony\\Component\\Console\\Input\\InputInterface", "input");
        put("Symfony\\Component\\Console\\Output\\OutputInterface", "output");
        put("Symfony\\Component\\Console\\Cursor", "cursor");
        put("Symfony\\Component\\Console\\Style\\SymfonyStyle", "io");
        put("Symfony\\Component\\Console\\Application", "application");
    }};

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return;
        }

        List<String> availableFqns = getAvailableParameterFqns(invokeMethod);
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
            .setTitle("Symfony: Add Parameter to __invoke")
            .setItemChosenCallback(selectedDisplay -> WriteCommandAction.writeCommandAction(project)
                .withName("Add __invoke Parameter")
                .run(() -> {
                    String fqn = displayToFqn.get(selectedDisplay);
                    String variableName = AVAILABLE_PARAMETERS.get(fqn);
                    if (variableName != null && fqn != null) {
                        PhpElementsUtil.addParameterToMethod(invokeMethod, "\\" + fqn, variableName);
                    }
                }))
            .createPopup()
            .showInBestPositionFor(editor);
    }

    /**
     * Formats a FQN for display as "ClassName (namespace)"
     * e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle" -> "SymfonyStyle (Symfony\\Component\\Console\\Style)"
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
     * Filters out parameters whose types are already present in the method signature.
     *
     * @param method The method to check
     * @return List of available FQNs (e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle")
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

        PhpClass phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass == null) {
            return false;
        }

        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false;
        }

        if (phpClass.getAttributes(AS_COMMAND_ATTRIBUTE).isEmpty()) {
            return false;
        }

        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod == null) {
            return false;
        }

        return !getAvailableParameterFqns(invokeMethod).isEmpty();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony: Add parameter to __invoke";
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
