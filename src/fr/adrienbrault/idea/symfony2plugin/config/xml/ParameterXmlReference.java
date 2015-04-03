package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlText;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.ParameterPercentWrapInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return PhpResolveResult.create(ServiceUtil.getServiceClassTargets(getElement().getProject(), this.parameterName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
