package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerActionGotoRelatedCollectorParameter {

    private final Collection<GotoRelatedItem> relatedItems;
    private final Method method;
    private final Project project;
    private PsiElement[] parameterLists = null;

    public ControllerActionGotoRelatedCollectorParameter(@NotNull Method method, @NotNull Collection<GotoRelatedItem> relatedItems) {
        this.relatedItems = relatedItems;
        this.method = method;
        this.project = method.getProject();
    }

    public Method getMethod() {
        return method;
    }

    public Collection<GotoRelatedItem> getRelatedItems() {
        return relatedItems;
    }

    public Project getProject() {
        return this.project;
    }

    public void add(GotoRelatedItem relatedItem) {
        this.relatedItems.add(relatedItem);
    }

    public void addAll(Collection<GotoRelatedItem> relatedItems) {
        this.relatedItems.addAll(relatedItems);
    }

    public PsiElement[] getParameterLists() {

        if(parameterLists != null) {
            return parameterLists;
        }

        return parameterLists = PsiTreeUtil.collectElements(method, psiElement ->
            psiElement.getParent() instanceof ParameterList
        );

    }

}
