package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerReference extends PsiReferenceBase<PsiElement> implements PsiReference {
    private final Project project;
    private final StringLiteralExpression element;

    public ControllerReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.element = element;
        this.project = element.getProject();
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        // @TODO: multiresolve

        String contents = element.getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(this.project, contents);
        if(methods.length > 0) {
            return methods[0];
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return ControllerIndex.getControllerLookupElements(this.project).toArray();
    }
}
