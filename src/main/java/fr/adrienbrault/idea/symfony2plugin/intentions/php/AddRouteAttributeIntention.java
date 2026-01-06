package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collection;

/**
 * Intention action to add #[\Symfony\Component\Routing\Attribute\Route] attribute to a public method in a controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AddRouteAttributeIntention extends PsiElementBaseIntentionAction implements Iconable {

    private static final String ROUTE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\Routing\\Attribute\\Route";
    private static final String ABSTRACT_CONTROLLER_CLASS = "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController";
    private static final String AS_CONTROLLER_ATTRIBUTE = "\\Symfony\\Component\\HttpKernel\\Attribute\\AsController";

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if (method == null) {
            return;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return;
        }

        String routeName = AnnotationBackportUtil.getRouteByMethod(method);
        if (routeName == null) {
            routeName = "";
        }

        String routePath = AnnotationBackportUtil.getRoutePathByMethod(method);
        if (routePath == null) {
            routePath = "/";
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(method.getContainingFile());
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);

        String importedRouteName = PhpElementsUtil.insertUseIfNecessary(phpClass, ROUTE_ATTRIBUTE_CLASS);
        if (importedRouteName == null) {
            importedRouteName = "Route";
        }

        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = method.getTextRange().getStartOffset();
        String attributePrefix = "#[" + importedRouteName + "('";
        String attributeText = attributePrefix + routePath + "', name: '" + routeName + "')]\n";

        document.insertString(methodStartOffset, attributeText);

        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);

        // position caret after the opening quote of the path: #[Route('<caret>/...
        if (editor != null) {
            int caretOffset = methodStartOffset + attributePrefix.length();
            editor.getCaretModel().moveToOffset(caretOffset);
        }
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

        if (!method.getAccess().isPublic() || method.isStatic()) {
            return false;
        }

        PhpClass phpClass = method.getContainingClass();
        if (phpClass == null) {
            return false;
        }

        if (!PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_CLASS)) {
            return false;
        }

        if (hasRouteAttribute(method)) {
            return false;
        }

        return isControllerClass(phpClass);
    }

    private boolean hasRouteAttribute(@NotNull Method method) {
        for (String routeAnnotation : RouteHelper.ROUTE_ANNOTATIONS) {
            Collection<PhpAttribute> attributes = method.getAttributes(routeAnnotation);
            if (!attributes.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isControllerClass(@NotNull PhpClass phpClass) {
        if (phpClass.getName().endsWith("Controller")) {
            return true;
        }

        if (!phpClass.getAttributes(AS_CONTROLLER_ATTRIBUTE).isEmpty()) {
            return true;
        }

        if (!phpClass.getAttributes(ROUTE_ATTRIBUTE_CLASS).isEmpty()) {
            return true;
        }

        if (PhpElementsUtil.isInstanceOf(phpClass, ABSTRACT_CONTROLLER_CLASS)) {
            return true;
        }

        for (Method ownMethod : phpClass.getOwnMethods()) {
            if (!ownMethod.getAccess().isPublic() || ownMethod.isStatic()) {
                continue;
            }

            if (!ownMethod.getAttributes(ROUTE_ATTRIBUTE_CLASS).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony: Add Route attribute";
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
}
