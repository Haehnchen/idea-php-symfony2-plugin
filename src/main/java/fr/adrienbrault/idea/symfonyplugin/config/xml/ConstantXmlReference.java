package fr.adrienbrault.idea.symfonyplugin.config.xml;

import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlText;
import fr.adrienbrault.idea.symfonyplugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ConstantXmlReference extends PsiPolyVariantReferenceBase<XmlText> {
    ConstantXmlReference(@NotNull XmlText element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String contents = getElement().getValue();
        if(StringUtils.isBlank(contents)) {
            return new ResolveResult[0];
        }

        return PsiElementResolveResult.createResults(
            ServiceContainerUtil.getTargetsForConstant(getElement().getProject(), contents)
        );
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}