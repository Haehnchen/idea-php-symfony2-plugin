package fr.adrienbrault.idea.symfony2plugin.completion.command;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils;
import org.jetbrains.plugins.terminal.view.TerminalOffset;
import org.jetbrains.plugins.terminal.view.TerminalOutputModel;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalBlockBase;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalBlocksModel;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalCommandBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides code completion for Symfony console command options in the integrated terminal.
 *
 * <p>This completion contributor activates when typing Symfony console command options,
 * detecting patterns like {@code bin/console app:greet Fabien -<caret>} or
 * {@code bin/console app:greet Fabien --<caret>} and offering autocompletion suggestions
 * for available command options.</p>
 *
 * <h3>Examples:</h3>
 * <pre>
 * # Terminal input                              → Completion result
 * $ bin/console app:greet Fabien -<caret>       → Shows all available options with shortcuts
 * $ bin/console app:greet Fabien --<caret>      → Shows all available options (long form)
 * $ bin/console app:greet Fabien <caret>        → Shows all available options with dashes
 * </pre>
 */
public class CommandOptionTerminalCompletionContributor extends CompletionContributor implements DumbAware {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        // Only handle basic completion
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        Editor editor = parameters.getEditor();
        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        if (!TerminalDataContextUtils.INSTANCE.isReworkedTerminalEditor(editor)) {
            return;
        }

        // Check if this is a terminal editor
        TerminalOutputModel outputModel = editor.getUserData(TerminalOutputModel.Companion.getKEY());
        if (outputModel == null) {
            return;
        }

        TerminalBlocksModel blocksModel = editor.getUserData(TerminalBlocksModel.Companion.getKEY());
        if (blocksModel == null) {
            return;
        }

        // Get the active command block
        TerminalBlockBase activeBlock = blocksModel.getActiveBlock();
        if (!(activeBlock instanceof TerminalCommandBlock commandBlock)) {
            return;
        }

        TerminalOffset commandStartOffset = commandBlock.getCommandStartOffset();
        if (commandStartOffset == null) {
            return;
        }

        // Get the command text from the start of the command to the caret
        int caretOffset = editor.getCaretModel().getOffset();
        String commandText = outputModel.getText(
            commandStartOffset,
            outputModel.getStartOffset().plus(caretOffset)
        ).toString();

        // Check if the command starts with "bin/console" or "console"
        if (!SymfonyCommandUtil.isSymfonyConsoleCommand(commandText)) {
            return;
        }

        // Extract the command name and check if we're in the option completion context
        String commandName = extractCommandName(commandText);
        if (commandName == null) {
            return;
        }

        // Extract the prefix for completion (handles -, --, or no dash)
        OptionCompletionContext context = extractOptionCompletionContext(commandText, commandName);
        if (context == null) {
            return;
        }

        // Find the command by name
        SymfonyCommand symfonyCommand = findCommandByName(project, commandName);
        if (symfonyCommand == null) {
            return;
        }

        // Get the PhpClass for the command
        Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getAnyByFQN(symfonyCommand.getFqn());
        if (phpClasses.isEmpty()) {
            return;
        }

        PhpClass phpClass = phpClasses.iterator().next();

        // Get command options
        Map<String, SymfonyCommandUtil.CommandOption> options = SymfonyCommandUtil.getCommandOptions(phpClass);
        if (options.isEmpty()) {
            return;
        }

        // Update result set with a custom prefix matcher
        CompletionResultSet customResult = result.withPrefixMatcher(new PlainPrefixMatcher(context.prefix, true));

        // Add option suggestions based on context
        customResult.addAllElements(getOptionSuggestions(options, context));
    }

    /**
     * Extract the command name from the command text
     * E.g., "bin/console app:greet Fabien -" -> "app:greet"
     */
    @Nullable
    private String extractCommandName(String commandText) {
        String trimmed = commandText.trim();

        // Remove "bin/console " or "console " prefix
        if (trimmed.startsWith("bin/console ")) {
            trimmed = trimmed.substring("bin/console ".length());
        } else if (trimmed.startsWith("console ")) {
            trimmed = trimmed.substring("console ".length());
        } else {
            return null;
        }

        // Extract the command name (first word)
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex <= 0) {
            return null; // No arguments/options yet
        }

        return trimmed.substring(0, spaceIndex);
    }

    /**
     * Extract the option completion context (what comes after the command and arguments)
     */
    @Nullable
    private OptionCompletionContext extractOptionCompletionContext(String commandText, String commandName) {
        String trimmed = commandText.trim();

        // Remove "bin/console " or "console " prefix
        if (trimmed.startsWith("bin/console ")) {
            trimmed = trimmed.substring("bin/console ".length());
        } else if (trimmed.startsWith("console ")) {
            trimmed = trimmed.substring("console ".length());
        }

        // Remove the command name
        if (!trimmed.startsWith(commandName)) {
            return null;
        }
        
        // Remove command name and leading spaces, but keep trailing spaces
        String afterCommand = trimmed.substring(commandName.length()).replaceFirst("^\\s+", "");

        // Check if there's anything after the command
        if (afterCommand.isEmpty()) {
            return null; // No arguments or options yet
        }

        // Find the last token to determine the completion context
        // Support cases:
        // 1. "app:greet Fabien -" -> after single dash
        // 2. "app:greet Fabien --" -> after double dash
        // 3. "app:greet Fabien " -> after space (show all options)
        
        if (afterCommand.endsWith("--")) {
            // Case: "bin/console app:greet Fabien --<caret>"
            // Show long-form options only
            return new OptionCompletionContext("", OptionType.LONG_ONLY);
        } else if (afterCommand.endsWith("-")) {
            // Case: "bin/console app:greet Fabien -<caret>"
            // Show both shortcuts and long-form options
            return new OptionCompletionContext("", OptionType.BOTH);
        } else {
            // Case: "bin/console app:greet Fabien <caret>"
            // Extract any partial option name
            String[] tokens = afterCommand.split("\\s+");
            String lastToken = tokens[tokens.length - 1];
            
            if (lastToken.startsWith("--")) {
                // Partial long option: "bin/console app:greet --ver<caret>"
                return new OptionCompletionContext(lastToken.substring(2), OptionType.LONG_ONLY);
            } else if (lastToken.startsWith("-")) {
                // Partial short option: "bin/console app:greet -v<caret>"
                return new OptionCompletionContext(lastToken.substring(1), OptionType.BOTH);
            } else {
                // After a space, show all options with dashes
                return new OptionCompletionContext("", OptionType.WITH_DASHES);
            }
        }
    }

    /**
     * Find a Symfony command by name
     */
    @Nullable
    private SymfonyCommand findCommandByName(Project project, String commandName) {
        for (SymfonyCommand command : SymfonyCommandUtil.getCommands(project)) {
            if (command.getName().equals(commandName)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Get option suggestions based on the completion context
     */
    private List<LookupElement> getOptionSuggestions(
        Map<String, SymfonyCommandUtil.CommandOption> options,
        OptionCompletionContext context
    ) {
        List<LookupElement> suggestions = new ArrayList<>();

        for (SymfonyCommandUtil.CommandOption option : options.values()) {
            String description = option.description();
            if (description == null) {
                description = "";
            }

            // Add long-form option (--option)
            if (context.type == OptionType.LONG_ONLY || context.type == OptionType.BOTH) {
                suggestions.add(LookupElementBuilder.create(option.name())
                    .withPresentableText("--" + option.name())
                    .withInsertHandler((insertContext, item) -> {
                        // Insert "--" + option name
                        insertContext.getDocument().insertString(
                            insertContext.getStartOffset(),
                            "--"
                        );
                    })
                    .withTypeText(description, true)
                    .withIcon(Symfony2Icons.SYMFONY)
                );
            }

            // Add short-form option (-o) if available
            if (!StringUtils.isBlank(option.shortcut())) {
                if (context.type == OptionType.BOTH) {
                    suggestions.add(LookupElementBuilder.create(option.shortcut())
                        .withPresentableText("-" + option.shortcut())
                        .withInsertHandler((insertContext, item) -> {
                            // Insert "-" + shortcut
                            insertContext.getDocument().insertString(
                                insertContext.getStartOffset(),
                                "-"
                            );
                        })
                        .withTypeText(description + " (shortcut for --" + option.name() + ")", true)
                        .withIcon(Symfony2Icons.SYMFONY)
                    );
                }
            }

            // Add with dashes prefix for the "after space" case
            if (context.type == OptionType.WITH_DASHES) {
                // Add long-form with --
                suggestions.add(LookupElementBuilder.create("--" + option.name())
                    .withTypeText(description, true)
                    .withIcon(Symfony2Icons.SYMFONY)
                );

                // Add short-form with - if available
                if (!StringUtils.isBlank(option.shortcut())) {
                    suggestions.add(LookupElementBuilder.create("-" + option.shortcut())
                        .withTypeText(description + " (shortcut for --" + option.name() + ")", true)
                        .withIcon(Symfony2Icons.SYMFONY)
                    );
                }
            }
        }

        return suggestions;
    }

    /**
     * Context for option completion
     */
    private record OptionCompletionContext(String prefix, OptionType type) {
    }

    /**
     * Type of option completion
     */
    private enum OptionType {
        LONG_ONLY,    // After "--", show only long-form without prefix
        BOTH,         // After "-", show both short and long forms without prefix
        WITH_DASHES   // After space, show options with dashes
    }
}
