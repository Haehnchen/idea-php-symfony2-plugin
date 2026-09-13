package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigTemplateCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import junit.framework.TestCase;

/**
 * @see TwigTemplateCompletionContributor#decorateFormFieldLookupElement
 */
public class TwigFormFieldLookupElementTest extends TestCase {
    public void testFormFieldCompletionUsesPrimitiveFormDataHolderPresentation() {
        LookupElement lookupElement = TwigTemplateCompletionContributor.decorateFormFieldLookupElement(
            LookupElementBuilder.create("title"),
            new FormFieldDataHolder("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", "\\App\\Form\\ProductType")
        );

        LookupElementPresentation presentation = new LookupElementPresentation();
        lookupElement.renderElement(presentation);

        assertEquals("title", lookupElement.getLookupString());
        assertEquals("TextType", presentation.getTypeText());
        assertEquals("(ProductType)", presentation.getTailText());
    }
}
