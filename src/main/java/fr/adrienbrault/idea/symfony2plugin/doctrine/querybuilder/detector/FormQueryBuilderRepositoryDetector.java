package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormQueryBuilderRepositoryDetector implements QueryBuilderRepositoryDetector {


    @Nullable
    @Override
    public String getRepository(@NotNull QueryBuilderRepositoryDetectorParameter parameter) {

        MethodReference qbMethodRef = parameter.getMethodReferenceByName("createQueryBuilder");
        if(qbMethodRef == null) {
            return null;
        }

        /*
        $builder->add('field_1', 'foo', array(
            'class' => 'Repository',
            'query_builder' => function (EntityRepository $er) {
                return $er->createQueryBuilder('u')
                    ->orderBy('u.field_1', 'ASC');
            },
        ));
        */

        Function parentOfType = PsiTreeUtil.getParentOfType(qbMethodRef, Function.class);
        if(parentOfType != null && parentOfType.isClosure()) {
            PsiElement closure = parentOfType.getParent();
            if(closure.getNode().getElementType() == PhpElementTypes.CLOSURE) {
                PsiElement arrayValue = closure.getParent();
                if(arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                    PsiElement arrayHash = arrayValue.getParent();
                    if(arrayHash instanceof ArrayHashElement) {
                        PsiElement arrayCreation = arrayHash.getParent();
                        if(arrayCreation instanceof ArrayCreationExpression) {
                            String aClass = PhpElementsUtil.getArrayValueString((ArrayCreationExpression) arrayCreation, "class");
                            if(aClass != null && StringUtils.isNotBlank(aClass)) {

                                // finally we found our class key
                                PhpClass phpClass = EntityHelper.resolveShortcutName(parameter.getProject(), aClass);
                                if(phpClass != null) {
                                    return phpClass.getPresentableFQN();
                                }

                            }
                        }
                    }
                }

            }
        }


        return null;
    }

}
