package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A synthetic PsiReference for Twig template usages.
 * Wraps a source element (like an include tag) and resolves to the target TwigFile.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateUsageReference extends PsiReferenceBase<PsiElement> {

    private final PsiElement targetElement;
    private final boolean isComponentUsage;

    public TwigTemplateUsageReference(@NotNull PsiElement sourceElement, @NotNull PsiElement targetElement, @NotNull TextRange rangeInElement) {
        this(sourceElement, targetElement, rangeInElement, false);
    }

    public TwigTemplateUsageReference(@NotNull PsiElement sourceElement, @NotNull PsiElement targetElement, @NotNull TextRange rangeInElement, boolean isComponentUsage) {
        super(sourceElement, rangeInElement, false);
        this.targetElement = targetElement;
        this.isComponentUsage = isComponentUsage;
    }

    public boolean isComponentUsage() {
        return isComponentUsage;
    }

    @Override
    public @Nullable PsiElement resolve() {
        return targetElement;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

    /**
     * Updates the template path or component name in the source element when the referenced Twig file is moved.
     *
     * <ul>
     *   <li>UX component usages ({@code {{ component('…') }}} / {@code {% component … %}}) →
     *       the component name is replaced. {@code XmlTag}s are renamed via direct document
     *       replacement (handles both open and close tags, including multi-colon names like
     *       {@code twig:foo:Bar}). PHP-backed components are skipped because their name comes
     *       from the class, not the template path.
     *   <li>Twig tag usages (include / extends / embed / …) → handled exclusively by
     *       {@link TwigMoveFileHandler#retargetUsages}, which runs after all
     *       {@code bindToElement()} calls. No-op here.
     *   <li>PHP render() calls → handled by {@code TemplateReference.bindToElement()}. No-op here.
     * </ul>
     */
    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (!(element instanceof PsiFile newFile)) {
            return myElement;
        }

        // Anonymous UX component usages: update the component name from the new file path.
        if (isComponentUsage) {
            return bindComponentUsage(newFile);
        }

        // Twig tag usages (extends/include/…): TwigMoveFileHandler.retargetUsages() handles these.
        // PHP render() calls: TemplateReference.bindToElement() handles these.
        return myElement;
    }

    /**
     * Handles rename of the referenced Twig file.
     *
     * <ul>
     *   <li>XmlTag component usages (e.g. {@code <twig:Foo />}) → both open and close tags are
     *       renamed via direct document replacement (works for multi-colon names).
     *   <li>Other anonymous UX component usages (e.g. {@code component('Foo')}) → range replacement.
     *   <li>In both cases the new component name is derived from the old name + the new filename
     *       stem, avoiding index queries (which are stale at rename time).
     *   <li>Twig tag and PHP render usages → no-op; handled by dedicated rename handlers.
     * </ul>
     */
    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        if (!isComponentUsage) {
            // Twig tag and PHP render usages: dedicated handlers take care of these.
            return myElement;
        }

        String newStem = stripTwigExtension(newElementName);

        if (myElement instanceof XmlTag xmlTag) {
            String prefix = xmlTag.getNamespacePrefix();
            String newTagName = prefix.isBlank() ? newStem : prefix + ":" + newStem;
            return applyXmlTagRename(xmlTag, newTagName);
        }

        // Non-tag component usage (e.g. {{ component('Foo') }} or {% component Foo %}).
        // Derive new name by replacing the last segment: "foo:Bar" + "Baz" → "foo:Baz".
        String oldComponentName = getRangeInElement().substring(myElement.getText());
        int colon = oldComponentName.lastIndexOf(':');
        String newComponentName = colon >= 0
            ? oldComponentName.substring(0, colon + 1) + newStem
            : newStem;

        PsiElement result = TemplateMoveRenameUtil.applyRangeReplacement(
            myElement, getRangeInElement(), newComponentName
        );
        return result != null ? result : myElement;
    }

    /**
     * Updates a UX component name reference when the backing template is moved.
     * <p>
     * PHP-backed components (name from {@code #[AsTwigComponent]} class) are skipped.
     * Anonymous components (name from template path) get the new name from the new file location.
     */
    private PsiElement bindComponentUsage(@NotNull PsiFile newFile) {
        if (!(newFile instanceof TwigFile newTwigFile)) {
            return myElement;
        }

        String oldComponentName = getRangeInElement().substring(myElement.getText());

        // PHP-backed component: name comes from the class, not the file path — skip.
        if (!UxUtil.getTwigComponentPhpClasses(myElement.getProject(), oldComponentName).isEmpty()) {
            return myElement;
        }

        // Anonymous component: derive new name from the new file location.
        Set<String> newNames = UxUtil.getTemplateComponentNames(newTwigFile);
        if (newNames.isEmpty()) {
            return myElement;
        }

        String newComponentName = newNames.iterator().next();

        if (myElement instanceof XmlTag xmlTag) {
            String prefix = xmlTag.getNamespacePrefix();
            String newTagName = prefix.isBlank() ? newComponentName : prefix + ":" + newComponentName;
            return applyXmlTagRename(xmlTag, newTagName);
        }

        PsiElement result = TemplateMoveRenameUtil.applyRangeReplacement(
            myElement, getRangeInElement(), newComponentName
        );
        return result != null ? result : myElement;
    }

    /**
     * Renames an XmlTag (both open and close) by direct document replacement.
     * <p>
     * Searches the tag's raw text for the old tag name and replaces it at both positions
     * (close tag first to preserve opening-tag offsets).
     */
    @NotNull
    private PsiElement applyXmlTagRename(@NotNull XmlTag xmlTag, @NotNull String newTagName) {
        String oldTagName = xmlTag.getName();
        if (oldTagName.equals(newTagName)) {
            return xmlTag;
        }

        PsiFile containingFile = xmlTag.getContainingFile();
        Project project = xmlTag.getProject();
        Document document = PsiDocumentManager.getInstance(project).getDocument(containingFile);
        if (document == null) {
            return xmlTag;
        }

        String tagText = xmlTag.getText();
        int baseOffset = xmlTag.getTextRange().getStartOffset();

        // Replace closing tag first (higher offset) so the opening tag offset is unaffected.
        int closeIdx = tagText.lastIndexOf("</" + oldTagName);
        if (closeIdx >= 0) {
            int abs = baseOffset + closeIdx + 2; // skip "</"
            document.replaceString(abs, abs + oldTagName.length(), newTagName);
        }

        // Replace opening tag.
        int openIdx = tagText.indexOf("<" + oldTagName);
        if (openIdx >= 0) {
            int abs = baseOffset + openIdx + 1; // skip "<"
            document.replaceString(abs, abs + oldTagName.length(), newTagName);
        }

        PsiDocumentManager.getInstance(project).commitDocument(document);
        return xmlTag;
    }

    /** Strips {@code .html.twig} or {@code .twig} from a filename, returning the bare stem. */
    @NotNull
    private static String stripTwigExtension(@NotNull String filename) {
        if (filename.endsWith(".html.twig")) return filename.substring(0, filename.length() - ".html.twig".length());
        if (filename.endsWith(".twig"))      return filename.substring(0, filename.length() - ".twig".length());
        return filename;
    }

    @Override
    public @NotNull String getCanonicalText() {
        if (targetElement instanceof PsiFile) {
            return ((PsiFile) targetElement).getName();
        }
        return targetElement.getText();
    }
}
