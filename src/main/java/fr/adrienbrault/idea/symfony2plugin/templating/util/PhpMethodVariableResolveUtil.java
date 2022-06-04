package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import kotlin.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

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
        Set<FunctionReference> references = new HashSet<>();
        visitRenderTemplateFunctions(method, triple -> {
            FunctionReference functionScope = triple.getThird();
            if (functionScope != null) {
                references.add(functionScope);
            }
        });

        for(FunctionReference methodReference : references) {
            PsiElement templateParameter = PsiElementUtils.getMethodParameterPsiElementAt((methodReference).getParameterList(), 1);
            if(templateParameter != null) {
                collectedTemplateVariables.add(templateParameter);
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
                Pair<String, PsiVariable> pair = getTypesOnArrayIndex((ArrayAccessExpression) parent);
                if (pair != null) {
                    collectedTypes.put(pair.getFirst(), pair.getSecond());
                }
            } else if (parent instanceof AssignmentExpression) {
                // array('foo' => $var)
                PhpPsiElement value = ((AssignmentExpression) parent).getValue();
                if (value instanceof ArrayCreationExpression) {
                    collectedTypes.putAll(getTypesOnArrayHash((ArrayCreationExpression) value));
                }
            }
        }

        return collectedTypes;
    }

    /**
     * $template['var'] = $foo
     */
    @Nullable
    private static Pair<String, PsiVariable> getTypesOnArrayIndex(@NotNull ArrayAccessExpression arrayAccessExpression) {
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

                return Pair.create(variableName, new PsiVariable(variableTypes, ((AssignmentExpression) parent).getValue()));
            } else {
                return Pair.create(variableName, new PsiVariable(variableTypes));
            }
        }

        return null;
    }

    /**
     *  array('foo' => $var, 'bar' => $bar)
     */
    public static Map<String, PsiVariable> getTypesOnArrayHash(@NotNull ArrayCreationExpression arrayCreationExpression) {
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

    /**
     * Visit method scope for template renders, also via annotation of the method itself
     *
     * As annotations are not in scope of the method itself
     */
    public static void visitRenderTemplateFunctions(@NotNull Method method, @NotNull Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
        TemplateRenderPsiRecursiveElementWalkingVisitor psiElementVisitor = new TemplateRenderPsiRecursiveElementWalkingVisitor(method, consumer);

        PhpDocComment docComment = method.getDocComment();
        for (PhpDocTag phpDocTag : PsiTreeUtil.getChildrenOfTypeAsList(docComment, PhpDocTag.class)) {
            psiElementVisitor.visitPhpDocTag(phpDocTag);
        }

        for (PhpAttributesList phpAttributesList : PsiTreeUtil.getChildrenOfTypeAsList(method, PhpAttributesList.class)) {
            psiElementVisitor.visitPhpAttribute(phpAttributesList);
        }

        method.accept(psiElementVisitor);
    }

    /**
     * Visit all possible elements for render clements, scope shop be the class or a file itself
     */
    public static void visitRenderTemplateFunctions(@NotNull PsiElement context, @NotNull Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
        context.accept(new TemplateRenderPsiRecursiveElementWalkingVisitor(context, consumer));
    }

    private static class TemplateRenderPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final PsiElement context;
        private final Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer;

        TemplateRenderPsiRecursiveElementWalkingVisitor(PsiElement context, Consumer<Triple<String, PhpNamedElement, FunctionReference>> consumer) {
            this.context = context;
            this.consumer = consumer;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if(element instanceof MethodReference) {
                visitMethodReference((MethodReference) element);
            } else if(element instanceof PhpDocTag) {
                visitPhpDocTag((PhpDocTag) element);
            } else if(element instanceof PhpAttributesList) {
                visitPhpAttribute((PhpAttributesList) element);
            }
            super.visitElement(element);
        }

        private void visitPhpAttribute(@NotNull PhpAttributesList phpAttributesList) {
            Collection<@NotNull PhpAttribute> attributes = phpAttributesList.getAttributes(TwigUtil.TEMPLATE_ANNOTATION_CLASS);
            for (PhpAttribute attribute : attributes) {
                if (attribute.getArguments().isEmpty()) {
                    // #[@Template()]
                    PsiElement parent = phpAttributesList.getParent();
                    if (parent instanceof Method) {
                        visitMethodForGuessing((Method) parent);
                    }
                } else {
                    // [@Template("foobar.html.twig")]
                    // #[@Template(template: "foobar.html.twig")]
                    String template = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "template");
                    if (StringUtils.isNotBlank(template)) {
                        PsiElement parent = phpAttributesList.getParent();
                        if (parent instanceof Method) {
                            addTemplateWithScope(template, (Method) parent, null);

                        }
                    }
                }
            }
        }

        private void visitMethodReference(@NotNull MethodReference methodReference) {
            String methodName = methodReference.getName();
            if (methodName == null) {
                return;
            }

            if (!methodName.toLowerCase().contains("render")) {
                return;
            }

            PsiElement[] parameters = methodReference.getParameters();
            if(parameters.length == 0) {
                return;
            }

            if (parameters[0] instanceof StringLiteralExpression) {
                addStringLiteralScope(methodReference, (StringLiteralExpression) parameters[0]);
            } else if(parameters[0] instanceof TernaryExpression) {
                // render(true === true ? 'foo.twig.html' : 'foobar.twig.html')
                for (PhpPsiElement phpPsiElement : new PhpPsiElement[]{((TernaryExpression) parameters[0]).getTrueVariant(), ((TernaryExpression) parameters[0]).getFalseVariant()}) {
                    if (phpPsiElement == null) {
                        continue;
                    }

                    if (phpPsiElement instanceof StringLiteralExpression) {
                        addStringLiteralScope(methodReference, (StringLiteralExpression) phpPsiElement);
                    } else if(phpPsiElement instanceof PhpReference) {
                        resolvePhpReference(methodReference, phpPsiElement);
                    }
                }
            } else if(parameters[0] instanceof AssignmentExpression) {
                // $this->render($template = 'foo.html.twig')
                PhpPsiElement value = ((AssignmentExpression) parameters[0]).getValue();
                if(value instanceof StringLiteralExpression) {
                    addStringLiteralScope(methodReference, (StringLiteralExpression) value);
                }
            } else if(parameters[0] instanceof PhpReference) {
                resolvePhpReference(methodReference, parameters[0]);
            } else if(parameters[0] instanceof BinaryExpression) {
                // render($foo ?? 'foo.twig.html')
                PsiElement phpPsiElement = ((BinaryExpression) parameters[0]).getRightOperand();

                if (phpPsiElement instanceof StringLiteralExpression) {
                    addStringLiteralScope(methodReference, (StringLiteralExpression) phpPsiElement);
                } else if(phpPsiElement instanceof PhpReference) {
                    resolvePhpReference(methodReference, phpPsiElement);
                }
            }
        }

        private void resolvePhpReference(@NotNull MethodReference methodReference, PsiElement parameter) {
            for (PhpNamedElement phpNamedElement : ((PhpReference) parameter).resolveLocal()) {
                // foo(self::foo)
                // foo($this->foo)
                if (phpNamedElement instanceof Field) {
                    PsiElement defaultValue = ((Field) phpNamedElement).getDefaultValue();
                    if (defaultValue instanceof StringLiteralExpression) {
                        addStringLiteralScope(methodReference, (StringLiteralExpression) defaultValue);
                    }
                } else if (phpNamedElement instanceof Variable) {
                    // foo($var) => $var = 'test.html.twig'
                    PsiElement assignmentExpression = phpNamedElement.getParent();
                    if (assignmentExpression instanceof AssignmentExpression) {
                        PhpPsiElement value = ((AssignmentExpression) assignmentExpression).getValue();
                        if (value instanceof StringLiteralExpression) {
                            addStringLiteralScope(methodReference, (StringLiteralExpression) value);
                        }
                    }
                } else if (phpNamedElement instanceof Parameter) {
                    // function foobar($defaultParameter = 'default-function-parameter.html.twig')
                    PsiElement value = ((Parameter) phpNamedElement).getDefaultValue();
                    if (value instanceof StringLiteralExpression) {
                        addStringLiteralScope(methodReference, (StringLiteralExpression) value);
                    }
                }
            }
        }

        private void addStringLiteralScope(@NotNull MethodReference methodReference, @NotNull StringLiteralExpression defaultValue) {
            String contents = defaultValue.getContents();
            if (StringUtils.isBlank(contents) || !contents.endsWith(".twig")) {
                return;
            }

            Function parentOfType = PsiTreeUtil.getParentOfType(methodReference, Function.class);
            if (parentOfType == null) {
                return;
            }

            addTemplateWithScope(contents, parentOfType, methodReference);
        }

        /**
         * "@Template("foobar.html.twig")"
         * "@Template(template="foobar.html.twig")"
         */
        private void visitPhpDocTag(@NotNull PhpDocTag phpDocTag) {
            // "@var" and user non related tags dont need an action
            if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                return;
            }

            // init scope imports
            Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(phpDocTag);
            if(fileImports.size() == 0) {
                return;
            }

            String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
            if(!StringUtils.stripStart(TwigUtil.TEMPLATE_ANNOTATION_CLASS, "\\").equals(StringUtils.stripStart(annotationFqnName, "\\"))) {
                return;
            }

            String template = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
            if (template == null) {
                // see \Sensio\Bundle\FrameworkExtraBundle\Templating\TemplateGuesser
                // App\Controller\MyNiceController::myAction => my_nice/my.html.twig
                Method methodScope = AnnotationBackportUtil.getMethodScope(phpDocTag);
                if(methodScope != null) {
                    visitMethodForGuessing(methodScope);
                }
            } else if(template.endsWith(".twig")) {
                Method methodScope = AnnotationBackportUtil.getMethodScope(phpDocTag);
                if(methodScope != null) {
                    addTemplateWithScope(template, methodScope, null);
                }
            }
        }

        private void visitMethodForGuessing(@NotNull Method methodScope) {
            PhpClass phpClass = methodScope.getContainingClass();
            if (phpClass != null) {
                // App\Controller\  "MyNice"  Controller
                Matcher matcher = Pattern.compile("Controller\\\\(.+)Controller$", Pattern.MULTILINE).matcher(StringUtils.stripStart(phpClass.getFQN(), "\\"));
                if(matcher.find()){
                    String group = underscore(matcher.group(1).replace("\\", "/"));
                    String name = methodScope.getName();

                    // __invoke is using controller as template name
                    if (name.equals("__invoke")) {
                        addTemplateWithScope(group + ".html.twig", methodScope, null);
                    } else {
                        String action = name.endsWith("Action") ? name.substring(0, name.length() - "Action".length()) : name;
                        addTemplateWithScope(group + "/" + underscore(action) + ".html.twig", methodScope, null);
                    }
                }
            }
        }

        private void addTemplateWithScope(@NotNull String contents, @NotNull PhpNamedElement scope, @Nullable FunctionReference functionReference) {
            String s = TwigUtil.normalizeTemplateName(contents);
            consumer.accept(new Triple<>(s, scope, functionReference));
        }
    }
}
