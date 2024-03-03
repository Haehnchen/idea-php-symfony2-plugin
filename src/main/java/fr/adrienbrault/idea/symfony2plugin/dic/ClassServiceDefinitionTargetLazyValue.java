package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassServiceDefinitionTargetLazyValue implements Supplier<Collection<? extends PsiElement>> {

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

    @Override
    public Collection<? extends PsiElement> get() {
        ContainerCollectionResolver.ServiceCollector serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project);

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (String fqnClass : fqnClasses) {
            Set<String> serviceNames = serviceCollector.convertClassNameToServices(fqnClass);
            if (serviceNames.isEmpty()) {
                continue;
            }

            for (String serviceName: serviceNames) {
                psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
            }
        }

        return psiElements;
    }
}
