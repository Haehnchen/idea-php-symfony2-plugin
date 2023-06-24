package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
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
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldResolver implements TwigTypeResolver {

    public void resolve(Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (targets.size() == 0 || psiVariables == null || psiVariables.size() == 0 || previousElements == null || previousElements.size() != 0) {
            return;
        }

        TwigTypeContainer twigTypeContainer = targets.iterator().next();
        if (twigTypeContainer.getPhpNamedElement() instanceof PhpClass phpClass && isFormView(phpClass)) {
            visitFormReferencesFields(psiVariables.iterator().next().getElement(), targets::add);
        }
    }

    public static boolean isFormView(@NotNull PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormView") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormInterface"); // form view is create converting by Symfony on template render
    }

    public static boolean isFormView(@NotNull PhpType phpType) {
        return phpType.types()
            .anyMatch(s ->
                s.equals("\\Symfony\\Component\\Form\\FormView") ||
                    s.equals("\\Symfony\\Component\\Form\\FormInterface") // form view is create converting by Symfony on template render
            );
    }

    public static Collection<PhpClass> getFormTypeFromFormFactory(@NotNull PsiElement formReference) {
        Collection<PhpClass> phpClasses = new ArrayList<>();

        // $form->createView()
        if (formReference instanceof MethodReference) {
            PsiElement form = formReference.getFirstChild();
            if (form instanceof Variable) {
                PsiElement varDecl = ((Variable) form).resolve();
                if (varDecl instanceof Variable) {
                    MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                    if (methodReference != null) {
                        PhpClass phpClass = resolveCall(methodReference);
                        if (phpClass != null) {
                            phpClasses.add(phpClass);
                        }
                    }
                }
            }
        }

        // nested resolve of form view; @TODO: should be some nicer
        // 'foo2' => $form2 => $form2 = $form->createView() => $this->createForm(new Type();
        if (formReference instanceof Variable) {
            PsiElement varDecl = ((Variable) formReference).resolve();


            // 'foo2' => $form2;
            if (varDecl instanceof Variable variable) {
                PhpType type = PhpIndex.getInstance(variable.getProject()).completeType(variable.getProject(), variable.getType(), new HashSet<>());
                if (isFormView(type)) {
                    PsiElement resolve = variable.resolve();
                    if (resolve != null) {
                        MethodReference nextSiblingOfType = PsiTreeUtil.getNextSiblingOfType(resolve, MethodReference.class);
                        if (nextSiblingOfType != null) {
                            PhpClass phpClass = resolveCall(nextSiblingOfType);
                            if (phpClass != null) {
                                phpClasses.add(phpClass);
                            }
                        }
                    }
                }

                MethodReference methodReference = PsiTreeUtil.getNextSiblingOfType(varDecl, MethodReference.class);
                if (methodReference != null) {
                    PsiElement scopeVar = methodReference.getFirstChild();

                    // $form2 = $form->createView()
                    if (scopeVar instanceof Variable) {
                        PsiElement varDeclParent = ((Variable) scopeVar).resolve();
                        if (varDeclParent instanceof Variable) {
                            // "$form"->createView();
                            PsiElement resolve = ((Variable) varDeclParent).resolve();
                            if (resolve != null) {
                                MethodReference nextSiblingOfType = PsiTreeUtil.getNextSiblingOfType(resolve, MethodReference.class);
                                if (nextSiblingOfType != null) {
                                    PhpClass phpClass = resolveCall(methodReference);
                                    if (phpClass != null) {
                                        phpClasses.add(phpClass);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return phpClasses;
    }

    @Nullable
    private static PhpClass resolveCall(@NotNull MethodReference methodReference) {
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
            return null;
        }

        PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, index);
        if (formType != null) {
            return FormUtil.getFormTypeClassOnParameter(formType);
        }

        return null;
    }

    @NotNull
    private static List<TwigTypeContainer> getTwigTypeContainer(@NotNull Method method, @NotNull PhpClass formTypClass) {
        List<TwigTypeContainer> twigTypeContainers = new ArrayList<>();

        for(MethodReference methodReference: FormUtil.getFormBuilderTypes(method)) {

            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
            TwigTypeContainer twigTypeContainer = new TwigTypeContainer(fieldName);

            // find form field type
            if(psiElement != null) {
                PhpClass fieldType = FormUtil.getFormTypeClassOnParameter(psiElement);
                if(fieldType != null) {
                    twigTypeContainer.withDataHolder(new FormDataHolder(fieldType, formTypClass));
                }
            }

            twigTypeContainers.add(twigTypeContainer);
        }

        return twigTypeContainers;
    }

    /**
     * Search and resolve: "$form->createView()"
     */
    public static void visitFormReferencesFields(PsiElement formReference, @NotNull Consumer<TwigTypeContainer> consumer) {
        for (PhpClass phpClass : getFormTypeFromFormFactory(formReference)) {
            Method method = phpClass.findMethodByName("buildForm");
            if(method == null) {
                return;
            }

            visitFormReferencesFields(phpClass, consumer);
        }
    }

    public static void visitFormReferencesFields(@NotNull PhpClass phpClass, @NotNull Consumer<TwigTypeContainer> consumer) {
        Method method = phpClass.findMethodByName("buildForm");
        if(method == null) {
            return;
        }

        for (TwigTypeContainer twigTypeContainer : getTwigTypeContainer(method, phpClass)) {
            consumer.accept(twigTypeContainer);
        }
    }
}
