package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassServiceDefinitionTargetLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {

    @NotNull
    private final Project project;

    @NotNull
    private final String fqnClass;

    public ClassServiceDefinitionTargetLazyValue(@NotNull Project project, @NotNull String fqnClass) {
        this.project = project;
        this.fqnClass = fqnClass;
    }

    @NotNull
    @Override
    protected Collection<? extends PsiElement> compute() {

        Set<String> serviceNames = new ContainerCollectionResolver.ServiceCollector(project, ContainerCollectionResolver.Source.INDEX, ContainerCollectionResolver.Source.COMPILER).convertClassNameToServices(fqnClass);
        if(serviceNames.size() == 0) {
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();
        for(String serviceName: serviceNames) {
            psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
        }

        return psiElements;
    }
}
