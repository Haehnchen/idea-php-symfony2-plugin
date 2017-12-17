package fr.adrienbrault.idea.symfony2plugin.twig.utils;

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
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFileUtil {
    /**
     * Visit parent Twig files eg on "embed" tag and provide all files in this path until root file
     */
    @NotNull
    public static Collection<VirtualFile> collectParentFiles(boolean includeSelf, @NotNull PsiFile... psiFiles) {
        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (PsiFile psiFile : psiFiles) {
            VirtualFile sourceFile = psiFile.getVirtualFile();
            if(includeSelf) {
                virtualFiles.add(sourceFile);
            }

            visitParentFiles(psiFile, 0, virtualFiles);
        }

        return virtualFiles;
    }

    private static void visitParentFiles(@NotNull PsiFile file, int depth, Collection<VirtualFile> virtualFiles) {
        // limit recursive calls
        if(depth++ > 20) {
            return;
        }

        Set<VirtualFile> myVirtualFiles = new HashSet<>();

        // {% extends 'foo' %}
        // find extend in self
        for(TwigExtendsTag extendsTag : PsiTreeUtil.getChildrenOfTypeAsList(file, TwigExtendsTag.class)) {
            for (String templateName : TwigUtil.getTwigExtendsTagTemplates(extendsTag)) {
                for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(file.getProject(), templateName)) {
                    VirtualFile virtualFile = psiFile.getVirtualFile();
                    if(!virtualFiles.contains(virtualFile)) {
                        myVirtualFiles.add(virtualFile);
                        virtualFiles.add(virtualFile);
                    }
                }
            }
        }

        // {% use 'foo' %}
        for(TwigCompositeElement twigCompositeElement: PsiTreeUtil.getChildrenOfTypeAsList(file, TwigCompositeElement.class)) {
            if(twigCompositeElement.getNode().getElementType() == TwigElementTypes.TAG) {
                twigCompositeElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(TwigPattern.getTwigTagUseNamePattern().accepts(element)) {
                            String templateName = PsiElementUtils.trimQuote(element.getText());
                            if(StringUtils.isNotBlank(templateName)) {
                                for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(file.getProject(), templateName)) {
                                    VirtualFile virtualFile = psiFile.getVirtualFile();
                                    if(!virtualFiles.contains(virtualFile)) {
                                        myVirtualFiles.add(virtualFile);
                                        virtualFiles.add(virtualFile);
                                    }
                                }
                            }
                        }

                        super.visitElement(element);
                    }
                });
            }
        }

        // visit files in this scope
        for(VirtualFile virtualFile : myVirtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(file.getProject()).findFile(virtualFile);
            if(psiFile instanceof TwigFile) {
                visitParentFiles(psiFile, depth, virtualFiles);
            }
        }
    }
}
