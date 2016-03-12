package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.processor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryBuilderChainProcessor {

    private static final String DOCTRINE_ORM_QUERY_BUILDER = "\\Doctrine\\ORM\\QueryBuilder";
    final private MethodReference startMethodRef;

    private List<MethodReference> queryBuilderMethodReferences;
    private List<MethodReference> queryBuilderFactoryMethods;

    public QueryBuilderChainProcessor(MethodReference psiElement) {
        this.startMethodRef = psiElement;
    }

    public void collectMethods() {

        // get chaining methods references after current one and reverse it; to get some right ordering
        List<MethodReference> methodReferences = getMethodReferencesAfter(startMethodRef);
        Collections.reverse(methodReferences);

        List<MethodReference> factoryReferences = new ArrayList<MethodReference>();
        processUpChainingMethods(startMethodRef, methodReferences, factoryReferences, true);

        this.queryBuilderMethodReferences = methodReferences;
        this.queryBuilderFactoryMethods = factoryReferences;
    }

    public List<MethodReference> getQueryBuilderMethodReferences() {
        return queryBuilderMethodReferences;
    }

    public List<MethodReference> getQueryBuilderFactoryMethods() {
        return queryBuilderFactoryMethods;
    }

    /**
     * We are inside addSelect and want to get orderBy and orderBy
     *
     * $qb->addSelect('<here>')->orderBy()->orderBy()
     *
     * @param methodReference current method
     */
    @NotNull
    public List<MethodReference> getMethodReferencesAfter(@NotNull MethodReference methodReference) {

        List<MethodReference> methodReferences = new ArrayList<MethodReference>();

        PsiElement parent = methodReference;
        while (parent instanceof MethodReference) {

            PsiElement method = ((MethodReference) parent).resolve();
            if(method instanceof Method && isQueryBuilderInstance((Method) method) == InstanceType.DIRECT) {
                methodReferences.add((MethodReference) parent);
            }

            parent = parent.getParent();
        }

        return methodReferences;
    }

    public void processNextMethodReference(Method method, List<MethodReference> medRefCollection, List<MethodReference> factoryReferences) {
        for(PhpReturn phpReturn: PsiTreeUtil.collectElementsOfType(method, PhpReturn.class)) {
            PsiElement child = phpReturn.getFirstPsiChild();
            processUpChainingMethods(child, medRefCollection, factoryReferences, true);
        }
    }

    private void processVariableScope(List<MethodReference> medRefCollection, List<MethodReference> factoryReferences, Variable child) {
        for(Variable varRef: PhpElementsUtil.getVariableReferencesInScope(child, false)) {

            // we need to handle variables and their declaration in different ways
            if(varRef.isDeclaration()) {

                // we are inside var declaration, so our expression is already "select", no further action
                // $var = $qb->addSelect()->select();
                PsiElement assignExpress = varRef.getParent();
                if(assignExpress instanceof AssignmentExpression) {
                    PhpPsiElement metRef = ((AssignmentExpression) assignExpress).getValue();
                    if(metRef instanceof MethodReference) {
                        processUpChainingMethods(metRef, medRefCollection, factoryReferences, false);
                    }
                }


            } else {

                // we get "$qb" first but to provide right chaining we need deepest method reference "select",
                // phpstorm lexer provide chaining methods in reverse order
                // we need to walk psi tree up until last method references call
                // $qb->addSelect()->select()
                MethodReference methodReference = getLastParentOfType(varRef, MethodReference.class);
                if(methodReference != null) {
                    processUpChainingMethods(methodReference, medRefCollection, factoryReferences, false);
                }

            }

        }
    }

    public void processUpChainingMethods(PsiElement psiElement, List<MethodReference> medRefCollection, List<MethodReference> factoryReferences, boolean resolveVar) {

        PsiElement child = psiElement;
        while (child instanceof MethodReference) {

            // stop on invalid item like factory method eg createQueryBuilder
            if (!nextMethodScope(medRefCollection, factoryReferences, (MethodReference) child)) {
                return;
            }

            child = ((MethodReference) child).getFirstPsiChild();
        }

        // $qb->addSelect();
        // $this->foo() = invalid
        if(resolveVar && child instanceof Variable && !"this".equals(((Variable) child).getName())) {
            processVariableScope(medRefCollection, factoryReferences, (Variable) child);
        }

    }

    private boolean nextMethodScope(List<MethodReference> medRefCollection, List<MethodReference> factoryReferences, MethodReference child) {

        // get original method
        PsiElement method = child.resolve();
        if(!(method instanceof Method)) {
            return false;
        }

        InstanceType queryBuilderInstance = isQueryBuilderInstance((Method) method);
        switch (queryBuilderInstance) {
            case DIRECT:

                // we are inside QueryBuilder class eg. addSelect
                medRefCollection.add(child);
                return true;

            case RESOLVE:

                // we found a method returning QueryBuilder

                // stop on direct querybuilder factory method
                if ("createQueryBuilder".equals(((Method) method).getName())) {
                    factoryReferences.add(child);
                    return false;
                }

                // goto (resolve) method and collect its QueryBuilder method
                processNextMethodReference((Method) method, medRefCollection, factoryReferences);
                return true;

            default:
                factoryReferences.add(child);
                return false;
        }

    }

    @NotNull
    private static InstanceType isQueryBuilderInstance(@NotNull Method method) {

        PhpClass containingClass = method.getContainingClass();
        if(containingClass != null && PhpElementsUtil.isInstanceOf(containingClass, DOCTRINE_ORM_QUERY_BUILDER)) {
            return InstanceType.DIRECT;
        }

        for(PhpClass phpClass: PhpElementsUtil.getClassFromPhpTypeSet(method.getProject(), method.getType().getTypes())) {
            if(PhpElementsUtil.isEqualClassName(phpClass, DOCTRINE_ORM_QUERY_BUILDER)) {
                return InstanceType.RESOLVE;
            }
        }

        return InstanceType.NONE;
    }

    public enum InstanceType {
        NONE, DIRECT, RESOLVE
    }

    @Nullable
    private static <T extends PsiElement> T getLastParentOfType(@Nullable PsiElement element, @NotNull Class<T> aClass) {
        if (element == null) return null;

        PsiElement last = element.getParent();
        if(!aClass.isInstance(last)) {
            return null;
        }

        while (aClass.isInstance(last)) {
            if (element instanceof PsiFile) return null;
            element = element.getParent();
            if(!aClass.isInstance(element)) {
                //noinspection unchecked
                return (T)last;
            }
            last = element;
        }

        //noinspection unchecked
        return null;
    }

}
