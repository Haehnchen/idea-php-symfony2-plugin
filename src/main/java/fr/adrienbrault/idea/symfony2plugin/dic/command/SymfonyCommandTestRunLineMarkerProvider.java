package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.actions.BaseRunConfigurationAction;
import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            // #[AsCommand('app:create-user')]
            // #[AsCommand(name: 'app:create-user')]
            for (PhpAttribute attribute : phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand")) {
                String name = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "name");
                if (name != null) {
                    return name;
                }
            }

            // @TODO: provide tag resolving here
            // - { name: 'console.command', command: 'app:sunshine' }

            // old style
            Method method = phpClass.findOwnMethodByName("configure");
            if(method != null) {
                PsiElement[] psiElements = PsiTreeUtil.collectElements(method, psiElement ->
                    psiElement instanceof MethodReference && "setName".equals(((MethodReference) psiElement).getName())
                );

                for (PsiElement psiElement : psiElements) {
                    if(!(psiElement instanceof MethodReference)) {
                        continue;
                    }

                    PsiElement psiMethodParameter = PsiElementUtils.getMethodParameterPsiElementAt((MethodReference) psiElement, 0);
                    if(psiMethodParameter == null) {
                        continue;
                    }

                    String stringValue = PhpElementsUtil.getStringValue(psiMethodParameter);
                    if(stringValue == null) {
                        continue;
                    }

                    return stringValue;
                }
            }
        }

        return null;
    }
}
