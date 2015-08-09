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

public class YamlPsiElementFactory {

    @NotNull
    public static PsiElement createYamlPsiFromText(Project p, final IElementType type, @NotNull String text) {
        final Ref<PsiElement> ret = new Ref<PsiElement>();
        PsiFile dummyFile = createDummyFile(p, text);
        dummyFile.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element.getNode() == type) {
                    ret.set(element);
                }

                super.visitElement(element);
            }
        });

        assert !ret.isNull() : "cannot create element from text:\n" + dummyFile.getText();

        return ret.get();
    }

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
    private static PsiFile createDummyFile(Project p, String fileText) {
        return PsiFileFactory.getInstance(p).createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, fileText, System.currentTimeMillis(), false);
    }

}
