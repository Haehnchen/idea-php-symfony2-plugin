package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlPsiElementFactory {
    @Nullable
    public static <T extends PsiElement> T createFromText(@NotNull Project p, final Class<T> aClass, String text) {
        final PsiElement[] ret = new PsiElement[]{null};

        createDummyFile(p, text).accept(new PsiRecursiveElementWalkingVisitor() {
            public void visitElement(PsiElement element) {
                if(ret[0] == null && aClass.isInstance(element)) {
                    ret[0] = element;
                }

                super.visitElement(element);
            }
        });

        return (T) ret[0];
    }


    @NotNull
    public static PsiFile createDummyFile(Project p, String fileText) {
        return PsiFileFactory.getInstance(p).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, fileText, System.currentTimeMillis(), false);
    }

    @NotNull
    public static PsiFile createDummyFile(@NotNull Project project, @NotNull String filename, @NotNull String content) {
        return PsiFileFactory.getInstance(project).createFileFromText(filename, YAMLFileType.YML, content, System.currentTimeMillis(), false);
    }
}
