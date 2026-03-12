package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMappingDriverArguments {

    @NotNull
    private final Project project;

    @NotNull
    private final PsiFile psiFile;

    @NotNull
    private final String fqnClass;

    /**
     * @param fqnClass fully qualified entity class name; leading backslash is added if missing
     */
    public DoctrineMappingDriverArguments(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull String fqnClass) {
        this.project = project;
        this.psiFile = psiFile;
        this.fqnClass = fqnClass.startsWith("\\") ? fqnClass : "\\" + fqnClass;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return psiFile;
    }

    /**
     * @return fully qualified entity class name with leading backslash
     */
    @NotNull
    public String getClassName() {
        return fqnClass;
    }

    public boolean isEqualClass(@Nullable String fqnClass) {
        if (fqnClass == null) {
            return false;
        }

        // normalize the incoming class name to also have a leading backslash before comparing
        if (!fqnClass.startsWith("\\")) {
            fqnClass = "\\" + fqnClass;
        }

        return this.fqnClass.equals(fqnClass);
    }
}
