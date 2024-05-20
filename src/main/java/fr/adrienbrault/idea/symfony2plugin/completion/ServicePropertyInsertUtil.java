package fr.adrienbrault.idea.symfony2plugin.completion;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.config.PhpLanguageFeature;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.lang.formatter.PhpCodeStyleSettings;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpPromotedFieldParameterImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.refactoring.PhpNameStyle;
import com.jetbrains.php.refactoring.PhpNameUtil;
import com.jetbrains.php.refactoring.PhpRefactoringUtil;
import com.jetbrains.php.refactoring.changeSignature.PhpChangeSignatureProcessor;
import com.jetbrains.php.refactoring.changeSignature.PhpParameterInfo;
import com.jetbrains.php.refactoring.introduce.introduceField.PhpIntroduceFieldHandler;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServicePropertyInsertUtil {
    private static final String[] CLASS_TYPE_NAMES = {"interface", "abstract", "decorator"};

    public static List<String> getInjectionService(@NotNull Project project, @NotNull String propertyNameFind) {
        return getInjectionService(project, propertyNameFind, null);
    }

    public static List<String> getInjectionService(@NotNull Project project, @NotNull String propertyNameFindRaw, @Nullable String methodName) {
        // @TODO: fill this list based on project usage

        final Set<String> propertyNameFind = new HashSet<>();
        propertyNameFind.add(normalizeClassTypeKeywords(propertyNameFindRaw));

        // LoggerInterface $fooBarLogger
        if (propertyNameFindRaw.endsWith("Logger") && !propertyNameFindRaw.equalsIgnoreCase("logger")) {
            propertyNameFind.add("logger");
        }

        Map<String, Match> servicesMatch = new HashMap<>();

        HashMap<String, String> alias = new HashMap<>() {{
            put("twig", "\\Twig\\Environment");
            put("template", "\\Twig\\Environment");
            put("router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");
            put("em", "Doctrine\\ORM\\EntityManagerInterface");
            put("om", "\\Doctrine\\Persistence\\ObjectManager");
        }};

        for (String property : propertyNameFind) {
            if (alias.containsKey(property.toLowerCase())) {
                String key = property.toLowerCase();
                if (!PhpIndex.getInstance(project).getAnyByFQN(alias.get(key)).isEmpty()) {
                    String fqn = alias.get(key);
                    servicesMatch.put(fqn, new Match(fqn, 4));
                }
            }
        }

        // try to find partial ending match for normalized properties: fooBarCar => barCar
        String classPropertyNameForEndingMatch = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(StringUtils.strip(propertyNameFindRaw, "_"));
        for (String replace : CLASS_TYPE_NAMES) {
            classPropertyNameForEndingMatch = StringUtils.removeEndIgnoreCase(classPropertyNameForEndingMatch, replace);
            classPropertyNameForEndingMatch = StringUtils.removeStartIgnoreCase(classPropertyNameForEndingMatch, replace);
        }

        classPropertyNameForEndingMatch = fr.adrienbrault.idea.symfony2plugin.util.StringUtils.camelize(classPropertyNameForEndingMatch, true);

        // collect partial match with least 3 parts
        Set<String> endingMatches = new HashSet<>();
        List<String> nameParts = PhpNameUtil.splitName(classPropertyNameForEndingMatch);
        if (nameParts.size() > 2) {
            PhpCodeStyleSettings settings = CodeStyle.getCustomSettings(PhpPsiElementFactory.createPsiFileFromText(project, "<?php"), PhpCodeStyleSettings.class);
            endingMatches.addAll(PhpNameStyle.DECAPITALIZE.withStyle(settings.VARIABLE_NAMING_STYLE).generateNames(nameParts)
                .stream()
                .filter(s -> fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(s).split("_").length > 2)
                .collect(Collectors.toSet())
            );
        }

        HashSet<String> classes = new HashSet<>();

        classes.addAll(PhpIndex.getInstance(project).getAllClassFqns(NonGarbageClassPrefixMatcher.INSTANCE));
        classes.addAll(PhpIndex.getInstance(project).getAllInterfacesFqns(NonGarbageClassPrefixMatcher.INSTANCE));

        for (String fqn : classes) {
            // Bar\Foo => Foo
            int i = fqn.lastIndexOf("\\");
            String classPropertyNameRaw = i > 0
                ? fqn.substring(i + 1)
                : fqn;

            String classPropertyName = normalizeClassTypeKeywords(classPropertyNameRaw);
            if (StringUtils.isBlank(classPropertyName)) {
                continue;
            }

            int weight;
            if (propertyNameFind.stream().anyMatch(classPropertyName::equalsIgnoreCase)) {
                // direct property match
                weight = 3;
            } else if(endingMatches.stream().anyMatch(s -> classPropertyName.toLowerCase().endsWith(s.toLowerCase()))) {
                // partial property with ending match
                weight = 1;
            } else {
                continue;
            }

            Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(fqn);
            if (anyByFQN.isEmpty()) {
                continue;
            }

            if (methodName != null && !hasMethodMatch(methodName, anyByFQN)) {
                weight -= 4;
            }

            if (anyByFQN.stream().anyMatch(PhpClass::isInterface)) {
                weight += 2;

                // Symfony\Contracts\EventDispatcher\EventDispatcherInterface
                // Psr\Log\LoggerInterface
                if (fqn.toLowerCase().contains("\\contracts\\") && fqn.toLowerCase().contains("\\symfony\\")) {
                    weight += 2;
                } else if(fqn.toLowerCase().contains("\\psr\\")) {
                    weight += 3;
                }
            }

            if (anyByFQN.stream().anyMatch(PhpClass::isAbstract)) {
                weight += 1;
            }

            if (classPropertyNameRaw.toLowerCase().contains("decorator")) {
                weight -= 3;
            }

            if (servicesMatch.containsKey(fqn)) {
                servicesMatch.get(fqn).modifyWeight(weight);
            } else {
                servicesMatch.put(fqn, new Match(fqn, weight));
            }
        }

        return servicesMatch.values().stream()
            .sorted((o1, o2) -> Integer.compare(o2.weight, o1.weight))
            .map(m -> m.fqn)
            .collect(Collectors.toList());
    }

    /**
     * Filter some invalid classes fqn
     * - RectorPrefix2...
     * - _PHPStan_f12ae...
     */
    private static class NonGarbageClassPrefixMatcher extends PrefixMatcher {
        private static final NonGarbageClassPrefixMatcher INSTANCE = new NonGarbageClassPrefixMatcher();

        private NonGarbageClassPrefixMatcher() {
            super("");
        }

        @Override
        public boolean prefixMatches(@NotNull String fqn) {
            if (fqn.contains("\\") && fqn.toLowerCase().contains("\\test\\")) {
                return false;
            }

            return !fqn.endsWith("Test")
                && !fqn.toLowerCase().contains("_phpstan_")
                && !fqn.toLowerCase().contains("ecsprefix")
                && !fqn.toLowerCase().contains("_humbugbox")
                && !fqn.toLowerCase().contains("rectorprefix");
        }

        @Override
        public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
            return new NonGarbageClassPrefixMatcher();
        }
    }

    private static class Match {
        private final String fqn;
        private int weight = 0;

        public Match(@NotNull String fqn, int weight) {
            this.fqn = fqn;
            this.modifyWeight(weight);
        }

        public void modifyWeight(int weight) {
            this.weight += weight;
        }
    }

    private static boolean hasMethodMatch(@NotNull String methodName, Collection<PhpClass> anyByFQN) {
        return anyByFQN.stream()
            .anyMatch(phpClass -> phpClass.findMethodByName(methodName) != null);
    }

    private static String normalizeClassTypeKeywords(@NotNull String classPropertyName) {
        classPropertyName = classPropertyName.replaceAll("_", "").toLowerCase();

        for (String replace : CLASS_TYPE_NAMES) {
            classPropertyName = StringUtils.removeEndIgnoreCase(classPropertyName, replace);
            classPropertyName = StringUtils.removeStartIgnoreCase(classPropertyName, replace);
        }

        return classPropertyName;
    }

    public static void appendPropertyInjection(@NotNull PhpClass phpClass, @NotNull String propertyName, @NotNull String typePhpClass) {
        Method constructor = PhpIntroduceFieldHandler.getOrCreateConstructor(phpClass);
        if (constructor == null) {
            return;
        }

        // use + constructor(Foo $foo)
        String importedClass = PhpElementsUtil.insertUseIfNecessary(phpClass, typePhpClass);

        // "private readonly Foo $foo"
        if (shouldUsePropertyPromotion(constructor)) {
            String readonlyProperty = !phpClass.isReadonly() ? "readonly " : "";

            Parameter parameter = PhpPsiElementFactory.createComplexParameter(phpClass.getProject(), String.format("private %s%s $%s", readonlyProperty, importedClass, propertyName));
            Parameter parameterToInsertAfter = PhpChangeSignatureProcessor.findParameterToInsertAfter(constructor);
            if (parameterToInsertAfter != null) {
                addParameterAfter(constructor, parameter, parameterToInsertAfter);
            } else if (constructor.getParameters().length == 0) {
                PhpChangeSignatureProcessor.appendParameterToParameterList(constructor, parameter);
            }

            return;
        }

        PhpParameterInfo phpParameterInfo = new PhpParameterInfo(0, propertyName);
        phpParameterInfo.setType(new PhpType().add(typePhpClass), importedClass);

        // find added parameter; should mostly the last
        PhpChangeSignatureProcessor.addParameterToFunctionSignature(phpClass.getProject(), constructor, List.of(phpParameterInfo));

        Parameter parameter = Arrays.stream(constructor.getParameters())
            .filter(parameter1 -> propertyName.equalsIgnoreCase(parameter1.getName()))
            .findFirst()
            .orElse(null);

        // add $this->foo
        if (parameter != null) {
            PhpRefactoringUtil.initializeFieldsByParameters(phpClass, List.of(parameter), PhpModifier.Access.PRIVATE);
        }
    }

    private static void addParameterAfter(@NotNull Function function, @NotNull Parameter parameter, @NotNull Parameter parameterToInsertAfter) {
        PsiElement parameterList = PhpPsiUtil.getChildOfType(function, PhpElementTypes.PARAMETER_LIST);
        assert parameterList != null;
        parameterList.addAfter(parameter, parameterList.addAfter(PhpPsiElementFactory.createComma(parameterList.getProject()), parameterToInsertAfter));
    }

    public static boolean shouldUsePropertyPromotion(@NotNull Function function) {
        Parameter[] parameters = function.getParameters();
        if (parameters.length == 0) {
            return PhpLanguageLevel.current(function.getProject()).hasFeature(PhpLanguageFeature.PROPERTY_PROMOTION);
        }

        for (Parameter parameter : parameters) {
            if (parameter instanceof PhpPromotedFieldParameterImpl) {
                return true;
            }
        }

        return false;
    }
}
