package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMethodVariableResolveUtil {

    /**
     * search for twig template variable on common use cases
     *
     * $this->render('foobar.html.twig', $foobar)
     * $this->render('foobar.html.twig', ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_merge($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_merge_recursive($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_push($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', array_replace($foobar, ['foobar' => $var]))
     * $this->render('foobar.html.twig', $foobar + ['foobar' => $var])
     * $this->render('foobar.html.twig', $foobar += ['foobar' => $var])
     */
    public static Map<String, PsiVariable> collectMethodVariables(@NotNull Function method) {

        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        for(PsiElement var: collectPossibleTemplateArrays(method)) {
            if(var instanceof ArrayCreationExpression) {
                // "return array(...)" we dont need any parsing
                collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) var));
            } else if(var instanceof Variable) {
                // we need variable declaration line so resolve it and search for references which attach other values to array
                // find definition and search for references on it
                PsiElement resolvedVariable = ((Variable) var).resolve();
                if(resolvedVariable instanceof Variable) {
                    collectedTypes.putAll(collectOnVariableReferences(method, (Variable) resolvedVariable));
                }
            } else if(var instanceof FunctionReference && "array_merge".equalsIgnoreCase(((FunctionReference) var).getName())) {
                // array_merge($var, ['foobar' => $var]);

                String name = ((FunctionReference) var).getName();
                if("array_merge".equalsIgnoreCase(name) || "array_merge_recursive".equalsIgnoreCase(name) || "array_push".equalsIgnoreCase(name) || "array_replace".equalsIgnoreCase(name)) {
                    for (PsiElement psiElement : ((FunctionReference) var).getParameters()) {
                        collectVariablesForPsiElement(method, collectedTypes, psiElement);
                    }
                }
            } else if(var instanceof BinaryExpression && var.getNode().getElementType() == PhpElementTypes.ADDITIVE_EXPRESSION) {
                // $var + ['foobar' => $foobar]
                PsiElement leftOperand = ((BinaryExpression) var).getLeftOperand();
                if(leftOperand != null) {
                    collectVariablesForPsiElement(method, collectedTypes, leftOperand);
                }

                PsiElement rightOperand = ((BinaryExpression) var).getRightOperand();
                if(rightOperand != null) {
                    collectVariablesForPsiElement(method, collectedTypes, rightOperand);
                }
            } else if(var instanceof SelfAssignmentExpression) {
                // $var += ['foobar' => $foobar]
                PhpPsiElement variable = ((SelfAssignmentExpression) var).getVariable();
                if(variable != null) {
                    collectVariablesForPsiElement(method, collectedTypes, variable);
                }

                PhpPsiElement value = ((SelfAssignmentExpression) var).getValue();
                if(value != null) {
                    collectVariablesForPsiElement(method, collectedTypes, value);
                }
            }
        }

        return collectedTypes;
    }

    private static void collectVariablesForPsiElement(@NotNull Function method, @NotNull Map<String, PsiVariable> collectedTypes, @NotNull PsiElement psiElement) {
        if(psiElement instanceof ArrayCreationExpression) {
            // reuse array collector: ['foobar' => $var]
            collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) psiElement));
        } else if(psiElement instanceof Variable) {
            // reuse variable collector: [$var]
            PsiElement resolvedVariable = ((Variable) psiElement).resolve();
            if(resolvedVariable instanceof Variable) {
                collectedTypes.putAll(collectOnVariableReferences(method, (Variable) resolvedVariable));
            }
        }
    }

    /**
     *  search for variables which are possible accessible inside rendered twig template
     */
    @NotNull
    private static List<PsiElement> collectPossibleTemplateArrays(@NotNull Function method) {

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
            if(PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES)) {
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
     * @param function should be function / method scope
     * @param variable the variable declaration psi $var = array();
     */
    @NotNull
    private static Map<String, PsiVariable> collectOnVariableReferences(@NotNull Function function, @NotNull Variable variable) {
        Map<String, PsiVariable> collectedTypes = new HashMap<>();

        for (Variable scopeVar : PhpElementsUtil.getVariablesInScope(function, variable)) {
            PsiElement parent = scopeVar.getParent();
            if (parent instanceof ArrayAccessExpression) {
                // $template['variable'] = $foo
                collectedTypes.putAll(getTypesOnArrayIndex((ArrayAccessExpression) parent));
            } else if (parent instanceof AssignmentExpression) {
                // array('foo' => $var)
                if (((AssignmentExpression) parent).getValue() instanceof ArrayCreationExpression) {
                    collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) ((AssignmentExpression) parent).getValue()));
                }
            }
        }

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
                    variableTypes.addAll(((PhpTypedElement) arrayValue).getType().getTypes());
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
                    variableTypes.addAll(((PhpTypedElement) arrayHashElement.getValue()).getType().getTypes());
                }

                collectedTypes.put(variableName, new PsiVariable(variableTypes, arrayHashElement.getValue()));

            }
        }

        return collectedTypes;
    }
}
