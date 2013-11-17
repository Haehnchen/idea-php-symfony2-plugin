package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import java.util.List;

public interface AssistantReferenceProvider {
    public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter);
    public String getAlias();
    public String getDocBlockParamAlias();

    public class AssistantReferenceProviderParameter {

        private StringLiteralExpression psiElement;
        private MethodParameterSetting methodParameterSetting;
        private List<MethodParameterSetting> configsMethodScope;

        public AssistantReferenceProviderParameter(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting, List<MethodParameterSetting> configsMethodScope) {
            this.psiElement = psiElement;
            this.methodParameterSetting = methodParameterSetting;
            this.configsMethodScope = configsMethodScope;
        }

        public List<MethodParameterSetting> getConfigsMethodScope() {
            return configsMethodScope;
        }

        public StringLiteralExpression getPsiElement() {
            return psiElement;
        }

        public MethodParameterSetting getMethodParameterSetting() {
            return methodParameterSetting;
        }

    }
}
