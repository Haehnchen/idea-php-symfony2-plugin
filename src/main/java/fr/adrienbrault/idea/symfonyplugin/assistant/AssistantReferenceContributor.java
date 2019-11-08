package fr.adrienbrault.idea.symfonyplugin.assistant;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.assistant.reference.MethodParameterSetting;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AssistantReferenceContributor {
    boolean supportData();
    String getAlias();
    boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config);
}
