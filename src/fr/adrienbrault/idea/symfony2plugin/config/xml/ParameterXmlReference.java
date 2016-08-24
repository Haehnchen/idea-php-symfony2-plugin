package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlText;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

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
        return PhpResolveResult.createResults(ServiceUtil.getServiceClassTargets(getElement().getProject(), this.parameterName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
