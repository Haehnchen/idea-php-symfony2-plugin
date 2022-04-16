package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.refactoring.PhpRefactoringUtil;
import com.jetbrains.php.refactoring.changeSignature.PhpChangeSignatureProcessor;
import com.jetbrains.php.refactoring.changeSignature.PhpParameterInfo;
import com.jetbrains.php.refactoring.introduce.introduceField.PhpIntroduceFieldHandler;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import kotlin.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IncompletePropertyServiceInjectionContributor extends CompletionContributor {
    private static final PatternCondition<FieldReference> THIS_FIELD_NAME_PATTERN = new PatternCondition<>("this pattern") {
        @Override
        public boolean accepts(@NotNull FieldReference fieldReference, ProcessingContext context) {
            PhpExpression classReference = fieldReference.getClassReference();
            if (classReference instanceof Variable && "this".equals(classReference.getName())) {
                return true;
            }

            return false;
        }
    };

    @NotNull
    private PsiElementPattern.Capture<PsiElement> getThisFieldNamePattern() {
        return PlatformPatterns.psiElement().withElementType(PhpTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(FieldReference.class).with(THIS_FIELD_NAME_PATTERN)
            );
    }

    public IncompletePropertyServiceInjectionContributor() {
        extend(CompletionType.BASIC, getThisFieldNamePattern(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                    return;
                }

                result.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1));

                String completedText = getCompletedText(completionParameters);
                if (completedText == null || completedText.length() < 2) {
                    return;
                }

                PsiElement originalPosition = completionParameters.getOriginalPosition();
                PhpClass phpClassScope = PsiTreeUtil.getParentOfType(originalPosition, PhpClass.class);
                if (phpClassScope == null || !ServiceUtil.isPhpClassAService(phpClassScope)) {
                    return;
                }

                Method constructor = phpClassScope.getConstructor();
                if (constructor != null && constructor.getAccess() != PhpModifier.Access.PUBLIC) {
                    return;
                }

                Set<String> fields = new HashSet<>();
                Set<String> types = new HashSet<>();

                for (Field field : phpClassScope.getFields()) {
                    if (!field.isConstant()) {
                        fields.add(field.getName().toLowerCase());
                        types.addAll(field.getType().getTypes()
                            .stream()
                            .flatMap((f) -> PhpIndex.getInstance(phpClassScope.getProject()).getAnyByFQN(f).stream())
                            .distinct()
                            .map(PhpNamedElement::getFQN)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()));
                    }
                }

                if (constructor != null) {
                    for (Parameter parameter : constructor.getParameters()) {
                        fields.add(parameter.getName().toLowerCase());

                        types.addAll(parameter.getType().getTypes()
                            .stream()
                            .flatMap((f) -> PhpIndex.getInstance(phpClassScope.getProject()).getAnyByFQN(f).stream())
                            .distinct()
                            .map(PhpNamedElement::getFQN)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()));
                    }
                }

                Collection<Triple<String, String, String[]>> completed = new ArrayList<>();

                for (Triple<String, String[], String[]> injection : getInjectionService(originalPosition.getProject())) {
                    for (String fqn : injection.getSecond()) {
                        if (types.contains(fqn.toLowerCase())) {
                            continue;
                        }

                        String propertyName = injection.getFirst();
                        if (propertyName == null) {
                            int i = fqn.lastIndexOf("\\");
                            if (i > 0) {
                                propertyName = fqn.substring(i + 1);
                            } else {
                                propertyName = fqn;
                            }

                            propertyName = StringUtils.removeEndIgnoreCase(propertyName, "interface");
                            propertyName = StringUtils.removeEndIgnoreCase(propertyName, "abstract");
                            propertyName = StringUtils.removeEndIgnoreCase(propertyName, "factory");

                            // propertyName = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(propertyName);
                            propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                        }

                        if (StringUtils.isBlank(propertyName) || !propertyName.toLowerCase().startsWith(completedText.toLowerCase()) || fields.contains(propertyName.toLowerCase())) {
                            continue;
                        }

                        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(originalPosition.getProject()).getAnyByFQN(fqn);
                        if (anyByFQN.size() == 0) {
                            continue;
                        }

                        LookupElementBuilder lookupElementBuilder = LookupElementBuilder.createWithSmartPointer(propertyName, phpClassScope)
                            .withInsertHandler(new LookupElementInsertHandler(propertyName, fqn))
                            .withIcon(Symfony2Icons.SERVICE_OPACITY)
                            .withTypeText(StringUtils.stripStart(fqn, "\\"), true);

                        result.addElement(lookupElementBuilder);

                        completed.add(new Triple<>(propertyName, fqn, injection.getThird()));

                        break;
                    }
                }

                if (completed.size() <= 5) {
                    int maxMethodUntilUnsureCompletion = 5;

                    if (completed.size() <= 2) {
                        maxMethodUntilUnsureCompletion = 15;
                    } else if(completed.size() <= 3) {
                        maxMethodUntilUnsureCompletion = 10;
                    }

                    for (Triple<String, String, String[]> injection : completed) {
                        String fqn = injection.getSecond();

                        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(originalPosition.getProject()).getAnyByFQN(fqn);
                        if (anyByFQN.size() == 0) {
                            continue;
                        }

                        String propertyName = injection.getFirst();

                        PhpClass next = anyByFQN.iterator().next();

                        Set<Method> methods = next.getMethods()
                            .stream()
                            .filter(method -> !method.isStatic() && method.getAccess() == PhpModifier.Access.PUBLIC && !method.isDeprecated())
                            .collect(Collectors.toSet());

                        // whitelisted
                        Set<Method> whitelisted = methods.stream()
                            .filter(method -> Arrays.stream(injection.getThird()).anyMatch(s -> method.getName().equalsIgnoreCase(s)))
                            .collect(Collectors.toSet());

                        Set<String> whitelistedMethodNames = new HashSet<>();
                        for (Method method : whitelisted) {
                            appendMethod(result, phpClassScope, fqn, propertyName, method);
                            whitelistedMethodNames.add(method.getName().toLowerCase());
                        }

                        if (methods.size() <= maxMethodUntilUnsureCompletion) {
                            for (Method method : methods) {
                                if (whitelistedMethodNames.contains(method.getName())) {
                                    continue;
                                }

                                appendMethod(result, phpClassScope, fqn, propertyName, method);
                            }
                        }
                    }
                }
            }

            private void appendMethod(@NotNull CompletionResultSet result, PhpClass parentOfType, String fqn, String propertyName, Method method) {
                LookupElementBuilder lookupElementBuilder = LookupElementBuilder.createWithSmartPointer(propertyName + "->" + method.getName() + "();", parentOfType)
                    .withPresentableText(propertyName + "->" + method.getName())
                    .withInsertHandler(new LookupElementInsertHandler(propertyName, fqn))
                    .withIcon(Symfony2Icons.SERVICE_OPACITY)
                    .withTypeText(StringUtils.stripStart(fqn, "\\"), true);

                Parameter[] parameters = method.getParameters();
                if (parameters.length > 0) {
                    lookupElementBuilder = lookupElementBuilder.withTailText(PhpPresentationUtil.formatParameters(null, method.getParameters()).toString(), true);
                }

                result.addElement(lookupElementBuilder);
            }
        });
    }

    private static Collection<Triple<String, String[], String[]>> getInjectionService(@NotNull Project project) {
        // @TODO: fill this list based on project usage
        return new ArrayList<>() {{
            add(new Triple<>(null, new String[] {"\\Symfony\\Contracts\\Translation\\TranslatorInterface", "Symfony\\Component\\Translation\\TranslatorInterface"}, new String[0]));
            add(new Triple<>(null, new String[] {"\\Symfony\\Component\\HttpFoundation\\RequestStack"}, new String[0]));
            add(new Triple<>(null, new String[] {"\\Twig\\Environment"}, new String[] {"render"}));
            add(new Triple<>("twig", new String[] {"\\Twig\\Environment"}, new String[] {"render"}));
            add(new Triple<>(null, new String[] {"\\Psr\\Log\\LoggerInterface"}, new String[] {"error", "debug", "info"}));
            add(new Triple<>(null, new String[] {"\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface"}, new String[] {"generate"}));
            add(new Triple<>(null, new String[] {"\\Doctrine\\ORM\\EntityManagerInterface", "\\Doctrine\\Persistence\\ObjectManager"}, new String[] {"flush", "find", "remove", "persist", "getRepository"}));
        }};
    }

    @Nullable
    private String getCompletedText(@NotNull CompletionParameters completionParameters) {
        PsiElement originalPosition = completionParameters.getOriginalPosition();
        if (originalPosition != null) {
            String text = originalPosition.getText();
            if (text.length() > 0) {
                return text;
            }
        }

        PsiElement position = completionParameters.getPosition();
        String text = position.getText().toLowerCase().replace("intellijidearulezzz", "");
        if (text.length() > 0) {
            return text;
        }

        return null;
    }

    private static class LookupElementInsertHandler implements InsertHandler<LookupElement> {
        private final String propertyName;
        private final String typePhpClass;

        public LookupElementInsertHandler(String propertyName, String typePhpClass) {
            this.propertyName = propertyName;
            this.typePhpClass = typePhpClass;
        }

        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
            SmartPsiElementPointer<PhpClass> parentOfType2 = (SmartPsiElementPointer<PhpClass>) item.getObject();
            PhpClass parentOfType = parentOfType2.getElement();
            if (parentOfType == null) {
                return;
            }

            Method constructor = PhpIntroduceFieldHandler.getOrCreateConstructor(parentOfType);
            if (constructor == null) {
                return;
            }

            // use + constructor(Foo $foo)
            String importedClass = PhpElementsUtil.insertUseIfNecessary(parentOfType, typePhpClass);
            PhpParameterInfo phpParameterInfo = new PhpParameterInfo(0, propertyName);
            phpParameterInfo.setType(new PhpType().add(typePhpClass), importedClass);

            // find added parameter; should mmostly the last
            Collection<PhpParameterInfo> parameterInfos = List.of(phpParameterInfo);
            PhpChangeSignatureProcessor.addParameterToFunctionSignature(parentOfType.getProject(), constructor, parameterInfos);
            Parameter parameter = Arrays.stream(constructor.getParameters())
                .filter(parameter1 -> propertyName.equalsIgnoreCase(parameter1.getName()))
                .findFirst()
                .orElse(null);

            // add $this->foo
            // readonly, constructor property promotion currently not supported; or handled automatically by code
            if (parameter != null) {
                PhpRefactoringUtil.initializeFieldsByParameters(parentOfType, List.of(parameter), PhpModifier.Access.PRIVATE);
            }

            // move caret inside the function
            String lookupString = item.getLookupString();
            if (lookupString.endsWith(");")) {
                context.getEditor().getCaretModel().moveCaretRelatively(-2, 0, false, false, true);
            }
        }
    }
}
