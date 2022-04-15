package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if(targets.size() == 0 || psiVariables == null || previousElements == null || previousElements.size() != 0) {
            return;
        }

        TwigTypeContainer twigTypeContainer = targets.iterator().next();

        if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {
            if(PhpElementsUtil.isInstanceOf((PhpClass) twigTypeContainer.getPhpNamedElement(), "\\Symfony\\Component\\Form\\FormView")) {
                if(psiVariables.size() > 0) {
                    PsiElement var = psiVariables.iterator().next().getElement();

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
                            if(methodReference != null) {
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
    }

    private static void attachFormFields(@Nullable MethodReference methodReference, @NotNull Collection<TwigTypeContainer> targets) {
        if (methodReference == null) {
            return;
        }

        int index = -1;

        if (PhpElementsUtil.isMethodReferenceInstanceOf(
            methodReference,
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "createForm"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "create")
        )) {
            index = 0;
        } else if (PhpElementsUtil.isMethodReferenceInstanceOf(
            methodReference,
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamed")
        )) {
            index = 1;
        }

        if (index < 0) {
            return;
        }

        PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, index);
        if(formType != null) {
            PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(formType);

            if(phpClass == null) {
                return;
            }

            Method method = phpClass.findMethodByName("buildForm");
            if(method == null) {
                return;
            }

            targets.addAll(getTwigTypeContainer(method, phpClass));
        }
    }

    @NotNull
    private static Collection<TwigTypeContainer> getTwigTypeContainer(@NotNull Method method, @NotNull PhpClass formTypClass) {
        Collection<TwigTypeContainer> twigTypeContainers = new ArrayList<>();

        for(MethodReference methodReference: FormUtil.getFormBuilderTypes(method)) {
            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if (fieldName == null) {
                continue;
            }

            PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
            TwigTypeContainer twigTypeContainer = new TwigTypeContainer(fieldName);

            // find form field type
            if(psiElement != null) {
                PhpClass fieldType = FormUtil.getFormTypeClassOnParameter(psiElement);
                if(fieldType != null) {
                    twigTypeContainer.withDataHolder(new FormDataHolder(fieldType, formTypClass, psiElement));
                }
            }

            twigTypeContainers.add(twigTypeContainer);
        }

        return twigTypeContainers;
    }

}
