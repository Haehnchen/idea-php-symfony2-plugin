package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Documentation for Symfony UX Twig component props on their usage, e.g. Ctrl-hover / Ctrl-Q on the
 * {@code variant} attribute of {@code <twig:Alert variant="">}. The prop's type and description come
 * from the component template's {@code {# @prop #}} docblock and its default value from the
 * {@code {%- props -%}} tag ({@link UxUtil#getComponentTemplateProps}).
 *
 * <p>Registered for HTML because the {@code <twig:...>} attribute leaf is HTML in the {@code .html.twig}
 * view. Returns {@code null} for anything that is not a {@code twig:} component attribute, so the
 * platform's built-in HTML documentation still applies elsewhere.
 */
public class TwigComponentAttributeDocumentationProvider extends AbstractDocumentationProvider {

    private static final String TWIG_TAG_PREFIX = "twig:";

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        if (!Symfony2ProjectComponent.isEnabled(file.getProject())) {
            return null;
        }

        // The context leaf is usually the HTML attribute token already; otherwise look through every
        // language root of the templated .html.twig file (the `<twig:...>` markup lives in the HTML root).
        XmlAttribute attribute = findTwigComponentAttribute(contextElement);
        if (attribute != null) {
            return attribute;
        }

        for (PsiFile root : file.getViewProvider().getAllFiles()) {
            attribute = findTwigComponentAttribute(root.findElementAt(targetOffset));
            if (attribute != null) {
                return attribute;
            }
        }

        return null;
    }

    @Override
    public @Nullable String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        UxUtil.TwigComponentProp prop = resolveProp(element);
        if (prop == null) {
            return null;
        }

        StringBuilder doc = new StringBuilder();
        doc.append("<code>").append(escape(prop.name())).append("</code>")
            .append(" : <code>").append(escape(prop.type())).append("</code>");

        if (prop.defaultValue() != null) {
            doc.append("<br/>Default: <code>").append(escape(prop.defaultValue())).append("</code>");
        }

        if (!prop.description().isBlank()) {
            doc.append("<br/><br/>").append(escape(prop.description()));
        }

        return doc.toString();
    }

    @Override
    public @Nullable String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        UxUtil.TwigComponentProp prop = resolveProp(element);
        if (prop == null) {
            return null;
        }

        StringBuilder info = new StringBuilder();
        info.append("<code>").append(escape(prop.name())).append("</code>")
            .append(" : <code>").append(escape(prop.type())).append("</code>");

        if (prop.defaultValue() != null) {
            info.append(" = <code>").append(escape(prop.defaultValue())).append("</code>");
        }

        return info.toString();
    }

    private static @Nullable UxUtil.TwigComponentProp resolveProp(@Nullable PsiElement element) {
        XmlAttribute attribute = findTwigComponentAttribute(element);
        if (attribute == null || !(attribute.getParent() instanceof XmlTag tag)) {
            return null;
        }

        Project project = attribute.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return null;
        }

        String componentName = tag.getName().substring(TWIG_TAG_PREFIX.length());
        String attributeName = StringUtils.stripStart(attribute.getName(), ":");

        for (PsiFile template : UxUtil.getComponentTemplates(project, componentName)) {
            if (template instanceof TwigFile twigFile) {
                UxUtil.TwigComponentProp prop = UxUtil.getComponentTemplateProps(twigFile).get(attributeName);
                if (prop != null) {
                    return prop;
                }
            }
        }

        return null;
    }

    private static @Nullable XmlAttribute findTwigComponentAttribute(@Nullable PsiElement element) {
        XmlAttribute attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute.class, false);
        if (attribute != null
            && attribute.getParent() instanceof XmlTag tag
            && tag.getName().startsWith(TWIG_TAG_PREFIX)) {
            return attribute;
        }

        return null;
    }

    private static String escape(@NotNull String text) {
        return StringUtil.escapeXmlEntities(text);
    }
}
