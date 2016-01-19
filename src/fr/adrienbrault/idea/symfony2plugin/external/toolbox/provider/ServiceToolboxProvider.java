package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderAbstract;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceToolboxProvider extends PhpToolboxProviderAbstract {

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements(@NotNull PhpToolboxCompletionContributorParameter parameter) {

        List<LookupElement> results = new ArrayList<LookupElement>();

        Boolean aPrivate = parameter.getParameterBag().getParameterBool("private", false);
        Boolean aPublic = parameter.getParameterBag().getParameterBool("public", true);

        for (ContainerService service : ContainerCollectionResolver.getServices(parameter.getProject()).values()) {
            if(service.isPrivate() == aPrivate) {
                results.add(new ServiceStringLookupElement(service));
                continue;
            }

            if(service.isPrivate() != aPublic) {
                results.add(new ServiceStringLookupElement(service));
            }
        }

        return results;
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(@NotNull PhpToolboxDeclarationHandlerParameter parameter) {
        final PhpClass serviceClass = ServiceUtil.getServiceClass(parameter.getProject(), parameter.getContents());
        if(serviceClass == null) {
            return Collections.emptyList();
        }

        return new ArrayList<PsiElement>() {{
            add(serviceClass);
        }};
    }

    @NotNull
    @Override
    public String getName() {
        return "symfony.services";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Icon getIcon() {
                return Symfony2Icons.SERVICE;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Public services";
            }

            @Nullable
            public ProviderParameter[] getParameter() {
                return new ProviderParameter[] {
                    new ProviderParameter("private", ProviderParameter.TYPE.BOOLEAN),
                    new ProviderParameter("public", ProviderParameter.TYPE.BOOLEAN),
                };
            }
        };
    }
}
