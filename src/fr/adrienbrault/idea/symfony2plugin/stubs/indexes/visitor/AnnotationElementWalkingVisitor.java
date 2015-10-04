package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.Processor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AnnotationElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {

    private final Processor<PhpDocTag> phpDocTagProcessor;
    @NotNull
    private final String[] annotations;
    private Map<String, String> fileImports;

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

    private void visitPhpDocTag(PhpDocTag phpDocTag) {

        // "@var" and user non related tags dont need an action
        if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
            return;
        }

        // init file imports
        if(this.fileImports == null) {
            this.fileImports = AnnotationRoutesStubIndex.getFileUseImports(phpDocTag.getContainingFile());
        }

        if(this.fileImports.size() == 0) {
            return;
        }

        String annotationFqnName = AnnotationRoutesStubIndex.getClassNameReference(phpDocTag, this.fileImports);
        for (String annotation : annotations) {
            if(annotation.equals(annotationFqnName)) {
                this.phpDocTagProcessor.process(phpDocTag);
            }
        }
    }
}
