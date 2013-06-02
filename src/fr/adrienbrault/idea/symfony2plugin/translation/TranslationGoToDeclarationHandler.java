package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!PlatformPatterns.psiElement()
            .withParent(StringLiteralExpression.class).inside(ParameterList.class)
            .withLanguage(PhpLanguage.INSTANCE).accepts(psiElement)) {

            return new PsiElement[0];
        }

        ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);

        if (parameterList == null) {
            return new PsiElement[0];
        }

        if (!(parameterList.getContext() instanceof MethodReference)) {
            return new PsiElement[0];
        }

        MethodReference method = (MethodReference) parameterList.getContext();
        if (!new Symfony2InterfacesUtil().isTranslatorCall(method)) {
            return new PsiElement[0];
        }

        // only use parameter: 3 = domain
        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement.getParent());
        if(currentIndex == null || currentIndex.getIndex() != 2) {
            return new PsiElement[0];
        }

        String domainName = PsiElementUtils.getMethodParameterAt(parameterList, 2);
        return TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), domainName);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

}
