package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.google.common.collect.Iterables;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
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
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormFieldResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable List<PsiVariable> psiVariables) {

        if(targets.size() == 0 || psiVariables == null || previousElements == null || previousElements.size() != 0) {
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


                    // nested resolve of form view; @TODO: should be some nicer
                    // 'foo2' => $form2 => $form2 = $form->createView() => $this->createForm(new Type();
                    if(var instanceof Variable) {
                        PsiElement varDecl = ((Variable) var).resolve();
                        if(varDecl instanceof Variable) {
                            MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                            PsiElement scopeVar = methodReference.getFirstChild();

                            // $form2 = $form->createView()
                            if(scopeVar instanceof Variable) {
                                PsiElement varDeclParent = ((Variable) scopeVar).resolve();
                                if(varDeclParent instanceof Variable) {

                                    // "$form"->createView();
                                    PsiElement resolve = ((Variable) varDeclParent).resolve();
                                    if(resolve != null) {
                                        attachFormFields(PsiTreeUtil.getNextSiblingOfType(resolve, MethodReference.class), targets);
                                    }

                                }
                            }
                            
                        }
                    }

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

                targets.addAll(getTwigTypeContainer(method));
            }

        }

    }

    public static List<TwigTypeContainer> getTwigTypeContainer(Method method) {

        MethodReference[] formBuilderTypes = FormUtil.getFormBuilderTypes(method);
        List<TwigTypeContainer> twigTypeContainers = new ArrayList<TwigTypeContainer>();

        for(MethodReference methodReference: formBuilderTypes) {

            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
            TwigTypeContainer twigTypeContainer = new TwigTypeContainer(fieldName);

            // find form field type
            if(psiElement != null) {
                PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(psiElement);
                if(phpClass != null) {
                    twigTypeContainer.withDataHolder(new FormDataHolder(phpClass));
                }
            }

            twigTypeContainers.add(twigTypeContainer);
        }

        return twigTypeContainers;
    }

}
