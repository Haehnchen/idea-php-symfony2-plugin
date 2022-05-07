package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.actions.BaseRunConfigurationAction;
import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.stubs.indexes.expectedArguments.PhpExpectedFunctionArgument;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandTestRunLineMarkerProvider extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement leaf) {
        PhpClass phpClass = getCommandContext(leaf);
        if (phpClass != null) {
            String commandNameFromClass = getCommandNameFromClass(phpClass);
            if (commandNameFromClass != null) {
                BaseRunConfigurationAction baseRunConfigurationAction = new RunContextAction(DefaultRunExecutor.getRunExecutorInstance());
                return new Info(AllIcons.RunConfigurations.TestState.Run, new AnAction[]{baseRunConfigurationAction}, (psiElement) -> "Run Command");
            }
        }

        return null;
    }

    @Nullable
    public static PhpClass getCommandContext(@NotNull PsiElement leaf) {
        if (PhpPsiUtil.isOfType(leaf, PhpTokenTypes.IDENTIFIER)) {
            PhpNamedElement element = ObjectUtils.tryCast(leaf.getParent(), PhpNamedElement.class);
            if (element != null && element.getNameIdentifier() == leaf) {
                if (element instanceof PhpClass) {
                    return (PhpClass) element;
                }
            }
        }

        return null;
    }

    @Nullable
    public static String getCommandNameFromClass(@NotNull PhpClass phpClass) {
        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            // lazy naming:
            // protected static $defaultName = 'app:create-user'
            Field defaultName = phpClass.findFieldByName("defaultName", false);
            if (defaultName != null) {
                PsiElement defaultValue = defaultName.getDefaultValue();
                if (defaultValue != null) {
                    return PhpElementsUtil.getStringValue(defaultValue);
                }
            }

            // php attributes:
            // #[AsCommand(name: 'app:create-user')]
            for (PhpAttribute attribute : phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand")) {
                for (PhpAttribute.PhpAttributeArgument argument : attribute.getArguments()) {
                    String name = argument.getName();
                    if ("name".equals(name)) {
                        PhpExpectedFunctionArgument argument1 = argument.getArgument();
                        if (argument1 != null) {
                            String value1 = PsiElementUtils.trimQuote(argument1.getValue());
                            if (StringUtils.isNotBlank(value1)) {
                                return value1;
                            }
                        }
                        break;
                    }
                }
            }

            // @TODO: provide tag resolving here
            // - { name: 'console.command', command: 'app:sunshine' }
        }

        return null;
    }
}
