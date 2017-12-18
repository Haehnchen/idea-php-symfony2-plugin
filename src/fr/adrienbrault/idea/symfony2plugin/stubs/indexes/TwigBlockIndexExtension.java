package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockIndexExtension extends FileBasedIndexExtension<String, Set<String>> {

    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_block_names");

    private final KeyDescriptor<String> KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private static final StringSetDataExternalizer DATA_EXTERNALIZER = new StringSetDataExternalizer();

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return fileContent -> {
            Map<String, Set<String>> blocks = new HashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(psiFile instanceof TwigFile) {
                for (TwigBlock twigBlock : TwigUtil.getBlocksInFile((TwigFile) psiFile)) {
                    // we only index file scope
                    // {% embed 'foo.html.twig' %}{% block foo %}{% endembed %}
                    PsiElement embedStatement = PsiElementUtils.getParentOfType(twigBlock.getTarget(), TwigElementTypes.EMBED_STATEMENT);
                    if(embedStatement == null) {
                        blocks.putIfAbsent("block", new HashSet<>());
                        blocks.get("block").add(twigBlock.getName());
                    }
                }

                for(TwigExtendsTag extendsTag : PsiTreeUtil.getChildrenOfTypeAsList(psiFile, TwigExtendsTag.class)) {
                    for (String templateName : TwigUtil.getTwigExtendsTagTemplates(extendsTag)) {
                        blocks.putIfAbsent("extends", new HashSet<>());
                        blocks.get("extends").add(TwigUtil.normalizeTemplateName(templateName));
                    }
                }

                for(TwigCompositeElement twigCompositeElement: PsiTreeUtil.getChildrenOfTypeAsList(psiFile, TwigCompositeElement.class)) {
                    if(twigCompositeElement.getNode().getElementType() == TwigElementTypes.TAG) {
                        twigCompositeElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                            @Override
                            public void visitElement(PsiElement element) {
                                if(TwigPattern.getTwigTagUseNamePattern().accepts(element) && PsiElementUtils.getParentOfType(element, TwigElementTypes.EMBED_STATEMENT) == null) {
                                    String templateName = TwigUtil.normalizeTemplateName(PsiElementUtils.trimQuote(element.getText()));
                                    if(StringUtils.isNotBlank(templateName)) {
                                        blocks.putIfAbsent("use", new HashSet<>());
                                        blocks.get("use").add(templateName);
                                    }
                                }

                                super.visitElement(element);
                            }
                        });
                    }
                }
            }

            return blocks;
        };
    }

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @NotNull
    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
