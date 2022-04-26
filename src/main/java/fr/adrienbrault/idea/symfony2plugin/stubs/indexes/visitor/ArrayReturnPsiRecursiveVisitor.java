package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import kotlin.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ArrayReturnPsiRecursiveVisitor extends PsiRecursiveElementWalkingVisitor {

    @NotNull
    private final Consumer<Pair<String, PsiElement>> arrayKeyVisitor;

    public ArrayReturnPsiRecursiveVisitor(@NotNull Consumer<Pair<String, PsiElement>> arrayKeyVisitor) {
        this.arrayKeyVisitor = arrayKeyVisitor;
    }

    @Override
    public void visitElement(@NotNull PsiElement element) {
        if(element instanceof PhpReturn) {
            visitPhpReturn((PhpReturn) element);
        }

        super.visitElement(element);
    }

    public void visitPhpReturn(PhpReturn phpReturn) {
        PsiElement arrayCreation = phpReturn.getFirstPsiChild();
        if(arrayCreation instanceof ArrayCreationExpression) {
            collectConfigKeys((ArrayCreationExpression) arrayCreation, this.arrayKeyVisitor);
        }
    }

    public static void collectConfigKeys(@NotNull ArrayCreationExpression creationExpression, @NotNull Consumer<Pair<String, PsiElement>> arrayKeyVisitor) {
        collectConfigKeys(creationExpression, arrayKeyVisitor, new ArrayList<>());
    }

    public static void collectConfigKeys(@NotNull ArrayCreationExpression creationExpression, @NotNull Consumer<Pair<String, PsiElement>> arrayKeyVisitor, @NotNull List<String> context) {

        for(ArrayHashElement hashElement: PsiTreeUtil.getChildrenOfTypeAsList(creationExpression, ArrayHashElement.class)) {

            PsiElement arrayKey = hashElement.getKey();
            PsiElement arrayValue = hashElement.getValue();

            if(arrayKey instanceof StringLiteralExpression) {
                List<String> myContext = new ArrayList<>(context);
                myContext.add(((StringLiteralExpression) arrayKey).getContents());
                String keyName = StringUtils.join(myContext, ".");

                if(arrayValue instanceof ArrayCreationExpression) {
                    collectConfigKeys((ArrayCreationExpression) arrayValue, arrayKeyVisitor, myContext);
                } else {
                    arrayKeyVisitor.accept(new Pair<>(keyName, arrayKey));
                }
            }
        }
    }
}