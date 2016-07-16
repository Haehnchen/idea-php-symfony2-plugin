package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TwigBlockParser {

    private boolean withSelfBlock = false;

    private Map<String, VirtualFile> twigFilesByName;

    public TwigBlockParser(Map<String, VirtualFile> twigFilesByName) {
        this.twigFilesByName = twigFilesByName;
    }

    public List<TwigBlock> walk(@Nullable PsiFile file) {
        return this.walk(file, "self", new ArrayList<>(), 0);
    }

    public List<TwigBlock> walk(@Nullable PsiFile file, String shortcutName, List<TwigBlock> current, int depth) {

        if(file == null) {
            return current;
        }

        // dont match on self file !?
        if(depth > 0 || (withSelfBlock && depth == 0)) {
            if(file instanceof TwigFile) {
                Collection<TwigBlock> blocksInFile = TwigHelper.getBlocksInFile((TwigFile) file);
                // @TODO: remove this here just presentation
                for (TwigBlock twigBlock : blocksInFile) {
                    twigBlock.setShortcutName(shortcutName);;
                }
                current.addAll(blocksInFile);
            }
        }

        // limit recursive calls
        if(depth++ > 20) {
            return current;
        }

        final Map<VirtualFile, String> virtualFiles = new HashMap<>();

        // {% extends 'foo' %}
        // find extend in self
        for(TwigExtendsTag extendsTag : PsiTreeUtil.getChildrenOfTypeAsList(file, TwigExtendsTag.class)) {
            for (String s : TwigHelper.getTwigExtendsTagTemplates(extendsTag)) {
                String templateName = TwigHelper.normalizeTemplateName(s);
                if(twigFilesByName.containsKey(templateName)) {
                    virtualFiles.put(twigFilesByName.get(templateName), templateName);
                }
            }
        }

        // {% use 'foo' %}
        for(TwigCompositeElement twigCompositeElement: PsiTreeUtil.getChildrenOfTypeAsList(file, TwigCompositeElement.class)) {
            if(twigCompositeElement.getNode().getElementType() == TwigElementTypes.TAG) {
                twigCompositeElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {

                        if(TwigHelper.getTwigTagUseNamePattern().accepts(element)) {

                            String templateName = PsiElementUtils.trimQuote(element.getText());
                            if(StringUtils.isNotBlank(templateName)) {
                                String templateNameNormalized = TwigHelper.normalizeTemplateName(TwigHelper.normalizeTemplateName(templateName));
                                if(twigFilesByName.containsKey(templateNameNormalized)) {
                                    virtualFiles.put(twigFilesByName.get(templateName), templateNameNormalized);
                                }
                            }

                        }

                        super.visitElement(element);
                    }
                });
            }
        }

        for(Map.Entry<VirtualFile, String> entry : virtualFiles.entrySet()) {

            // can be null if deleted during iteration
            VirtualFile key = entry.getKey();
            if(key == null) {
                continue;
            }

            PsiFile psiFile = PsiManager.getInstance(file.getProject()).findFile(key);
            if(psiFile instanceof TwigFile) {
                this.walk(psiFile, TwigUtil.getFoldingTemplateNameOrCurrent(entry.getValue()), current, depth);
            }
        }

        return current;
    }

    public TwigBlockParser withSelfBlocks(boolean withSelfBlock) {
        this.withSelfBlock = withSelfBlock;
        return this;
    }

}
