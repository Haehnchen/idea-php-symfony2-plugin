package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocParamTag;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.DefaultReferenceProvider;
import fr.adrienbrault.idea.symfony2plugin.config.PhpClassReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeReference;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteReference;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


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

                    ArrayList<PhpDocParamTag> phpDocParamTags = docTagHashing.getPhpDocParamTags();

                    ArrayList<PsiReference> psiReferences = new ArrayList<PsiReference>();

                    int i = 0;
                    for(PhpDocParamTag phpDocParamTag: phpDocParamTags) {
                        this.attachReferences((StringLiteralExpression) psiElement, phpDocParamTag, docTagHashing.getParameters().get(i++), docTagHashing.getParameterList() , psiReferences);
                    }

                    return psiReferences.toArray(new PsiReference[psiReferences.size()]);

                }

                private void attachReferences(StringLiteralExpression psiElement, PhpDocParamTag phpDocParamTag, Parameter[] parameters, ParameterList parameterList, ArrayList<PsiReference> psiReferences) {

                    /* @TODO: use this
                       for(AssistantReferenceProvider assistantReferenceProvider: DefaultReferenceProvider.DEFAULT_PROVIDERS) {
                        if(phpDocParamTag.getTagValue().contains("#" + assistantReferenceProvider.getDocBlockParamAlias())) {
                            psiReferences.add(assistantReferenceProvider.getPsiReference(psiElement, null));
                        }
                    } */

                    if(phpDocParamTag.getTagValue().contains("#Entity")) {
                        psiReferences.add(new EntityReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#Service")) {
                        psiReferences.add(new ServiceReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#TranslationDomain")) {
                        psiReferences.add(new TranslationDomainReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#Template")) {
                        psiReferences.add(new TemplateReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#Route")) {
                        psiReferences.add(new RouteReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#Class")) {
                        psiReferences.add(new PhpClassReference(psiElement, true));
                    }

                    if(phpDocParamTag.getTagValue().contains("#FormType")) {
                        psiReferences.add(new FormTypeReference(psiElement));
                    }

                    if(phpDocParamTag.getTagValue().contains("#TranslationKey")) {

                        String filterDomain = "messages";

                        int domainKey = getMatchingTranslationDomain(parameters);
                        if(domainKey >= 0) {
                            String keyParameter = PsiElementUtils.getMethodParameterAt(parameterList, domainKey);
                            if(keyParameter != null) {
                                filterDomain = keyParameter;
                            }
                        }

                        psiReferences.add(new TranslationReference(psiElement, filterDomain));
                    }

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
        private ArrayList<PhpDocParamTag> phpDocParamTags;

        private ArrayList<Parameter[]> parameters = new ArrayList<Parameter[]>();
        private ParameterList parameterList;

        public DocTagHashing(PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        private ArrayList<PhpDocParamTag> getPhpDocParamTags() {
            return phpDocParamTags;
        }

        private ParameterList getParameterList() {
            return parameterList;
        }

        private ArrayList<Parameter[]> getParameters() {
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
            Parameter[] methodParameter = method.getParameters();
            if(methodParameter.length -1 < currentIndex.getIndex()) {
                return false;
            }

            this.phpDocParamTags = new ArrayList<PhpDocParamTag>();
            Method[] implementedMethods = PhpElementsUtil.getImplementedMethods(method);

            for(Method implementedMethod: implementedMethods) {
                Parameter[] implementedParameters = implementedMethod.getParameters();
                if(!(implementedParameters.length -1 < currentIndex.getIndex())) {
                    Parameter parameter = implementedParameters[currentIndex.getIndex()];
                    PsiElement implementedParameterList = parameter.getContext();

                    if(implementedParameterList instanceof ParameterList) {
                        PhpDocParamTag phpDocParamTag = parameter.getDocTag();
                        if(phpDocParamTag != null) {
                            this.phpDocParamTags.add(phpDocParamTag);
                            this.parameters.add(implementedParameters);
                        }
                    }
                }
            }

            return phpDocParamTags.size() > 0;
        }

    }

}
