package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlCompletionContributor extends CompletionContributor {

    public XmlCompletionContributor() {
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("class"), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-service"), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-class"), new PhpClassAndParameterCompletionProvider());
    }

}

