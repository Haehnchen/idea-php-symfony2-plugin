package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.*;

public class PhpMethodVariableResolveUtil {

    /**
     * search for twig template variable on common use cases
     */
    public static Map<String, PsiVariable> collectMethodVariables(Function method) {

        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        List<PsiElement> psiElements = collectPossibleTemplateArrays(method);
        for(PsiElement templateVariablePsi: psiElements) {

            if(templateVariablePsi instanceof ArrayCreationExpression) {
                // "return array(...)" we dont need any parsing
                collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) templateVariablePsi));

            } else if(templateVariablePsi instanceof Variable) {
                // we need variable declaration line so resolve it and search for references which attach other values to array
                // find definition and search for references on it
                PsiElement resolvedVariable = ((Variable) templateVariablePsi).resolve();
                if(resolvedVariable instanceof Variable) {
                    collectedTypes.putAll(collectOnVariableReferences(method.getUseScope(), (Variable) resolvedVariable));
                }
            }
        }

        return collectedTypes;
    }

    /**
     *  search for possible variables which are possible accessible inside rendered twig template
     */
    private static List<PsiElement> collectPossibleTemplateArrays(Function method) {

        List<PsiElement> collectedTemplateVariables = new ArrayList<>();

        // Annotation controller
        // @TODO: check for phpdoc tag
        for(PhpReturn phpReturn : PsiTreeUtil.findChildrenOfType(method, PhpReturn.class)) {
            PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();

            // @TODO: think of support all types here
            // return $template
            // return array('foo' => $var)
            if(returnPsiElement instanceof Variable || returnPsiElement instanceof ArrayCreationExpression) {
                collectedTemplateVariables.add(returnPsiElement);
            }

        }

        // twig render calls:
        // $twig->render('foo', $vars);
        for(MethodReference methodReference : PsiTreeUtil.findChildrenOfType(method, MethodReference.class)) {
            if(new Symfony2InterfacesUtil().isTemplatingRenderCall(methodReference)) {
                PsiElement templateParameter = PsiElementUtils.getMethodParameterPsiElementAt((methodReference).getParameterList(), 1);
                if(templateParameter != null) {
                    collectedTemplateVariables.add(templateParameter);
                }
            }
        }

        return collectedTemplateVariables;
    }


    /**
     * search for references of variable declaration and collect the types
     *
     * @param searchScope should be method scope
     * @param variable the variable declaration psi $var = array();
     */
    private static Map<String, PsiVariable> collectOnVariableReferences(SearchScope searchScope, Variable variable) {

        final Map<String, PsiVariable> collectedTypes = new HashMap<>();

        PhpPsiUtil.hasReferencesInSearchScope(searchScope, variable, new CommonProcessors.FindProcessor<PsiReference>() {
            @Override
            protected boolean accept(PsiReference psiReference) {
                if (psiReference.getElement() instanceof Variable) {
                    PsiElement parent = psiReference.getElement().getParent();

                    // $template['variable'] = $foo
                    if (parent instanceof ArrayAccessExpression) {
                        collectedTypes.putAll(getTypesOnArrayIndex((ArrayAccessExpression) parent));
                    }

                    // array('foo' => $var)
                    if (parent instanceof AssignmentExpression) {
                        if (((AssignmentExpression) parent).getValue() instanceof ArrayCreationExpression) {
                            collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) ((AssignmentExpression) parent).getValue()));
                        }
                    }
                }

                return false;
            }
        });

        return collectedTypes;
    }

    /**
     * $template['var'] = $foo
     */
    private static Map<String, PsiVariable> getTypesOnArrayIndex(ArrayAccessExpression arrayAccessExpression) {

        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        ArrayIndex arrayIndex = arrayAccessExpression.getIndex();
        if(arrayIndex != null && arrayIndex.getValue() instanceof StringLiteralExpression) {

            String variableName = ((StringLiteralExpression) arrayIndex.getValue()).getContents();
            Set<String> variableTypes = new HashSet<>();

            PsiElement parent = arrayAccessExpression.getParent();
            if(parent instanceof AssignmentExpression) {
                PsiElement arrayValue = ((AssignmentExpression) parent).getValue();
                if(arrayValue instanceof PhpTypedElement) {
                    variableTypes = ((PhpTypedElement) arrayValue).getType().getTypes();
                }

                collectedTypes.put(variableName, new PsiVariable(variableTypes, ((AssignmentExpression) parent).getValue()));

            } else {
                collectedTypes.put(variableName, new PsiVariable(variableTypes, null));
            }


        }

        return collectedTypes;
    }

    /**
     *  array('foo' => $var, 'bar' => $bar)
     */
    public static Map<String, PsiVariable> getTypesOnArrayHash(ArrayCreationExpression arrayCreationExpression) {

        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            if(arrayHashElement.getKey() instanceof StringLiteralExpression) {

                String variableName = ((StringLiteralExpression) arrayHashElement.getKey()).getContents();
                Set<String> variableTypes = new HashSet<>();

                if(arrayHashElement.getValue() instanceof PhpTypedElement) {
                    variableTypes = ((PhpTypedElement) arrayHashElement.getValue()).getType().getTypes();
                }

                collectedTypes.put(variableName, new PsiVariable(variableTypes, arrayHashElement.getValue()));

            }
        }

        return collectedTypes;
    }
}
