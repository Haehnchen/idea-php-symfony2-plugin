package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

public interface AssistantPsiReferenceContributor {
    public PsiReference[] getPsiReferences(StringLiteralExpression psiElement);
}
