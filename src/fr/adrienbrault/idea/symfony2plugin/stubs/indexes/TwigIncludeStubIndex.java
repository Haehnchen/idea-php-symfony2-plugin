package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TwigIncludeStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_include_tags");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return new DataIndexer<String, Void, FileContent>() {
            @NotNull
            @Override
            public Map<String, Void> map(@NotNull FileContent inputData) {

                final Map<String, Void> map = new THashMap<>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return map;
                }

                if(!(psiFile instanceof TwigFile)) {
                    return map;
                }

                PsiTreeUtil.collectElements(psiFile, new PsiElementFilter() {
                    @Override
                    public boolean isAccepted(PsiElement psiElement) {

                        // {% include %}
                        if(psiElement instanceof TwigTagWithFileReference && psiElement.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                            for (String templateName : TwigHelper.getIncludeTagStrings((TwigTagWithFileReference) psiElement)) {
                                map.put(templateName, null);
                            }
                        }

                        if(psiElement instanceof TwigCompositeElement) {

                            // {{ include() }}
                            PsiElement includeTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getPrintBlockFunctionPattern("include", "source"));
                            if(includeTag != null) {
                                String templateName = includeTag.getText();
                                if(StringUtils.isNotBlank(templateName)) {
                                    map.put(templateName, null);
                                }
                            }

                            // {% embed "foo.html.twig"
                            PsiElement embedTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getEmbedPattern());
                            if(embedTag != null) {
                                String templateName = embedTag.getText();
                                if(StringUtils.isNotBlank(templateName)) {
                                    map.put(templateName, null);
                                }
                            }

                        }

                        return false;
                    }
                });

                return map;
            }

        };

    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.getFileType() == TwigFileType.INSTANCE;
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 2;
    }

}



