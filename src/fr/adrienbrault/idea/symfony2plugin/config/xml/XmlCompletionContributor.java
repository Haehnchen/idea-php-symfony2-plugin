package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlCompletionContributor extends CompletionContributor {
    public XmlCompletionContributor() {

        extend(CompletionType.BASIC, XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue()
                .inside(XmlPatterns
                    .xmlAttribute()
                    .withName(StandardPatterns.string().equalTo("class")
                    )
                )
        ), new PhpClassCompletionProvider());

    }

}

