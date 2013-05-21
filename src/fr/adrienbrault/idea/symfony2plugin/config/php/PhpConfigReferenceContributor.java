package fr.adrienbrault.idea.symfony2plugin.config.php;


import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import fr.adrienbrault.idea.symfony2plugin.config.component.ParameterReference;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpStringLiteralExpressionReference;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpConfigReferenceContributor extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(ParameterReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "hasParameter")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "getParameter")
        );

        psiReferenceRegistrar.registerReferenceProvider(PhpElementsUtil.methodWithFirstStringPattern(), new PhpStringLiteralExpressionReference(ServiceReference.class)
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerInterface", "has")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "hasDefinition")
            .addCall("\\Symfony\\Component\\DependencyInjection\\ContainerBuilder", "getDefinition")
        );

    }

}
