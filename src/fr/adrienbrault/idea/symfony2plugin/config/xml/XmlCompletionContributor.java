package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.EventCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlCompletionContributor extends CompletionContributor {

    public XmlCompletionContributor() {
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-service").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("parent").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getParameterWithClassEndingPattern().inside(XmlHelper.getInsideTagPattern("parameters")), new PhpClassCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "event").inside(XmlHelper.getInsideTagPattern("services")), new EventCompletionProvider());

    }

}

