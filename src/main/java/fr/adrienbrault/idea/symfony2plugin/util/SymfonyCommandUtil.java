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
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndexUtil;
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
    private static final Key<CachedValue<Map<String, String>>> SYMFONY_COMMAND_NAME_MAP = new Key<>("SYMFONY_COMMAND_NAME_MAP");

    @NotNull
    public static Collection<SymfonyCommand> getCommands(@NotNull Project project) {
        Map<String, String> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_COMMAND_NAME_MAP,
            () -> {
                Map<String, String> symfonyCommands = new HashMap<>();

                // Traditional Command subclasses
                for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, "\\Symfony\\Component\\Console\\Command\\Command")) {
                    if (PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    for (String commandName : SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass)) {
                        symfonyCommands.put(commandName, phpClass.getFQN());
                    }
                }

                // AsCommand attributes from index
                for (PhpClass phpClass : PhpAttributeIndexUtil.getClassesWithAttribute(project, "\\Symfony\\Component\\Console\\Attribute\\AsCommand")) {
                    if (PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    // Extract command names from the class (using existing method)
                    for (String commandName : SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass)) {
                        symfonyCommands.put(commandName, phpClass.getFQN());
                    }
                }

                return CachedValueProvider.Result.create(symfonyCommands, PsiModificationTracker.MODIFICATION_COUNT);
            },
            false
        );

        return cachedValue.entrySet().stream()
            .map(entry -> new SymfonyCommand(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    public static boolean isSymfonyConsoleCommand(@NotNull String commandText) {
        String trimmed = commandText.trim();
        return trimmed.startsWith("bin/console ") ||
            trimmed.startsWith("console ") ||
            trimmed.startsWith("symfony console ") ||
            trimmed.equals("bin/console") ||
            trimmed.equals("console") ||
            trimmed.equals("symfony console");
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
                CommandOption option = parseAddOptionCall(methodRef);
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
            for (Parameter parameter : invokeMethod.getParameters()) {
                CommandOption option = parseOptionAttribute(parameter);
                if (option != null) {
                    options.put(option.name(), option);
                }
            }
        }

        return options;
    }

    /**
     * Parses an addOption() method call and extracts the option name, shortcut, and metadata.
     */
    @Nullable
    private static CommandOption parseAddOptionCall(@NotNull MethodReference methodRef) {
        // addOption(name, shortcut, mode, description, default)
        PsiElement[] parameters = methodRef.getParameters();

        // Get name (parameter 0)
        if (parameters.length == 0) {
            return null;
        }

        PsiElement nameParam = parameters[0];
        String name = PhpElementsUtil.getStringValue(nameParam);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        // Get shortcut (parameter 1)
        String shortcut = null;
        if (parameters.length > 1) {
            shortcut = PhpElementsUtil.getStringValue(parameters[1]);
        }

        // Get description (parameter 3)
        String description = null;
        if (parameters.length > 3) {
            description = PhpElementsUtil.getStringValue(parameters[3]);
        }

        // Get default value (parameter 4)
        String defaultValue = null;
        if (parameters.length > 4) {
            defaultValue = parameters[4].getText();
        }

        return new CommandOption(nameParam, name, shortcut, description, defaultValue);
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
                CommandOption option = parseInputOptionConstructor(newExpression);
                if (option != null) {
                    options.put(option.name(), option);
                }
            }
        }

        return options;
    }

    /**
     * Parses a new InputOption() constructor and extracts the option name, shortcut, and metadata.
     *
     * Constructor signature: new InputOption(name, shortcut, mode, description, default)
     */
    @Nullable
    private static CommandOption parseInputOptionConstructor(@NotNull NewExpression newExpression) {
        PsiElement[] parameters = newExpression.getParameters();

        // Get name (parameter 0)
        if (parameters.length == 0) {
            return null;
        }

        String name = PhpElementsUtil.getStringValue(parameters[0]);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        // Get shortcut (parameter 1)
        String shortcut = null;
        if (parameters.length > 1) {
            shortcut = PhpElementsUtil.getStringValue(parameters[1]);
        }

        // Get description (parameter 3)
        String description = null;
        if (parameters.length > 3) {
            description = PhpElementsUtil.getStringValue(parameters[3]);
        }

        // Get default value (parameter 4)
        String defaultValue = null;
        if (parameters.length > 4) {
            defaultValue = parameters[4].getText();
        }

        return new CommandOption(parameters[0], name, shortcut, description, defaultValue);
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
                CommandArgument argument = parseAddArgumentCall(methodRef);
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
            for (Parameter parameter : invokeMethod.getParameters()) {
                CommandArgument argument = parseArgumentAttribute(parameter);
                if (argument != null) {
                    arguments.put(argument.name(), argument);
                }
            }
        }

        return arguments;
    }

    /**
     * Parses an addArgument() method call and extracts the argument metadata.
     */
    @Nullable
    private static CommandArgument parseAddArgumentCall(@NotNull MethodReference methodRef) {
        // addArgument(name, mode, description, default)
        PsiElement[] parameters = methodRef.getParameters();

        // Get name (parameter 0)
        if (parameters.length == 0) {
            return null;
        }

        PsiElement nameParam = parameters[0];
        String name = PhpElementsUtil.getStringValue(nameParam);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        // Get description (parameter 2)
        String description = null;
        if (parameters.length > 2) {
            description = PhpElementsUtil.getStringValue(parameters[2]);
        }

        // Get default value (parameter 3) - keep as text representation
        String defaultValue = null;
        if (parameters.length > 3) {
            defaultValue = parameters[3].getText();
        }

        return new CommandArgument(nameParam, name, description, defaultValue);
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
                CommandArgument argument = parseInputArgumentConstructor(newExpression);
                if (argument != null) {
                    arguments.put(argument.name(), argument);
                }
            }
        }

        return arguments;
    }

    /**
     * Parses a new InputArgument() constructor and extracts the argument metadata.
     *
     * Constructor signature: new InputArgument(name, mode, description, default)
     */
    @Nullable
    private static CommandArgument parseInputArgumentConstructor(@NotNull NewExpression newExpression) {
        PsiElement[] parameters = newExpression.getParameters();

        // Get name (parameter 0)
        if (parameters.length == 0) {
            return null;
        }

        String name = PhpElementsUtil.getStringValue(parameters[0]);
        if (StringUtils.isBlank(name)) {
            return null;
        }

        // Get description (parameter 2)
        String description = null;
        if (parameters.length > 2) {
            description = PhpElementsUtil.getStringValue(parameters[2]);
        }

        // Get default value (parameter 3) - keep as text representation
        String defaultValue = null;
        if (parameters.length > 3) {
            defaultValue = parameters[3].getText();
        }

        return new CommandArgument(parameters[0], name, description, defaultValue);
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
