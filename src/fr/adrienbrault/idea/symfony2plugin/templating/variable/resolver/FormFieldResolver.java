package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.google.common.collect.Iterables;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class FormFieldResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable List<PsiVariable> psiVariables) {

        if(psiVariables == null || previousElements == null || previousElements.size() != 0) {
            return;
        }

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        TwigTypeContainer twigTypeContainer = targets.iterator().next();

        if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
            if(symfony2InterfacesUtil.isInstanceOf((PhpClass) twigTypeContainer.getPhpNamedElement(), "\\Symfony\\Component\\Form\\FormView")) {
                if(psiVariables.size() > 0) {
                    PsiElement var = psiVariables.get(0).getElement();

                    // $form->createView()
                    if(var instanceof MethodReference) {
                        PsiElement form = var.getFirstChild();
                        if(form instanceof Variable) {
                            PsiElement varDecl = ((Variable) form).resolve();
                            if(varDecl instanceof Variable) {
                                MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                                attachFormFields(methodReference, targets);
                            }
                        }
                    }

                    /*
                    // $form
                    if(var instanceof Variable) {
                        PsiElement varDecl = ((Variable) var).resolve();
                        if(varDecl instanceof Variable) {
                            MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                            attachFormFields(methodReference, targets);
                        }
                    } */

                }
            }

        }
    }

    private static void attachFormFields(@Nullable MethodReference methodReference, Collection<TwigTypeContainer> targets) {

        if(methodReference == null) {
            return;
        }

        Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        if(symfony2InterfacesUtil.isCallTo(methodReference, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "createForm")) {
            PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0);
            if(formType != null) {
                PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(formType);

                if(phpClass == null) {
                    return;
                }

                Method method = PhpElementsUtil.getClassMethod(phpClass, "buildForm");
                if(method == null) {
                    return;
                }

                for(LookupElement lookupElement: FormFieldNameReference.getFormLookups(method)) {
                    targets.add(new TwigTypeContainer(lookupElement.getLookupString()));
                }
            }

        }

    }

}
