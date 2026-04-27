package fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver;

import com.intellij.openapi.project.Project;
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
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldResolver implements TwigTypeResolver {

    public void resolve(@NotNull Project project, Collection<TwigTypeContainer> targets, Collection<TwigTypeContainer> previousElement, String typeName, Collection<List<TwigTypeContainer>> previousElements, @Nullable Collection<PsiVariable> psiVariables) {
        if (targets.isEmpty() || previousElements == null || !previousElements.isEmpty()) {
            return;
        }

        TwigTypeContainer twigTypeContainer = targets.iterator().next();
        FormViewDataHolder formViewDataHolder = twigTypeContainer.getFormViewDataHolder();
        if (formViewDataHolder == null || formViewDataHolder.formTypeFqns().isEmpty()) {
            return;
        }

        visitFormFields(project, formViewDataHolder.formTypeFqns(), field -> targets.add(toTwigTypeContainer(field)));
    }

    public static boolean isFormView(@NotNull PhpType phpType) {
        return phpType.types()
            .anyMatch(s ->
                s.equals("\\Symfony\\Component\\Form\\FormView") ||
                    s.equals("\\Symfony\\Component\\Form\\FormInterface") // form view is create converting by Symfony on template render
            );
    }

    @NotNull
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

    /**
     * Resolves form type FQNs for a form reference such as {@code $form->createView()}.
     *
     * <p>This is the primitive counterpart of {@link #getFormTypeFromFormFactory(PsiElement)}. It may still use PSI
     * internally, but it returns only normalized FQN strings with a leading backslash.</p>
     */
    @NotNull
    public static Set<String> getFormTypeFqnsFromFormFactory(@NotNull PsiElement formReference) {
        Set<String> formTypeFqns = new LinkedHashSet<>();

        for (PhpClass phpClass : getFormTypeFromFormFactory(formReference)) {
            formTypeFqns.add(phpClass.getFQN());
        }

        return formTypeFqns;
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
    private static List<TwigFormField> getTwigFormFields(@NotNull Method method, @NotNull PhpClass formTypeClass) {
        List<TwigFormField> twigFormFields = new ArrayList<>();

        for(MethodReference methodReference: FormUtil.getFormBuilderTypes(method)) {

            String fieldName = PsiElementUtils.getMethodParameterAt(methodReference, 0);
            if (fieldName == null) {
                continue;
            }

            PsiElement psiElement = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 1);
            String fieldTypeFqn = null;

            // find form field type
            if(psiElement != null) {
                fieldTypeFqn = FormUtil.getFormTypeFqnOnParameter(psiElement);
            }

            twigFormFields.add(new TwigFormField(fieldName, fieldTypeFqn, formTypeClass.getFQN()));
        }

        return twigFormFields;
    }

    /**
     * Search and resolve: "$form->createView()" to its PhpClass which is a form type
     */
    public static void visitFormReferencesFields(@NotNull PsiElement formReference, @NotNull Consumer<TwigTypeContainer> consumer) {
        visitFormFields(formReference.getProject(), getFormTypeFqnsFromFormFactory(formReference), field -> consumer.accept(toTwigTypeContainer(field)));
    }

    /**
     * Visit all form fields in given PhpClass which are already a form type
     */
    public static void visitFormReferencesFields(@NotNull PhpClass phpClass, @NotNull Consumer<TwigTypeContainer> consumer) {
        visitFormFields(phpClass.getProject(), Collections.singleton(phpClass.getFQN()), field -> consumer.accept(toTwigTypeContainer(field)));
    }

    public static void visitFormFields(@NotNull Project project, @NotNull Collection<String> formTypeFqns, @NotNull Consumer<TwigFormField> consumer) {
        FormUtil.FormTypeCollector collector = null;

        Collection<Method> methods = new HashSet<>();

        for (String formTypeFqn : formTypeFqns) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, formTypeFqn);
            if (phpClass == null) {
                continue;
            }

            Method method = phpClass.findMethodByName("buildForm");
            if (method != null) {
                methods.add(method);
            }

            if (collector == null) {
                collector = new FormUtil.FormTypeCollector(project).collect();
            }

            for (PhpClass formInHierarchicalPath : FormUtil.getParentAndExtendingTypes(phpClass, collector)) {
                Method buildFormHierarchical = formInHierarchicalPath.findMethodByName("buildForm");
                if (buildFormHierarchical != null) {
                    methods.add(buildFormHierarchical);
                }
            }
        }

        for (Method method : methods) {
            PhpClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                consumeFieldType(containingClass, consumer);
            }
        }
    }

    @NotNull
    private static TwigTypeContainer toTwigTypeContainer(@NotNull TwigFormField field) {
        return new TwigTypeContainer(field.name(), new FormFieldDataHolder(field.fieldTypeFqn(), field.ownerFormTypeFqn()));
    }

    private static void consumeFieldType(@NotNull PhpClass phpClass, @NotNull Consumer<TwigFormField> consumer) {
        Method method = phpClass.findMethodByName("buildForm");
        if (method == null) {
            return;
        }

        for (TwigFormField twigFormField : getTwigFormFields(method, phpClass)) {
            consumer.accept(twigFormField);
        }
    }
}
