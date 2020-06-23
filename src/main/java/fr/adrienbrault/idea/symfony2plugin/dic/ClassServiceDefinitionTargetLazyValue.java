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
    private final Collection<String> fqnClasses;

    public ClassServiceDefinitionTargetLazyValue(@NotNull Project project, @NotNull Collection<String> fqnClasses) {
        this.project = project;
        this.fqnClasses = fqnClasses;
    }

    public ClassServiceDefinitionTargetLazyValue(@NotNull Project project, @NotNull String fqnClass) {
        this.project = project;
        this.fqnClasses = Collections.singletonList(fqnClass);
    }

    @NotNull
    @Override
    protected Collection<? extends PsiElement> compute() {
        ContainerCollectionResolver.ServiceCollector serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project);

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (String fqnClass : fqnClasses) {
            Set<String> serviceNames = serviceCollector.convertClassNameToServices(fqnClass);
            if (serviceNames.size() == 0) {
                continue;
            }

            for (String serviceName: serviceNames) {
                psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
            }
        }

        return psiElements;
    }
}
