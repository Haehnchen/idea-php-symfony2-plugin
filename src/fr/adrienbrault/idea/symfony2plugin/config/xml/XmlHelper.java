package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;

public class XmlHelper {
    public static PsiElementPattern.Capture<PsiElement> getTagPattern(String... tags) {
        return XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue()
                .inside(XmlPatterns
                    .xmlAttribute()
                    .withName(StandardPatterns.string().oneOfIgnoreCase(tags)
                    )
                )
            );
    }
}
