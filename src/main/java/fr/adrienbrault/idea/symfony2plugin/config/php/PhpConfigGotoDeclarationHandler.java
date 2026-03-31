package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides Ctrl+Click navigation for root config keys in PHP config files.
 * Navigates from: {@code 'framework' => [...]} to the corresponding PHP configuration class.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlGoToDeclarationHandler
 */
public class PhpConfigGotoDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        var parent = psiElement.getContext();
        if (!(parent instanceof StringLiteralExpression stringLiteral)) {
            return null;
        }

        if (!PhpConfigUtil.isRootConfigKey(stringLiteral) && !PhpConfigUtil.isConditionalConfigKey(stringLiteral)) {
            return null;
        }

        String key = stringLiteral.getContents();
        if (StringUtils.isBlank(key) || key.startsWith("when@")) {
            return null;
        }

        var targets = ConfigUtil.getTreeSignatureTargets(psiElement.getProject(), key);
        if (targets.isEmpty()) {
            return null;
        }

        return targets.toArray(PsiElement.EMPTY_ARRAY);
    }

    @Nullable
    @Override
    public String getActionText(@NotNull DataContext context) {
        return null;
    }
}
