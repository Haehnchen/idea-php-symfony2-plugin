package fr.adrienbrault.idea.symfony2plugin.config.annotation;


import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.parser.PhpElementTypes;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationElementPatternHelper {

    public static ElementPattern<PsiElement> getTextIdentifier(String keyName) {
        return PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_IDENTIFIER).withParent(
                PlatformPatterns.psiElement(PhpDocElementTypes.phpDocTagValue).withParent(
                        PlatformPatterns.psiElement(PhpDocElementTypes.phpDocTag).withText(PlatformPatterns.string().startsWith(keyName))
                )
        ).withLanguage(PhpLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getOrmProperties() {
        // @TODO: afterSibling: dont we have beforeSibling ?
        return PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_IDENTIFIER).withParent(
                PlatformPatterns.psiElement(PhpDocComment.class).afterSibling(
                        PlatformPatterns.psiElement(PhpElementTypes.CLASS_FIELDS))
                            ).inside(
                                PlatformPatterns.psiElement(PhpElementTypes.NAMESPACE)
        ).withLanguage(PhpLanguage.INSTANCE);
    }

}
