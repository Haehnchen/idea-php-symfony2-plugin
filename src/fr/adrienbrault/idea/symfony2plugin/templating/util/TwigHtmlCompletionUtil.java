package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;

public class TwigHtmlCompletionUtil {

    public static PsiElementPattern.Capture<PsiElement> getHrefAttributePattern() {
        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns.xmlAttribute("href")
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

}
