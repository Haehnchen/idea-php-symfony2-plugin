package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.form.FormDefaultOptionsKeyReference;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeReference;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

public class DefaultReferenceProvider {

    public static AssistantReferenceProvider[] DEFAULT_PROVIDERS = new AssistantReferenceProvider[] {
        new RouteReferenceProvider(),
        new RepositoryReferenceProvider(),
        new TemplateProvider(),
        new TranslationDomainReferenceProvider(),
        new PhpClassReferenceProvider(),
        new ServiceReferenceProvider(),
        new FormTypeReferenceProvider(),
        new FormOptionReferenceProvider(),
        new PhpInterfaceReferenceProvider(),
        new PhpClassInterfaceReferenceProvider(),
        new TranslationKeyReferenceProvider(),
    };

    public enum DEFAULT_PROVIDER_ENUM {
        ROUTE {
            public String toString() {
                return "route";
            }
        },
        ENTITY {
            public String toString() {
                return "entity";
            }
        },
        TEMPLATE {
            public String toString() {
                return "template";
            }
        },
        TRANSLATION_DOMAIN {
            public String toString() {
                return "translation_domain";
            }
        },
        CLASS {
            public String toString() {
                return "class";
            }
        },
        SERVICE {
            public String toString() {
                return "service";
            }
        },
        FORM_TYPE {
            public String toString() {
                return "form_type";
            }
        },
        FORM_OPTION {
            public String toString() {
                return "form_option";
            }
        }
    }

    private static class RouteReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new RouteReference(parameter.getPsiElement());
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
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new EntityReference(parameter.getPsiElement());
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
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new TemplateReference(parameter.getPsiElement());
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
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new TranslationDomainReference(parameter.getPsiElement());
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

    private static class TranslationKeyReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {

            String translationDomain = "messages";

            // more than self match; search for translation_domain provider
            if(parameter.getConfigsMethodScope().size() > 1) {

                // last translation domain wins
                for(MethodParameterSetting config: parameter.getConfigsMethodScope()) {
                    if(config.getReferenceProviderName().equals("translation_domain")) {
                        String parameterValue = PsiElementUtils.getMethodParameterAt(parameter.getMethodReference(), config.getIndexParameter());
                        if(parameterValue != null) {
                            translationDomain = parameterValue;
                        }
                    }

                }

            }

            return new TranslationReference(parameter.getPsiElement(), translationDomain);
        }

        @Override
        public String getAlias() {
            return "translation_key";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "TranslationKey";
        }
    }

    private static class PhpClassReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new PhpClassReference(parameter.getPsiElement(), true);
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

    private static class PhpInterfaceReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new PhpClassReference(parameter.getPsiElement(), true).setUseInterfaces(true).setUseClasses(false);
        }

        @Override
        public String getAlias() {
            return "interface";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Interface";
        }
    }

    private static class PhpClassInterfaceReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new PhpClassReference(parameter.getPsiElement(), true).setUseInterfaces(true).setUseClasses(true);
        }

        @Override
        public String getAlias() {
            return "class_interface";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "ClassInterface";
        }
    }

    private static class ServiceReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new ServiceReference(parameter.getPsiElement());
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

    private static class FormTypeReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new FormTypeReference(parameter.getPsiElement());
        }

        @Override
        public String getAlias() {
            return "form_type";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "FormType";
        }
    }

    private static class FormOptionReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new FormDefaultOptionsKeyReference(parameter.getPsiElement(), "form");
        }

        @Override
        public String getAlias() {
            return "form_option";
        }

        @Override
        public String getDocBlockParamAlias() {
            return null;
        }
    }

}
