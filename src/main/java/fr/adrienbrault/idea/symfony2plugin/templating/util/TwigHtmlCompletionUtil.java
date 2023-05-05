package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigHtmlCompletionUtil {
    public static final PatternCondition<PsiElement> HTML_TAG_TWIG_COMPONENT_PREFIX = new PatternCondition<>("twig prefix") {
        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
            String text = psiElement.getText();
            return text.startsWith("twig:");
        }
    };

    // html inside twig: href=""
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

    public static PsiElementPattern.Capture<PsiElement> getFormActionAttributePattern() {
        return getTagAttributePattern("form", "action");
    }

    /**
     * <foo>bar</foo>
     */
    public static PsiElementPattern.Capture<PsiElement> getTagTextPattern(@NotNull String... tag) {
        return XmlPatterns
            .psiElement().withParent(
                PlatformPatterns.psiElement(XmlText.class).withParent(
                    PlatformPatterns.psiElement(HtmlTag.class).withName(tag)
                )
            )
            .inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    public static PsiElementPattern.Capture<PsiElement> getTagAttributePattern(@NotNull String tag, @NotNull String attribute) {
        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(
                        XmlPatterns.xmlAttribute(attribute).withParent(
                            XmlPatterns.xmlTag().withName(tag)
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );
    }

    // html inside twig: <link href="#" rel="stylesheet" />
    public static PsiElementPattern.Capture<PsiElement> getAssetCssAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                        .xmlAttribute("href")
                        .withParent(XmlPatterns
                            .xmlTag()
                            .withChild(XmlPatterns
                                .xmlAttribute("rel")
                                .withValue(
                                    StandardPatterns.string().equalTo("stylesheet")
                                )
                            )
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );

    }

    // html inside twig: <script src="#"></script>
    public static PsiElementPattern.Capture<PsiElement> getAssetJsAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                        .xmlAttribute("src")
                        .withParent(XmlPatterns
                            .xmlTag().withName("script")
                        )
                    )
            ).inFile(XmlPatterns.psiFile()
                .withName(XmlPatterns
                    .string().endsWith(".twig")
                )
            );

    }

    // html inside twig: <img src="">
    public static PsiElementPattern.Capture<PsiElement> getAssetImageAttributePattern() {

        return PlatformPatterns.psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns
                            .xmlAttribute("src")
                            .withParent(XmlPatterns
                                    .xmlTag().withName("img")
                            )
                    )
            ).inFile(XmlPatterns.psiFile()
                    .withName(XmlPatterns
                            .string().endsWith(".twig")
                    )
            );

    }

    /**
     * "<twig:Alert></twig:Alert>"
     */
    public static PsiElementPattern.Capture<PsiElement> getTwigNamespacePattern() {
        return PlatformPatterns.psiElement().withElementType(XmlTokenType.XML_NAME).with(HTML_TAG_TWIG_COMPONENT_PREFIX);
    }
}
