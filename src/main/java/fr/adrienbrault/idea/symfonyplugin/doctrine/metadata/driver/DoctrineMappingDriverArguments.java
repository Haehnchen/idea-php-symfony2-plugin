package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.driver;

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
    private final String className;

    public DoctrineMappingDriverArguments(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull String className) {
        this.project = project;
        this.psiFile = psiFile;
        this.className = className;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return psiFile;
    }

    @NotNull
    public String getClassName() {
        return className;
    }

    public boolean isEqualClass(@Nullable String className) {
        if(className == null) {
            return false;
        }

        if(this.className.equals(className)) {
            return true;
        }

        String myClass = this.className;
        if(myClass.startsWith("\\")) {
            myClass = myClass.substring(1);
        }

        if(className.startsWith("\\")) {
            className = myClass.substring(1);
        }

        return className.equals(myClass);
    }
}
