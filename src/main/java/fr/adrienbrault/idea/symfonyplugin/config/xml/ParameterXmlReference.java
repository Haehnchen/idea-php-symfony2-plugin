package fr.adrienbrault.idea.symfonyplugin.config.xml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlText;
import fr.adrienbrault.idea.symfonyplugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterXmlReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String parameterName;

    public ParameterXmlReference(@NotNull XmlText element) {
        super(element);
        parameterName = element.getValue();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Collection<PsiElement> targets = new ArrayList<>();

        targets.addAll(
            DotEnvUtil.getEnvironmentVariableTargetsForParameter(getElement().getProject(), this.parameterName)
        );

        targets.addAll(ServiceUtil.getServiceClassTargets(getElement().getProject(), this.parameterName));

        return PsiElementResolveResult.createResults(targets);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
