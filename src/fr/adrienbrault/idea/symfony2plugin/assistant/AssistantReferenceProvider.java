package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

public interface AssistantReferenceProvider {
    public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting);
    public String getAlias();
    public String getDocBlockParamAlias();
}
