package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ConstantXmlReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String value;

    ConstantXmlReference(@NotNull PsiElement element, @NotNull String value) {
        super(element);
        this.value = value;
    }

    @NotNull
    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(
            ServiceContainerUtil.getTargetsForConstant(getElement().getProject(), value)
        );
    }

    @NotNull
    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }
}