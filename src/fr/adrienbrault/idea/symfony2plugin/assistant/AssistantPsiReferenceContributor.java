package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AssistantPsiReferenceContributor {
    PsiReference[] getPsiReferences(StringLiteralExpression psiElement);
}
