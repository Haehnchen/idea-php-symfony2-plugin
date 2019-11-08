package fr.adrienbrault.idea.symfonyplugin.external.toolbox.provider;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIcons;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderInterface;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfonyplugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerToolboxProvider implements PhpToolboxProviderInterface {
    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements(@NotNull PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return ControllerIndex.getControllerLookupElements(parameter.getProject());
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(final @NotNull PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(parameter.getProject(), parameter.getContents()));
    }

    @NotNull
    @Override
    public String getName() {
        return "symfony.controller";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Icon getIcon() {
                return PhpIcons.METHOD_ICON;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Controller";
            }
        };
    }
}
