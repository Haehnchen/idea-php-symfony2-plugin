package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLScalar;

public class ConstantYamlReference extends PsiPolyVariantReferenceBase<YAMLScalar> {

    public ConstantYamlReference(@NotNull YAMLScalar element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        var constantName = getElement().getTextValue();
        if (StringUtils.isBlank(constantName)) {
            return ResolveResult.EMPTY_ARRAY;
        }

        return PsiElementResolveResult.createResults(
            ServiceContainerUtil.getTargetsForConstant(getElement().getProject(), constantName)
        );
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        var constantName = getValue();
        if (isClassConst(constantName)) {
            newElementName = getClassName(constantName) + "::" + newElementName;
        }

        return super.handleElementRename(newElementName);
    }

    private boolean isClassConst(@NotNull String value) {
        return value.contains("::");
    }

    @NotNull
    private String getClassName(@NotNull String value) {
        return value.substring(0, value.indexOf("::"));
    }
}
