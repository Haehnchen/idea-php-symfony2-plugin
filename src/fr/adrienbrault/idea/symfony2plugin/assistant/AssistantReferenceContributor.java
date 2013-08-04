package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

public interface AssistantReferenceContributor {
    public boolean supportData();
    public String getAlias();
    public boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config);
}
