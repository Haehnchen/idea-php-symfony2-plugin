package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.Processor;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpAttributesList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Visit class attributes; filtered by instance
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AttributeElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {

    @NotNull
    private final Processor<Pair<PhpAttribute, PhpClass>> phpDocTagProcessor;

    @NotNull
    private final String[] annotations;

    public AttributeElementWalkingVisitor(@NotNull Processor<Pair<PhpAttribute, PhpClass>> phpDocTagProcessor, @NotNull String... annotations) {
        this.phpDocTagProcessor = phpDocTagProcessor;
        this.annotations = annotations;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if ((element instanceof PhpAttributesList)) {
            visitPhpAttributesList((PhpAttributesList) element);
        }

        super.visitElement(element);
    }

    private void visitPhpAttributesList(@NotNull PhpAttributesList phpAttributesList) {
        PsiElement parent = phpAttributesList.getParent();

        if (parent instanceof PhpClass) {
            for (PhpAttribute attribute : phpAttributesList.getAttributes()) {
                String fqn = attribute.getFQN();
                if (fqn == null) {
                    continue;
                }

                if (PhpElementsUtil.isEqualClassName(fqn, annotations)) {
                    this.phpDocTagProcessor.process(Pair.create(attribute, (PhpClass) parent));
                }
            }
        }
    }
}
