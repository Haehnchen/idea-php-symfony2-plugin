package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterXmlReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String parameterName;

    public ParameterXmlReference(@NotNull PsiElement psiElement, @NotNull String parameterName) {
        super(psiElement);
        this.parameterName = parameterName;
    }

    @NotNull
    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        Collection<PsiElement> targets = new ArrayList<>();

        targets.addAll(
            DotEnvUtil.getEnvironmentVariableTargetsForParameter(getElement().getProject(), this.parameterName)
        );

        targets.addAll(ServiceUtil.getServiceClassTargets(getElement().getProject(), this.parameterName));

        return PsiElementResolveResult.createResults(targets);
    }

    @NotNull
    @Override
    public Object @NotNull [] getVariants() {
        return new Object[0];
    }
}
