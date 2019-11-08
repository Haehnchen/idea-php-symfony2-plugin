package fr.adrienbrault.idea.symfonyplugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfonyplugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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

        Set<String> serviceNames = ContainerCollectionResolver.ServiceCollector.create(project).convertClassNameToServices(fqnClass);
        if(serviceNames.size() == 0) {
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new ArrayList<>();
        for(String serviceName: serviceNames) {
            psiElements.addAll(ServiceIndexUtil.findServiceDefinitions(project, serviceName));
        }

        return psiElements;
    }
}
