package fr.adrienbrault.idea.symfonyplugin.assistant.reference;

import com.intellij.psi.PsiReference;
import fr.adrienbrault.idea.symfonyplugin.assistant.AssistantReferenceProvider;
import fr.adrienbrault.idea.symfonyplugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfonyplugin.config.component.ParameterReference;
import fr.adrienbrault.idea.symfonyplugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfonyplugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfonyplugin.form.FormDefaultOptionsKeyReference;
import fr.adrienbrault.idea.symfonyplugin.form.FormTypeReference;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteReference;
import fr.adrienbrault.idea.symfonyplugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfonyplugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfonyplugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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
        new ParameterReferenceProvider(),
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

    private static class ParameterReferenceProvider implements AssistantReferenceProvider {

        @Override
        public PsiReference getPsiReference(AssistantReferenceProviderParameter parameter) {
            return new ParameterReference(parameter.getPsiElement());
        }

        @Override
        public String getAlias() {
            return "parameter";
        }

        @Override
        public String getDocBlockParamAlias() {
            return "Parameter";
        }
    }

}
