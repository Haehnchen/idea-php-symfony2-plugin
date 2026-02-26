package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInspection.XmlSuppressionProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.html.HtmlTag;
import com.intellij.psi.impl.source.xml.SchemaPrefix;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.XmlTagNameProvider;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileViewProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigComponentHtmlTagExtensions {
    @Nullable
    private static XmlTag findParentComponentTag(@NotNull XmlTag tag) {
        XmlTag parentTag = tag.getParentTag();
        while (parentTag != null) {
            if (isTwigComponentTag(parentTag)) {
                return parentTag;
            }

            parentTag = parentTag.getParentTag();
        }

        return null;
    }

    private static boolean isTwigComponentTag(@NotNull XmlTag tag) {
        if (TwigHtmlCompletionUtil.isTwigBlockTag(tag)) {
            return false;
        }

        String name = tag.getName();
        if (name.startsWith("twig:")) {
            return true;
        }

        return "twig".equals(tag.getNamespacePrefix());
    }

    public static class TwigTemplateTagNameProvider implements XmlTagNameProvider {
        @Override
        public void addTagNameVariants(List<LookupElement> elements, @NotNull XmlTag tag, String prefix) {
            if (!Symfony2ProjectComponent.isEnabled(tag.getProject())) {
                return;
            }

            PsiElement elementOnTwigViewProvider = TwigUtil.getElementOnTwigViewProvider(tag);

            if (elementOnTwigViewProvider != null && !(elementOnTwigViewProvider.getContainingFile() instanceof TwigFile)) {
                return;
            }

            for (String twigComponentName : UxUtil.getTwigComponentNames(tag.getProject())) {
                elements.add(LookupElementBuilder.create("twig:" + twigComponentName).withIcon(Symfony2Icons.SYMFONY));
            }

            if (findParentComponentTag(tag) != null) {
                elements.add(
                    LookupElementBuilder.create("twig:block")
                        .withIcon(Symfony2Icons.SYMFONY)
                        .withTypeText("Block", true)
                );
            }
        }
    }

    public static class TwigTemplateXmlExtension extends XmlExtension {
        @Override
        public boolean isAvailable(PsiFile file) {
            if (!Symfony2ProjectComponent.isEnabled(file.getProject())) {
                return false;
            }

            return file.getViewProvider() instanceof TwigFileViewProvider;
        }

        @Override
        public @NotNull List<TagInfo> getAvailableTagNames(@NotNull XmlFile file, @NotNull XmlTag context) {
            return Collections.emptyList();
        }

        @Override
        public @Nullable SchemaPrefix getPrefixDeclaration(XmlTag context, String namespacePrefix) {
            if (namespacePrefix.equals("twig") && context instanceof HtmlTag htmlTag) {
                if (isTwigComponentTag(htmlTag)) {
                    return new NullableParentShouldOverwriteSchemaPrefix(
                        context.getProject(),
                        context.getContainingFile(),
                        new TextRange(0, 4),
                        "twig"
                    );
                }

                if (TwigHtmlCompletionUtil.isTwigBlockTag(htmlTag) && findParentComponentTag(htmlTag) != null) {
                    return new NullableParentShouldOverwriteSchemaPrefix(
                        context.getProject(),
                        context.getContainingFile(),
                        new TextRange(0, 4),
                        "twig"
                    );
                }
            }

            return null;
        }

        private static class NullableParentShouldOverwriteSchemaPrefix extends SchemaPrefix {
            private final Project project;
            private final PsiFile psiFile;

            public NullableParentShouldOverwriteSchemaPrefix(@NotNull Project project, @NotNull PsiFile psiFile, TextRange range, String name) {
                super(null, range, name);
                this.project = project;
                this.psiFile = psiFile;
            }

            @Override
            public PsiFile getContainingFile() {
                return this.psiFile;
            }

            @Override
            public @NotNull Project getProject() {
                return this.project;
            }
        }
    }

    public static class TwigTemplateXmlSuppressionProvider extends XmlSuppressionProvider {

        @Override
        public boolean isProviderAvailable(@NotNull PsiFile file) {
            return true;
        }

        @Override
        public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String inspectionId) {
            if (inspectionId.equals("HtmlUnknownTag") && element instanceof XmlToken xmlToken && element.getNode().getElementType() == XmlTokenType.XML_NAME) {
                String text = xmlToken.getText();

                if (text.startsWith("twig:")
                    && UxUtil.hasTwigComponentName(element.getProject(), text.substring(5))) {
                    return true;
                }

                return "twig:block".equals(text);
            }

            return false;
        }

        @Override
        public void suppressForFile(@NotNull PsiElement element, @NotNull String inspectionId) {

        }

        @Override
        public void suppressForTag(@NotNull PsiElement element, @NotNull String inspectionId) {

        }
    }
}
