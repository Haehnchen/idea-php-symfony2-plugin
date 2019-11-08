package fr.adrienbrault.idea.symfonyplugin.external.toolbox.provider;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderAbstract;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RoutesToolboxProvider extends PhpToolboxProviderAbstract {
    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements(@NotNull PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return RouteHelper.getRoutesLookupElements(parameter.getProject());
    }


    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(@NotNull PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        return RouteHelper.getRouteDefinitionTargets(
            parameter.getProject(),
            parameter.getContents()
        );
    }

    @NotNull
    @Override
    public String getName() {
        return "symfony.routes";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Icon getIcon() {
                return Symfony2Icons.ROUTE;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Symfony routes";
            }
        };
    }

}
