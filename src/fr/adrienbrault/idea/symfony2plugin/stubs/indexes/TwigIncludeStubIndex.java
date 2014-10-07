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
            public Map<String, Void> map(FileContent inputData) {

                final Map<String, Void> map = new THashMap<String, Void>();

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
                        if(psiElement instanceof TwigTagWithFileReference) {
                            PsiElement includeTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getTemplateFileReferenceTagPattern("include"));
                            if(includeTag != null) {
                                String templateName = includeTag.getText();
                                if(!StringUtils.isBlank(templateName)) {
                                    map.put(templateName, null);
                                }
                            }
                        }

                        // {{ include }}
                        if(psiElement instanceof TwigCompositeElement) {
                            PsiElement includeTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getPrintBlockFunctionPattern("include", "source"));
                            if(includeTag != null) {
                                String templateName = includeTag.getText();
                                if(!StringUtils.isBlank(templateName)) {
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

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(VirtualFile file) {
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
        return 1;
    }

}



