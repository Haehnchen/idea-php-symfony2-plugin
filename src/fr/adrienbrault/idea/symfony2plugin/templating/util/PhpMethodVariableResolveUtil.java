package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.CommonProcessors;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.*;

public class PhpMethodVariableResolveUtil {

    /**
     * search for twig template variable on common use cases
     */
    public static HashMap<String, Set<String>> collectMethodVariables(Method method) {

        HashMap<String, Set<String>> collectedTypes = new HashMap<String, Set<String>>();

        ArrayList<PsiElement> psiElements = collectPossibleTemplateArrays(method);
        for(PsiElement templateVariablePsi: psiElements) {

            // "return array(...)" we dont need any parsing
            if(templateVariablePsi instanceof ArrayCreationExpression) {
                collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) templateVariablePsi));
            }

            // we need variable declaration line so resolve it and search for references which attach other values to array
            if(templateVariablePsi instanceof Variable) {

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
    private static ArrayList<PsiElement> collectPossibleTemplateArrays(Method method) {

        ArrayList<PsiElement> collectedTemplateVariables = new ArrayList<PsiElement>();

        Collection<PhpReturn> phpReturns = PsiTreeUtil.findChildrenOfType(method, PhpReturn.class);

        for(PhpReturn phpReturn : phpReturns) {
            PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();

            // @TODO: think of support all types here
            // return $template
            // return array('foo' => $var)
            if(returnPsiElement instanceof Variable || returnPsiElement instanceof ArrayCreationExpression) {
                collectedTemplateVariables.add(returnPsiElement);
            }

            // return render('foo.html.twig', $template|array())
            if(returnPsiElement instanceof MethodReference) {
                if(new Symfony2InterfacesUtil().isTemplatingRenderCall(returnPsiElement)) {
                    PsiElement templateParameter = PsiElementUtils.getMethodParameterPsiElementAt(((MethodReference) returnPsiElement).getParameterList(), 1);
                    if(templateParameter != null) {
                        collectedTemplateVariables.add(templateParameter);
                    }

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
    private static HashMap<String, Set<String>> collectOnVariableReferences(SearchScope searchScope, Variable variable) {

        final HashMap<String, Set<String>> collectedTypes = new HashMap<String, Set<String>>();

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
    private static HashMap<String, Set<String>> getTypesOnArrayIndex(ArrayAccessExpression arrayAccessExpression) {

        HashMap<String, Set<String>> collectedTypes = new HashMap<String, Set<String>>();

        ArrayIndex arrayIndex = arrayAccessExpression.getIndex();
        if(arrayIndex != null && arrayIndex.getValue() instanceof StringLiteralExpression) {

            String variableName = ((StringLiteralExpression) arrayIndex.getValue()).getContents();
            Set<String> variableTypes = new HashSet<String>();

            if(arrayAccessExpression.getParent() instanceof AssignmentExpression) {
                PsiElement arrayValue = ((AssignmentExpression) arrayAccessExpression.getParent()).getValue();
                if(arrayValue instanceof PhpTypedElement) {
                    variableTypes = ((PhpTypedElement) arrayValue).getType().getTypes();
                }
            }

            collectedTypes.put(variableName, variableTypes);

        }

        return collectedTypes;
    }

    /**
     *  array('foo' => $var, 'bar' => $bar)
     */
    public static HashMap<String, Set<String>> getTypesOnArrayHash(ArrayCreationExpression arrayCreationExpression) {
        HashMap<String, Set<String>> collectedTypes = new HashMap<String, Set<String>>();

        for(ArrayHashElement arrayHashElement: arrayCreationExpression.getHashElements()) {
            if(arrayHashElement.getKey() instanceof StringLiteralExpression) {

                String variableName = ((StringLiteralExpression) arrayHashElement.getKey()).getContents();
                Set<String> variableTypes = new HashSet<String>();

                if(arrayHashElement.getValue() instanceof PhpTypedElement) {
                    variableTypes = ((PhpTypedElement) arrayHashElement.getValue()).getType().getTypes();
                }

                collectedTypes.put(variableName, variableTypes);

            }
        }

        return collectedTypes;
    }
}
