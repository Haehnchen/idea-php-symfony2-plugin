package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector;

import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RepositoryQueryBuilderRepositoryDetector implements QueryBuilderRepositoryDetector {

    @Nullable
    @Override
    public String getRepository(@NotNull QueryBuilderRepositoryDetectorParameter parameter) {

        MethodReference qbMethodRef = parameter.getMethodReferenceByName("createQueryBuilder");
        if(qbMethodRef == null) {
            return null;
        }

        PhpClass parentOfType = PsiTreeUtil.getParentOfType(qbMethodRef, PhpClass.class);
        if(parentOfType == null) {
            return null;
        }



        return null;
    }

}
