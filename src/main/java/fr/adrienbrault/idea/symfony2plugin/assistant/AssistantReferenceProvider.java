package fr.adrienbrault.idea.symfony2plugin.assistant;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AssistantReferenceProvider {
    PsiReference getPsiReference(AssistantReferenceProviderParameter parameter);
    String getAlias();
    String getDocBlockParamAlias();

    class AssistantReferenceProviderParameter {

        private final StringLiteralExpression psiElement;
        private final MethodParameterSetting methodParameterSetting;
        private final List<MethodParameterSetting> configsMethodScope;
        private final MethodReference methodReference;

        public AssistantReferenceProviderParameter(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting, List<MethodParameterSetting> configsMethodScope, MethodReference methodReference) {
            this.psiElement = psiElement;
            this.methodParameterSetting = methodParameterSetting;
            this.configsMethodScope = configsMethodScope;
            this.methodReference = methodReference;
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

        public MethodReference getMethodReference() {
            return methodReference;
        }

    }
}
