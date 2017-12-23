package fr.adrienbrault.idea.symfony2plugin.security.utils;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class VoterUtil {

    public static void visitAttribute(@NotNull Project project, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses("Symfony\\Component\\Security\\Core\\Authorization\\Voter\\Voter")) {
            Method supports = phpClass.findMethodByName("supports");
            if(supports != null) {
                visitAttribute(supports, consumer);
            }

            Method voteOnAttribute = phpClass.findMethodByName("voteOnAttribute");
            if(voteOnAttribute != null) {
                visitAttribute(voteOnAttribute, consumer);
            }
        }

        for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses("Symfony\\Component\\Security\\Core\\Authorization\\Voter\\VoterInterface")) {
            Method vote = phpClass.findMethodByName("vote");
            if(vote != null) {
                visitAttributeForeach(vote, consumer);
            }
        }

        for (String files : new String[]{"security.yml", "security.yaml"}) {
            for (PsiFile psiFile : FilenameIndex.getFilesByName(project, files, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), YAMLFileType.YML))) {
                if(!(psiFile instanceof YAMLFile)) {
                    continue;
                }

                YAMLKeyValue roleHierarchy = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, "security", "role_hierarchy");
                if(roleHierarchy != null) {
                    YAMLValue value = roleHierarchy.getValue();
                    if(!(value instanceof YAMLMapping)) {
                        continue;
                    }

                    for (YAMLPsiElement yamlPsiElement : value.getYAMLElements()) {
                        if(!(yamlPsiElement instanceof YAMLKeyValue)) {
                            continue;
                        }

                        String keyText = ((YAMLKeyValue) yamlPsiElement).getKeyText();
                        if(StringUtils.isNotBlank(keyText)) {
                            consumer.accept(Pair.create(keyText, yamlPsiElement));
                        }

                        YAMLValue yamlValue = ((YAMLKeyValue) yamlPsiElement).getValue();
                        if(yamlValue instanceof YAMLSequence) {
                            for (String item : YamlHelper.getYamlArrayValuesAsString((YAMLSequence) yamlValue)) {
                                consumer.accept(Pair.create(item, yamlValue));
                            }
                        }
                    }
                }

                YAMLKeyValue accessControl = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, "security", "access_control");
                if(accessControl != null) {
                    YAMLValue value = accessControl.getValue();
                    if(!(value instanceof YAMLSequence)) {
                        continue;
                    }

                    for (YAMLPsiElement yamlPsiElement : value.getYAMLElements()) {
                        if(!(yamlPsiElement instanceof YAMLSequenceItem)) {
                            continue;
                        }

                        YAMLValue value1 = ((YAMLSequenceItem) yamlPsiElement).getValue();
                        if(!(value1 instanceof YAMLMapping)) {
                            continue;
                        }

                        YAMLKeyValue roles = ((YAMLMapping) value1).getKeyValueByKey("roles");
                        if(roles == null) {
                            continue;
                        }

                        YAMLValue value2 = roles.getValue();
                        if(value2 instanceof YAMLScalar) {
                            // roles: FOOBAR
                            String textValue = ((YAMLScalar) value2).getTextValue();
                            if(StringUtils.isNotBlank(textValue)) {
                                consumer.accept(Pair.create(textValue, value2));
                            }
                        } else if(value2 instanceof YAMLSequence) {
                            // roles: [FOOBAR, FOOBAR_1]
                            for (String item : YamlHelper.getYamlArrayValuesAsString((YAMLSequence) value2)) {
                                consumer.accept(Pair.create(item, value2));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void visitAttributeForeach(@NotNull Method method, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        Parameter[] parameters = method.getParameters();
        if(parameters.length < 3) {
            return;
        }

        for (Variable variable : PhpElementsUtil.getVariablesInScope(method, parameters[2])) {
            // foreach ($attributes as $attribute)
            PsiElement psiElement = PsiTreeUtil.nextVisibleLeaf(variable);
            if(psiElement != null && psiElement.getNode().getElementType() == PhpTokenTypes.kwAS) {
                PsiElement parent = variable.getParent();
                if(!(parent instanceof ForeachStatement)) {
                    continue;
                }

                PhpPsiElement variableDecl = variable.getNextPsiSibling();
                if(variableDecl instanceof Variable) {
                    for (Variable variable1 : PhpElementsUtil.getVariablesInScope(parent, (Variable) variableDecl)) {
                        visitVariable(variable1, consumer);
                    }
                }
            }

            // in_array('foobar', $attributes)
            PsiElement parameterList = variable.getParent();
            if(parameterList instanceof ParameterList && PsiElementUtils.getParameterIndexValue(variable) == 1) {
                PsiElement functionCall = parameterList.getParent();
                if(functionCall instanceof FunctionReference && "in_array".equalsIgnoreCase(((FunctionReference) functionCall).getName())) {
                    PsiElement[] functionParameter = ((ParameterList) parameterList).getParameters();
                    if(functionParameter.length > 0) {
                        String stringValue = PhpElementsUtil.getStringValue(functionParameter[0]);
                        if(stringValue != null && StringUtils.isNotBlank(stringValue)) {
                            consumer.accept(Pair.create(stringValue, functionParameter[0]));
                        }
                    }
                }
            }
        }
    }

    private static void visitAttribute(@NotNull Method method, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        Parameter[] parameters = method.getParameters();
        if(parameters.length == 0) {
            return;
        }

        for (Variable variable : PhpElementsUtil.getVariablesInScope(method, parameters[0])) {
            visitVariable(variable, consumer);
        }
    }

    public static class StringPairConsumer implements Consumer<Pair<String, PsiElement>> {
        private Set<String> values = new HashSet<>();

        @Override
        public void accept(Pair<String, PsiElement> pair) {
            values.add(pair.getFirst());
        }

        @NotNull
        public Set<String> getValues() {
            return values;
        }
    }

    public static class TargetPairConsumer implements Consumer<Pair<String, PsiElement>> {
        @NotNull
        private final String filterValue;

        @NotNull
        private Set<PsiElement> values = new HashSet<>();

        public TargetPairConsumer(@NotNull String filterValue) {
            this.filterValue = filterValue;
        }

        @Override
        public void accept(Pair<String, PsiElement> pair) {
            if(pair.getFirst().equalsIgnoreCase(filterValue)) {
                values.add(pair.getSecond());
            }
        }

        @NotNull
        public Set<PsiElement> getValues() {
            return values;
        }
    }

    public static class LookupElementPairConsumer implements Consumer<Pair<String, PsiElement>> {
        @NotNull
        private final Set<String> elements = new HashSet<>();

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            return lookupElements;
        }

        @Override
        public void accept(Pair<String, PsiElement> pair) {
            String name = pair.getFirst();
            if (!elements.contains(name)) {
                LookupElementBuilder lookupElement = LookupElementBuilder.create(name).withIcon(Symfony2Icons.SYMFONY);

                PhpClass phpClass = PsiTreeUtil.getParentOfType(pair.getSecond(), PhpClass.class);
                if (phpClass != null) {
                    lookupElement = lookupElement.withTypeText(phpClass.getName(), true);
                }

                lookupElements.add(lookupElement);

                elements.add(name);
            }
        }

        @NotNull
        private final Collection<LookupElement> lookupElements = new ArrayList<>();
    }

    /**
     * Find security roles on Voter implementation and security roles in Yaml
     */
    private static void visitVariable(@NotNull Variable resolve, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        PsiElement parent = resolve.getParent();
        if(parent instanceof BinaryExpression) {
            // 'VALUE' == $var
            PsiElement rightElement = PsiTreeUtil.prevVisibleLeaf(resolve);
            if(rightElement != null) {
                IElementType node = rightElement.getNode().getElementType();
                if(isIfOperand(node)) {
                    PsiElement leftOperand = ((BinaryExpression) parent).getLeftOperand();
                    String stringValue = PhpElementsUtil.getStringValue(leftOperand);
                    if(StringUtils.isNotBlank(stringValue)) {
                        consumer.accept(Pair.create(stringValue, leftOperand));
                    }
                }
            }

            // $var == 'VALUE'
            PsiElement leftElement = PsiTreeUtil.nextVisibleLeaf(resolve);
            if(leftElement != null) {
                IElementType node = leftElement.getNode().getElementType();
                if(isIfOperand(node)) {
                    PsiElement rightOperand = ((BinaryExpression) parent).getRightOperand();
                    String stringValue = PhpElementsUtil.getStringValue(rightOperand);
                    if(StringUtils.isNotBlank(stringValue)) {
                        consumer.accept(Pair.create(stringValue, rightOperand));
                    }
                }
            }
        } else if(parent instanceof ParameterList && PsiElementUtils.getParameterIndexValue(resolve) == 0) {
            // in_array($caret, X)
            PsiElement functionCall = parent.getParent();
            if(functionCall instanceof FunctionReference && "in_array".equalsIgnoreCase(((FunctionReference) functionCall).getName())) {
                PsiElement[] functionParameter = ((ParameterList) parent).getParameters();
                if(functionParameter.length > 1) {
                    if(functionParameter[1] instanceof ArrayCreationExpression) {
                        // in_array($x, ['FOOBAR'])
                        PsiElement[] psiElements = PsiTreeUtil.collectElements(functionParameter[1], psiElement -> psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE);
                        for (PsiElement psiElement : psiElements) {
                            PsiElement firstChild = psiElement.getFirstChild();
                            String stringValue = PhpElementsUtil.getStringValue(firstChild);
                            if(StringUtils.isNotBlank(stringValue)) {
                                consumer.accept(Pair.create(stringValue, firstChild));
                            }
                        }
                    } else if(functionParameter[1] instanceof MemberReference) {
                        // in_array($attribute, self::FOO);
                        // in_array($attribute, $this->foo);
                        PsiElement PsiReference = ((PsiReference) functionParameter[1]).resolve();
                        if(PsiReference instanceof Field) {
                            PsiElement defaultValue = ((Field) PsiReference).getDefaultValue();
                            if(defaultValue instanceof ArrayCreationExpression) {
                                for (String s : PhpElementsUtil.getArrayValuesAsString((ArrayCreationExpression) defaultValue)) {
                                    consumer.accept(Pair.create(s, defaultValue));
                                }
                            }
                        }
                    }
                }
            }
        } else if(parent instanceof PhpSwitch) {
            // case "foobar":
            for (PhpCase phpCase : ((PhpSwitch) parent).getAllCases()) {
                PhpPsiElement condition = phpCase.getCondition();
                String stringValue = PhpElementsUtil.getStringValue(condition);
                if(StringUtils.isNotBlank(stringValue)) {
                    consumer.accept(Pair.create(stringValue, condition));
                }
            }
        }
    }

    /**
     * null == null, null != null, null === null
     */
    private static boolean isIfOperand(@NotNull IElementType node) {
        return
            node == PhpTokenTypes.opIDENTICAL ||
                node == PhpTokenTypes.opEQUAL ||
                node == PhpTokenTypes.opNOT_EQUAL ||
                node == PhpTokenTypes.opNOT_IDENTICAL
            ;
    }
}
