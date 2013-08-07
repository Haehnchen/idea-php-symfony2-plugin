package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;

public class DefaultReferenceProvider {

    public static AssistantReferenceProvider[] DEFAULT_PROVIDERS = new AssistantReferenceProvider[] {
        new RouteReferenceProvider(),
        new RepositoryReferenceProvider(),
        new TemplateProvider(),
        new TranslationDomainReferenceProvider(),
        new PhpClassReferenceProvider(),
        new ServiceReferenceProvider(),
    };

    private static class RouteReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new RouteReference(psiElement);
        }

        @Override
        public String getAlias() {
            return "route";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Route";
        }
    }

    private static class RepositoryReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new EntityReference(psiElement);
        }

        @Override
        public String getAlias() {
            return "entity";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Entity";
        }
    }

    private static class TemplateProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new TemplateReference(psiElement);
        }

        @Override
        public String getAlias() {
            return "template";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Template";
        }
    }

    private static class TranslationDomainReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new TranslationDomainReference(psiElement);
        }

        @Override
        public String getAlias() {
            return "translation_domain";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "TranslationDomain";
        }
    }

    private static class PhpClassReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new PhpClassReference(psiElement, true);
        }

        @Override
        public String getAlias() {
            return "class";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Class";
        }
    }

    private static class ServiceReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(StringLiteralExpression psiElement, MethodParameterSetting methodParameterSetting) {
            return new ServiceReference(psiElement);
        }

        @Override
        public String getAlias() {
            return "service";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Service";
        }
    }
}
