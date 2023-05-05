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
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
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
    public static class TwigTemplateTagNameProvider implements XmlTagNameProvider {
        @Override
        public void addTagNameVariants(List<LookupElement> elements, @NotNull XmlTag tag, String prefix) {
            PsiElement elementOnTwigViewProvider = TwigUtil.getElementOnTwigViewProvider(tag);

            if (elementOnTwigViewProvider != null && !(elementOnTwigViewProvider.getContainingFile() instanceof TwigFile)) {
                return;
            }

            for (String twigComponentName : UxUtil.getTwigComponentNames(tag.getProject())) {
                elements.add(LookupElementBuilder.create("twig:" + twigComponentName).withIcon(Symfony2Icons.SYMFONY));
            }
        }
    }

    public static class TwigTemplateXmlExtension extends XmlExtension {
        @Override
        public boolean isAvailable(PsiFile file) {
            return true;
        }

        @Override
        public @NotNull List<TagInfo> getAvailableTagNames(@NotNull XmlFile file, @NotNull XmlTag context) {
            return Collections.emptyList();
        }

        @Override
        public @Nullable SchemaPrefix getPrefixDeclaration(XmlTag context, String namespacePrefix) {
            if (namespacePrefix.equals("twig") && context instanceof HtmlTag && context.getName().startsWith("twig")) {
                return new NullableParentShouldOverwriteSchemaPrefix(
                    context.getProject(),
                    context.getContainingFile(),
                    new TextRange(0, 4),
                    "twig"
                );
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

                return text.startsWith("twig:")
                    && UxUtil.getTwigComponentNames(element.getProject()).contains(text.substring(5));
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
