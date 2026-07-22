package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionScalarArgument;
import fr.adrienbrault.idea.symfony2plugin.dic.ConsoleCommandServiceParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandUtil {
    private static final String COMMAND_CLASS = "\\Symfony\\Component\\Console\\Command\\Command";
    private static final String AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand";

    private static final Key<CachedValue<Map<String, SymfonyCommand>>> SYMFONY_COMMAND_NAME_MAP = new Key<>("SYMFONY_COMMAND_NAME_MAP");

    @NotNull
    public static Collection<SymfonyCommand> getCommands(@NotNull Project project) {
        Map<String, SymfonyCommand> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_COMMAND_NAME_MAP,
            () -> {
                Map<String, SymfonyCommand> symfonyCommands = new HashMap<>();
                Collection<PhpAttributeIndex.AttributeTarget> attributeTargets = PhpAttributeIndexUtil.getAttributeData(project, AS_COMMAND_ATTRIBUTE);
                Set<String> indexedCommandClasses = attributeTargets.stream()
                    .filter(target -> target.scope() == PhpAttributeIndex.TargetScope.PHP_CLASS && !target.data().isEmpty())
                    .map(target -> normalizeFqn(target.classFqn()).toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

                // Traditional Command subclasses
                for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, COMMAND_CLASS)) {
                    if (PhpElementsUtil.isTestClass(phpClass) || indexedCommandClasses.contains(phpClass.getFQN().toLowerCase(Locale.ROOT))) {
                        continue;
                    }

                    putClassCommands(symfonyCommands, phpClass);
                }

                // AsCommand attributes from index
                for (PhpAttributeIndex.AttributeTarget target : attributeTargets) {
                    PhpClass phpClass = PhpElementsUtil.getClassInterface(project, normalizeFqn(target.classFqn()));
                    if (phpClass == null || PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    if (!target.data().isEmpty()) {
                        for (String commandName : target.data()) {
                            symfonyCommands.put(commandName, new SymfonyCommand(commandName, phpClass.getFQN(), target.memberName()));
                        }
                        continue;
                    }

                    if (target.scope() == PhpAttributeIndex.TargetScope.PHP_CLASS) {
                        putClassCommands(symfonyCommands, phpClass);
                        continue;
                    }

                    String methodName = target.memberName();
                    if (methodName == null) {
                        continue;
                    }

                    Method method = phpClass.findOwnMethodByName(methodName);
                    if (method == null || !method.getAccess().isPublic()) {
                        continue;
                    }

                    for (String commandName : getCommandNameFromMethod(method)) {
                        symfonyCommands.put(commandName, new SymfonyCommand(commandName, phpClass.getFQN(), method.getName()));
                    }
                }

                return CachedValueProvider.Result.create(symfonyCommands, PsiModificationTracker.getInstance(project).forLanguage(com.jetbrains.php.lang.PhpLanguage.INSTANCE));
            },
            false
        );

        // Fallback: compiled container XML for commands tagged with "console.command" (e.g., Doctrine)
        Map<String, SymfonyCommand> result = new HashMap<>(cachedValue);
        ConsoleCommandServiceParser compiledParser = ServiceXmlParserFactory.getInstance(project, ConsoleCommandServiceParser.class);
        if (compiledParser != null) {
            for (Map.Entry<String, String> entry : compiledParser.getCommands().entrySet()) {
                result.putIfAbsent(entry.getKey(), new SymfonyCommand(entry.getKey(), entry.getValue()));
            }
        }

        return result.values().stream().collect(Collectors.toList());
    }

    private static void putClassCommands(@NotNull Map<String, SymfonyCommand> symfonyCommands, @NotNull PhpClass phpClass) {
        for (String commandName : getCommandNameFromClass(phpClass)) {
            symfonyCommands.put(commandName, new SymfonyCommand(commandName, phpClass.getFQN()));
        }
    }

    @Nullable
    public static PhpClass resolveCommandClass(@NotNull Project project, @NotNull SymfonyCommand command) {
        return PhpElementsUtil.getClassInterface(project, command.getFqn());
    }

    @Nullable
    public static Method resolveCommandMethod(@NotNull Project project, @NotNull SymfonyCommand command) {
        if (!command.isMethodCommand()) {
            return null;
        }

        PhpClass phpClass = resolveCommandClass(project, command);
        return phpClass != null ? phpClass.findOwnMethodByName(command.getMethodName()) : null;
    }

    @Nullable
    public static PhpNamedElement resolveCommandTarget(@NotNull Project project, @NotNull SymfonyCommand command) {
        Method method = resolveCommandMethod(project, command);
        if (method != null) {
            return method;
        }

        return resolveCommandClass(project, command);
    }

    @NotNull
    private static String normalizeFqn(@NotNull String fqn) {
        return fqn.startsWith("\\") ? fqn : "\\" + fqn;
    }

    /**
     * Collects all available option names and shortcuts from a command class.
     * Supports both traditional configure() methods with addOption() calls
     * and modern #[Option] attributes on __invoke() method parameters.
     */
    @NotNull
    public static Map<String, CommandOption> getCommandOptions(@NotNull PhpClass phpClass) {
        Map<String, CommandOption> options = new HashMap<>();

        // Collect options from traditional configure() method
        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod != null) {
            // Case 1: addOption() method calls
            Collection<MethodReference> addOptionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addOption");
            for (MethodReference methodRef : addOptionCalls) {
                CommandOption option = parseOptionFromParameters(methodRef.getParameters());
                if (option != null) {
                    options.put(option.name(), option);
                }
            }

            // Case 2: setDefinition() with new InputOption() instances
            Collection<MethodReference> setDefinitionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setDefinition");
            for (MethodReference methodRef : setDefinitionCalls) {
                Map<String, CommandOption> definitionOptions = parseSetDefinitionCall(methodRef);
                options.putAll(definitionOptions);
            }
        }

        // Collect options from modern __invoke() method with #[Option] attributes
        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod != null) {
            options.putAll(getCommandOptions(invokeMethod));
        }

        return options;
    }

    @NotNull
    public static Map<String, CommandOption> getCommandOptions(@NotNull Method commandMethod) {
        Map<String, CommandOption> options = new HashMap<>();

        for (Parameter parameter : commandMethod.getParameters()) {
            CommandOption option = parseOptionAttribute(parameter);
            if (option != null) {
                options.put(option.name(), option);
            }
        }

        return options;
    }

    @NotNull
    public static Map<String, CommandOption> getCommandOptions(@NotNull Project project, @NotNull SymfonyCommand command) {
        Method method = resolveCommandMethod(project, command);
        if (method != null) {
            return getCommandOptions(method);
        }

        PhpClass phpClass = resolveCommandClass(project, command);
        return phpClass != null ? getCommandOptions(phpClass) : Collections.emptyMap();
    }

    /**
     * Parses a setDefinition() method call and extracts options from new InputOption() instances.
     * Handles both single instances and arrays of instances.
     */
    @NotNull
    private static Map<String, CommandOption> parseSetDefinitionCall(@NotNull MethodReference methodRef) {
        Map<String, CommandOption> options = new HashMap<>();

        // Collect all new InputOption() expressions within the setDefinition call
        Collection<NewExpression> newExpressions = com.intellij.psi.util.PsiTreeUtil.collectElementsOfType(methodRef, NewExpression.class);
        for (NewExpression newExpression : newExpressions) {
            // Check if this is a new InputOption() instance
            if (PhpElementsUtil.getNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Console\\Input\\InputOption") != null) {
                CommandOption option = parseOptionFromParameters(newExpression.getParameters());
                if (option != null) {
                    options.put(option.name(), option);
                }
            }
        }

        return options;
    }

    /**
     * Parses the parameters of addOption(name, shortcut, mode, description, default)
     * or new InputOption(name, shortcut, mode, description, default).
     */
    @Nullable
    private static CommandOption parseOptionFromParameters(@NotNull PsiElement[] parameters) {
        if (parameters.length == 0) {
            return null;
        }

        PsiElement nameParam = parameters[0];
        String name = PhpElementsUtil.getStringValue(nameParam);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        String shortcut = parameters.length > 1 ? PhpElementsUtil.getStringValue(parameters[1]) : null;
        String description = parameters.length > 3 ? PhpElementsUtil.getStringValue(parameters[3]) : null;
        String defaultValue = parameters.length > 4 ? parameters[4].getText() : null;

        return new CommandOption(nameParam, name, shortcut, description, defaultValue);
    }

    /**
     * Parses a #[Option] attribute from a parameter and extracts the option name, shortcut, and metadata.
     */
    @Nullable
    private static CommandOption parseOptionAttribute(@NotNull Parameter parameter) {
        // Check for Option attribute (only the valid FQN)
        Collection<PhpAttribute> attributes = parameter.getAttributes("\\Symfony\\Component\\Console\\Attribute\\Option");

        for (PhpAttribute attribute : attributes) {
            // Extract option name, shortcut, and description from attribute arguments
            String name = null;
            String shortcut = null;
            String description = null;

            for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                String argName = argument.getName();

                if (argument.getArgument() instanceof PhpExpectedFunctionScalarArgument scalarArgument) {
                    String value = PsiElementUtils.trimQuote(scalarArgument.getNormalizedValue());

                    if ("name".equals(argName)) {
                        name = value;
                    } else if ("shortcut".equals(argName)) {
                        shortcut = value;
                    } else if ("description".equals(argName)) {
                        description = value;
                    }
                }
            }

            // If name is not explicitly set, use the parameter name
            if (name == null) {
                name = parameter.getName();
            }

            if (StringUtils.isBlank(name)) {
                continue;
            }

            // Get default value from parameter default value
            String defaultValue = null;
            PsiElement defaultValueElement = parameter.getDefaultValue();
            if (defaultValueElement != null) {
                defaultValue = defaultValueElement.getText();
            }

            return new CommandOption(parameter, name, shortcut, description, defaultValue);
        }

        return null;
    }

    /**
     * Collects all available arguments from a command class.
     * Supports both traditional configure() methods with addArgument() calls,
     * setDefinition() with new InputArgument() instances,
     * and modern #[Argument] attributes on __invoke() method parameters.
     */
    @NotNull
    public static Map<String, CommandArgument> getCommandArguments(@NotNull PhpClass phpClass) {
        Map<String, CommandArgument> arguments = new HashMap<>();

        // Collect arguments from traditional configure() method
        Method configureMethod = phpClass.findOwnMethodByName("configure");
        if (configureMethod != null) {
            // Case 1: addArgument() method calls
            Collection<MethodReference> addArgumentCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "addArgument");
            for (MethodReference methodRef : addArgumentCalls) {
                CommandArgument argument = parseArgumentFromParameters(methodRef.getParameters());
                if (argument != null) {
                    arguments.put(argument.name(), argument);
                }
            }

            // Case 2: setDefinition() with new InputArgument() instances
            Collection<MethodReference> setDefinitionCalls = PhpElementsUtil.collectMethodReferencesInsideControlFlow(configureMethod, "setDefinition");
            for (MethodReference methodRef : setDefinitionCalls) {
                Map<String, CommandArgument> definitionArguments = parseSetDefinitionCallForArguments(methodRef);
                arguments.putAll(definitionArguments);
            }
        }

        // Collect arguments from modern __invoke() method with #[Argument] attributes
        Method invokeMethod = phpClass.findOwnMethodByName("__invoke");
        if (invokeMethod != null) {
            arguments.putAll(getCommandArguments(invokeMethod));
        }

        return arguments;
    }

    @NotNull
    public static Map<String, CommandArgument> getCommandArguments(@NotNull Method commandMethod) {
        Map<String, CommandArgument> arguments = new HashMap<>();

        for (Parameter parameter : commandMethod.getParameters()) {
            CommandArgument argument = parseArgumentAttribute(parameter);
            if (argument != null) {
                arguments.put(argument.name(), argument);
            }
        }

        return arguments;
    }

    @NotNull
    public static Map<String, CommandArgument> getCommandArguments(@NotNull Project project, @NotNull SymfonyCommand command) {
        Method method = resolveCommandMethod(project, command);
        if (method != null) {
            return getCommandArguments(method);
        }

        PhpClass phpClass = resolveCommandClass(project, command);
        return phpClass != null ? getCommandArguments(phpClass) : Collections.emptyMap();
    }

    /**
     * Parses a setDefinition() method call and extracts arguments from new InputArgument() instances.
     *
     * @param methodRef The setDefinition method reference
     * @return Map of argument names to CommandArgument objects
     */
    @NotNull
    private static Map<String, CommandArgument> parseSetDefinitionCallForArguments(@NotNull MethodReference methodRef) {
        Map<String, CommandArgument> arguments = new HashMap<>();

        // Collect all new InputArgument() expressions within the setDefinition call
        Collection<NewExpression> newExpressions = com.intellij.psi.util.PsiTreeUtil.collectElementsOfType(methodRef, NewExpression.class);
        for (NewExpression newExpression : newExpressions) {
            // Check if this is a new InputArgument() instance
            if (PhpElementsUtil.getNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Console\\Input\\InputArgument") != null) {
                CommandArgument argument = parseArgumentFromParameters(newExpression.getParameters());
                if (argument != null) {
                    arguments.put(argument.name(), argument);
                }
            }
        }

        return arguments;
    }

    /**
     * Parses the parameters of addArgument(name, mode, description, default)
     * or new InputArgument(name, mode, description, default).
     */
    @Nullable
    private static CommandArgument parseArgumentFromParameters(@NotNull PsiElement[] parameters) {
        if (parameters.length == 0) {
            return null;
        }

        PsiElement nameParam = parameters[0];
        String name = PhpElementsUtil.getStringValue(nameParam);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        String description = parameters.length > 2 ? PhpElementsUtil.getStringValue(parameters[2]) : null;
        String defaultValue = parameters.length > 3 ? parameters[3].getText() : null;

        return new CommandArgument(nameParam, name, description, defaultValue);
    }

    /**
     * Parses a #[Argument] attribute from a parameter and extracts the argument metadata.
     */
    @Nullable
    private static CommandArgument parseArgumentAttribute(@NotNull Parameter parameter) {
        // Check for Argument attribute
        Collection<PhpAttribute> attributes = parameter.getAttributes("\\Symfony\\Component\\Console\\Attribute\\Argument");

        for (PhpAttribute attribute : attributes) {
            // Extract argument name and description from attribute arguments
            String name = null;
            String description = null;

            for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                String argName = argument.getName();

                if (argument.getArgument() instanceof PhpExpectedFunctionScalarArgument scalarArgument) {
                    String value = PsiElementUtils.trimQuote(scalarArgument.getNormalizedValue());

                    if ("name".equals(argName)) {
                        name = value;
                    } else if ("description".equals(argName)) {
                        description = value;
                    }
                }
            }

            // If name is not explicitly set, use the parameter name
            if (name == null) {
                name = parameter.getName();
            }

            if (StringUtils.isBlank(name)) {
                continue;
            }

            // Get default value from parameter default value
            String defaultValue = null;
            PsiElement defaultValueElement = parameter.getDefaultValue();
            if (defaultValueElement != null) {
                defaultValue = defaultValueElement.getText();
            }

            return new CommandArgument(parameter, name, description, defaultValue);
        }

        return null;
    }

    /**
     * Extracts console command names from a PHP class. Detection priority:
     * <ul>
     *   <li>{@code #[AsCommand(name: '...')]} attribute including aliases (Symfony 5.4+, also 7.3+ without extends Command)</li>
     *   <li>{@code protected static $defaultName} property with pipe-separated aliases (Symfony 3.4+)</li>
     *   <li>{@code configure()} method with {@code $this->setName('...')} call (Symfony 2.0+)</li>
     * </ul>
     */
    @NotNull
    public static List<String> getCommandNameFromClass(@NotNull PhpClass phpClass) {
        // #[AsCommand] attribute - works for both styles (with and without extends Command)
        List<String> attributeNames = getCommandNamesFromAsCommandAttributes(phpClass);
        if (!attributeNames.isEmpty()) {
            return attributeNames;
        }

        // Only for classic commands (extends Command)
        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            Field defaultName = phpClass.findFieldByName("defaultName", false);
            if (defaultName != null) {
                PsiElement defaultValue = defaultName.getDefaultValue();
                if (defaultValue != null) {
                    String stringValue = PhpElementsUtil.getStringValue(defaultValue);
                    if (stringValue != null) {
                        return List.of(stringValue.split("\\|"));
                    }
                }
            }

            Method method = phpClass.findOwnMethodByName("configure");
            if (method != null) {
                for (MethodReference methodReference : PhpElementsUtil.collectMethodReferencesInsideControlFlow(method, "setName")) {
                    PsiElement psiMethodParameter = PsiElementUtils.getMethodParameterPsiElementAt(methodReference, 0);
                    if (psiMethodParameter == null) {
                        continue;
                    }

                    String stringValue = PhpElementsUtil.getStringValue(psiMethodParameter);
                    if (stringValue != null) {
                        return Collections.singletonList(stringValue);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    @NotNull
    public static List<String> getCommandNameFromMethod(@NotNull Method method) {
        if (!method.getAccess().isPublic()) {
            return Collections.emptyList();
        }

        return getCommandNamesFromAsCommandAttributes(method);
    }

    @NotNull
    private static List<String> getCommandNamesFromAsCommandAttributes(@NotNull PhpAttributesOwner element) {
        for (PhpAttribute attribute : element.getAttributes(AS_COMMAND_ATTRIBUTE)) {
            String name = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "name");
            List<String> names = new ArrayList<>();

            if (StringUtils.isNotBlank(name)) {
                names.add(name);
            }

            names.addAll(PhpPsiAttributesUtil.getAttributeValueByNameAsArray(attribute, "aliases"));

            if (!names.isEmpty()) {
                return names;
            }
        }

        return Collections.emptyList();
    }

    public record CommandOption(
        @NotNull PsiElement target,
        @NotNull String name,
        @Nullable String shortcut,
        @Nullable String description,
        @Nullable String defaultValue
    ) {
    }

    public record CommandArgument(
        @NotNull PsiElement target,
        @NotNull String name,
        @Nullable String description,
        @Nullable String defaultValue
    ) {
    }
}
