package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import org.jetbrains.annotations.NotNull;


public class DocHashTagReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    DocTagHashing docTagHashing = new DocTagHashing(psiElement);
                    if(!docTagHashing.isValid()) {
                        return new PsiReference[0];
                    }

                    PhpDocParamTag phpDocParamTag = docTagHashing.getPhpDocParamTag();

                    if(phpDocParamTag.getTagValue().contains("#Entity")) {
                        return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#Service")) {
                        return new PsiReference[]{ new ServiceReference((StringLiteralExpression) psiElement)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#TranslationDomain")) {
                        return new PsiReference[]{ new TranslationDomainReference((StringLiteralExpression) psiElement)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#Template")) {
                        return new PsiReference[]{ new TemplateReference((StringLiteralExpression) psiElement)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#Route")) {
                        return new PsiReference[]{ new RouteReference((StringLiteralExpression) psiElement)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#Class")) {
                        return new PsiReference[]{ new PhpClassReference((StringLiteralExpression) psiElement, true)};
                    }

                    if(phpDocParamTag.getTagValue().contains("#TranslationKey")) {

                        String filterDomain = "messages";

                        int domainKey = getMatchingTranslationDomain(docTagHashing.getParameters());
                        if(domainKey >= 0) {
                            String keyParameter = PsiElementUtils.getMethodParameterAt(docTagHashing.getParameterList(), domainKey);
                            if(keyParameter != null) {
                                filterDomain = keyParameter;
                            }
                        }

                        return new PsiReference[]{ new TranslationReference((StringLiteralExpression) psiElement, filterDomain)};
                    }

                    return new PsiReference[0];

                }

                private int getMatchingTranslationDomain(Parameter[] parameters) {

                    for (int i = 0; i < parameters.length; i++) {
                        PhpDocParamTag phpDocParamTag1 = parameters[i].getDocTag();
                        if(phpDocParamTag1 != null) {
                            if(phpDocParamTag1.getTagValue().contains("#TranslationDomain")) {
                                return i;
                            }
                        }
                    }

                    return -1;
                }

            }

        );
    }

    public class DocTagHashing {

        private PsiElement psiElement;
        private PhpDocParamTag phpDocParamTag;

        private Parameter[] parameters;
        private ParameterList parameterList;

        public DocTagHashing(PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        private PhpDocParamTag getPhpDocParamTag() {
            return phpDocParamTag;
        }

        private ParameterList getParameterList() {
            return parameterList;
        }

        private Parameter[] getParameters() {
            return parameters;
        }

        public boolean isValid() {

            this.parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);

            if (this.parameterList == null || !(this.parameterList.getContext() instanceof MethodReference)) {
                return false;
            }

            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
            if(currentIndex == null) {
                return false;
            }

            MethodReference methodReference = (MethodReference) this.parameterList.getContext();
            if(methodReference == null) {
                return false;
            }

            PsiReference psiReference = methodReference.getReference();
            if (null == psiReference) {
                return false;
            }

            PsiElement resolvedReference = psiReference.resolve();
            if (!(resolvedReference instanceof Method)) {
                return false;
            }

            Method method = (Method) resolvedReference;
            this.parameters = method.getParameters();
            if(this.parameters.length -1 < currentIndex.getIndex()) {
                return false;
            }

            Parameter parameter = this.parameters[currentIndex.getIndex()];
            PhpDocParamTag phpDocParamTag = parameter.getDocTag();

            if(phpDocParamTag == null) {
                return false;
            }

            this.phpDocParamTag = phpDocParamTag;

            return true;
        }

    }

}
