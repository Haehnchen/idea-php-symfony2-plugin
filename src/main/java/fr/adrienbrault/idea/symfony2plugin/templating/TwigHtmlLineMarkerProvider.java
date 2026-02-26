package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TwigHtmlLineMarkerProvider implements LineMarkerProvider {
    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> results) {
        if (psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        PsiFile file = psiElements.get(0).getContainingFile();
        if (!isSupportedFile(file)) {
            return;
        }

        for (PsiElement psiElement : psiElements) {
            if (!(psiElement instanceof XmlToken xmlToken) || xmlToken.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
                continue;
            }

            if (!(xmlToken.getParent() instanceof XmlAttributeValue xmlAttributeValue)) {
                continue;
            }

            LineMarkerInfo<?> blockOverride = attachTwigComponentBlockOverride(xmlAttributeValue, xmlToken);
            if (blockOverride != null) {
                results.add(blockOverride);
            }
        }
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    public static boolean isSupportedFile(@Nullable PsiFile file) {
        return file != null && file.getName().endsWith(".twig");
    }

    @Nullable
    protected LineMarkerInfo<?> attachTwigComponentBlockOverride(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull PsiElement lineMarkerTarget) {
        if (!(xmlAttributeValue.getParent() instanceof XmlAttribute xmlAttribute) || !"name".equals(xmlAttribute.getName())) {
            return null;
        }

        if (!(xmlAttribute.getParent() instanceof XmlTag xmlTag) || !TwigHtmlCompletionUtil.isTwigBlockTag(xmlTag)) {
            return null;
        }

        if (!isTwigBackedXmlTag(xmlTag)) {
            return null;
        }

        String blockName = xmlAttributeValue.getValue();
        if (blockName.isBlank()) {
            return null;
        }

        XmlTag componentTag = findParentComponentTag(xmlTag);
        if (componentTag == null) {
            return null;
        }

        String name = componentTag.getName();
        String rawComponentName = name.startsWith("twig:") ? name.substring(5) : name;

        String componentName = resolveComponentName(xmlAttributeValue.getProject(), rawComponentName);
        if (componentName == null) {
            return null;
        }

        Project project = xmlAttributeValue.getProject();
        if (!hasComponentBlock(project, componentName, blockName)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES)
            .setTargets(NotNullLazyValue.lazy(new TwigComponentBlockTargetsLazyValue(project, componentName, blockName)))
            .setTooltipText("Navigate to block");

        return builder.createLineMarkerInfo(lineMarkerTarget);
    }

    @Nullable
    protected String resolveComponentName(@NotNull Project project, @NotNull String rawComponentName) {
        return UxUtil.resolveTwigComponentName(project, rawComponentName);
    }

    protected boolean hasComponentBlock(@NotNull Project project, @NotNull String componentName, @NotNull String blockName) {
        List<VirtualFile> virtualFiles = new ArrayList<>();
        for (PsiFile templateFile : UxUtil.getComponentTemplates(project, componentName)) {
            VirtualFile virtualFile = templateFile.getVirtualFile();
            if (virtualFile != null) {
                virtualFiles.add(virtualFile);
            }
        }

        if (virtualFiles.isEmpty()) {
            return false;
        }

        Map<VirtualFile, Collection<String>> blockNamesForFiles = TwigUtil.getBlockNamesForFiles(project, virtualFiles);
        for (Collection<String> blockNames : blockNamesForFiles.values()) {
            if (blockNames.contains(blockName)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isTwigBackedXmlTag(@NotNull XmlTag xmlTag) {
        PsiElement twigElement = TwigUtil.getElementOnTwigViewProvider(xmlTag);
        return twigElement != null && twigElement.getContainingFile() instanceof TwigFile;
    }

    @Nullable
    private static XmlTag findParentComponentTag(@NotNull XmlTag tag) {
        XmlTag parentTag = tag.getParentTag();
        while (parentTag != null) {
            if (!TwigHtmlCompletionUtil.isTwigBlockTag(parentTag)) {
                String name = parentTag.getName();
                if (name.startsWith("twig:") || "twig".equals(parentTag.getNamespacePrefix())) {
                    return parentTag;
                }
            }

            parentTag = parentTag.getParentTag();
        }

        return null;
    }

    private record TwigComponentBlockTargetsLazyValue(@NotNull Project project, @NotNull String componentName, @NotNull String blockName) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            Collection<PsiElement> targets = new ArrayList<>();

            for (PsiFile templateFile : UxUtil.getComponentTemplates(project, componentName)) {
                if (!(templateFile instanceof TwigFile twigFile)) {
                    continue;
                }

                for (TwigBlock block : TwigUtil.getBlocksInFile(twigFile)) {
                    if (blockName.equals(block.getName())) {
                        targets.add(block.getTarget());
                    }
                }
            }

            return targets;
        }
    }
}
