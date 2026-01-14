package fr.adrienbrault.idea.symfony2plugin.completion.command;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.block.util.TerminalDataContextUtils;
import org.jetbrains.plugins.terminal.view.TerminalOffset;
import org.jetbrains.plugins.terminal.view.TerminalOutputModel;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalBlockBase;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalBlocksModel;
import org.jetbrains.plugins.terminal.view.shellIntegration.TerminalCommandBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides code completion for Symfony console command names in the integrated terminal.
 *
 * <p>This completion contributor activates when typing Symfony console commands in the terminal,
 * detecting patterns like {@code bin/console <caret>}, {@code console <caret>}, or {@code symfony console <caret>}
 * and offering autocompletion suggestions for available Symfony commands.</p>
 *
 * <h3>Examples:</h3>
 * <pre>
 * # Terminal input                    → Completion result
 * $ bin/console <caret>               → Shows all available commands
 * $ console cac<caret>                → Filters to cache:* commands
 * $ symfony console debug:con<caret>  → Suggests debug:container, debug:config, etc.
 * </pre>
 */
public class CommandNameTerminalCompletionContributor extends CompletionContributor implements DumbAware {

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

        // Extract the prefix for completion
        String prefix = extractCompletionPrefix(commandText);
        
        // If prefix is null, we're not in a valid position for command name completion
        // (e.g., we're typing arguments/options after the command name)
        if (prefix == null) {
            return;
        }

        // Update result set with a custom prefix matcher
        CompletionResultSet customResult = result.withPrefixMatcher(new PlainPrefixMatcher(prefix, true));

        customResult.addAllElements(getSymfonyCommandSuggestions(editor.getProject()));
    }

    private String extractCompletionPrefix(String commandText) {
        String trimmed = commandText.trim();

        // Remove "bin/console ", "console ", or "symfony console " prefix
        if (trimmed.startsWith("bin/console ")) {
            trimmed = trimmed.substring("bin/console ".length());
        } else if (trimmed.startsWith("console ")) {
            trimmed = trimmed.substring("console ".length());
        } else if (trimmed.startsWith("symfony console ")) {
            trimmed = trimmed.substring("symfony console ".length());
        } else {
            return null;
        }

        // For now, we only support command name completion (not arguments)
        // So we take everything until the first space
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            // Already typed a complete command with arguments, no completion
            return null;
        }

        return trimmed;
    }

    private List<LookupElement> getSymfonyCommandSuggestions(@NotNull Project project) {
        List<LookupElement> suggestions = new ArrayList<>();

        for (SymfonyCommand command : SymfonyCommandUtil.getCommands(project)) {
            String className = extractClassName(command.getFqn());
            suggestions.add(LookupElementBuilder.create(command.getName())
                .withTypeText(className, true)
                .withIcon(Symfony2Icons.SYMFONY)
            );
        }

        return suggestions;
    }

    private String extractClassName(@NotNull String fqn) {
        int lastBackslash = fqn.lastIndexOf('\\');

        if (lastBackslash >= 0 && lastBackslash < fqn.length() - 1) {
            return fqn.substring(lastBackslash + 1);
        }

        return fqn;
    }
}