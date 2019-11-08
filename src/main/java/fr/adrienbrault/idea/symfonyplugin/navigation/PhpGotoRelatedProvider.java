package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ControllerMethodLineMarkerProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpGotoRelatedProvider extends GotoRelatedProvider {

    @NotNull
    @Override
    public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement psiElement) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return Collections.emptyList();
        }

        if(psiElement.getLanguage() != PhpLanguage.INSTANCE) {
            return Collections.emptyList();
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if(method == null || !method.getName().endsWith("Action")) {
            return Collections.emptyList();
        }

        return ControllerMethodLineMarkerProvider.getGotoRelatedItems(method);
    }

}
