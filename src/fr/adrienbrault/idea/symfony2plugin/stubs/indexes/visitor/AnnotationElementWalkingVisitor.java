package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.Processor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {

    @NotNull
    private final Processor<PhpDocTag> phpDocTagProcessor;

    @NotNull
    private final String[] annotations;

    public AnnotationElementWalkingVisitor(@NotNull Processor<PhpDocTag> phpDocTagProcessor, @NotNull String... annotations) {
        this.phpDocTagProcessor = phpDocTagProcessor;
        this.annotations = annotations;
    }

    @Override
    public void visitElement(PsiElement element) {
        if ((element instanceof PhpDocTag)) {
            visitPhpDocTag((PhpDocTag) element);
        }
        super.visitElement(element);
    }

    private void visitPhpDocTag(@NotNull PhpDocTag phpDocTag) {

        // "@var" and user non related tags dont need an action
        if(AnnotationUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
            return;
        }

        Map<String, String> fileImports = AnnotationUtil.getUseImportMap(phpDocTag);
        if(fileImports.size() == 0) {
            return;
        }

        String annotationFqnName = AnnotationRoutesStubIndex.getClassNameReference(phpDocTag, fileImports);
        for (String annotation : annotations) {
            if(annotation.equals(annotationFqnName)) {
                this.phpDocTagProcessor.process(phpDocTag);
            }
        }
    }
}
