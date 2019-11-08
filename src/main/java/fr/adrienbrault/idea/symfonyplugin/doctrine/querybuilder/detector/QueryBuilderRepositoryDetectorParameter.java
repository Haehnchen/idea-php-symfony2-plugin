package fr.adrienbrault.idea.symfonyplugin.doctrine.querybuilder.detector;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QueryBuilderRepositoryDetectorParameter {

    @NotNull
    private final Project project;
    @NotNull
    private final Collection<MethodReference> methodReferences;

    public QueryBuilderRepositoryDetectorParameter(@NotNull Project project, @NotNull Collection<MethodReference> methodReferences) {
        this.project = project;
        this.methodReferences = methodReferences;
    }

    @Nullable
    public MethodReference getMethodReferenceByName(@NotNull String methodName) {

        for (MethodReference methodReference : methodReferences) {
            if(methodName.equals(methodReference.getName())) {
                return methodReference;
            }
        }

        return null;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public Collection<MethodReference> getMethodReferences() {
        return methodReferences;
    }

}
