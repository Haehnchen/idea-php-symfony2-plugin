package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.VirtualFilePattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlToken;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationTagCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {% trans from "app" %}<caret>{% endtrans %}
        registrar.register(
            getTranslationTagValuePattern(), psiElement -> {
                TwigCompositeElement element = getTagOnTwigViewProvider(psiElement);
                if(element == null) {
                    return null;
                }

                String domain = TwigHelper.getDomainFromTranslationTag(element);
                if(domain == null) {
                    domain = TwigUtil.getTransDefaultDomainOnScope(element);
                }

                return new MyTranslationGotoCompletionProvider(element, domain);
            }
        );
    }

    @NotNull
    private PsiElementPattern.Capture<XmlToken> getTranslationTagValuePattern() {
        return PlatformPatterns.psiElement(XmlToken.class)
            .inVirtualFile(new VirtualFilePattern().withExtension("twig"))
            .withLanguage(XMLLanguage.INSTANCE);
    }

    /**
     * Find trans tag as this is
     * <caret>{% endtrans %}
     */
    private static TwigCompositeElement getTagOnTwigViewProvider(@NotNull PsiElement element) {
        PsiElement psiElement = TwigUtil.getElementOnTwigViewProvider(element);
        if(psiElement == null) {
            return null;
        }

        // end tag given, find start tag
        PsiElement prevSibling = psiElement.getPrevSibling();
        if(prevSibling instanceof TwigCompositeElement && prevSibling.getNode().getElementType() == TwigElementTypes.TAG) {
            return (TwigCompositeElement) prevSibling;
        }

        return null;
    }

    private static class MyTranslationGotoCompletionProvider extends GotoCompletionProvider {
        private final String domain;

        MyTranslationGotoCompletionProvider(PsiElement element, String domain) {
            super(element);
            this.domain = domain;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return TranslationUtil.getTranslationLookupElementsOnDomain(getProject(), domain);
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String text = element.getText();

            if(StringUtils.isBlank(text)) {
                return Collections.emptyList();
            }

            return Arrays.asList(
                TranslationUtil.getTranslationPsiElements(getProject(), text, domain)
            );
        }
    }
}
