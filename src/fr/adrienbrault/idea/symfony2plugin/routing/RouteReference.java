package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class RouteReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private String routeName;

    public RouteReference(@NotNull StringLiteralExpression element) {
        super(element);

        routeName = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Collection<PhpClass> classes = PhpIndex.getInstance(getElement().getProject()).getClassesByFQN("\\UrlGenerator");

        return new Object[] {
            "route1",
            "route2",
            "route3",
        };
    }
}
