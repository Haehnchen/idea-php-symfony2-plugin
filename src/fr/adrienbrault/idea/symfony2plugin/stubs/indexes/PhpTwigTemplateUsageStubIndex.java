package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PhpTwigTemplateUsageStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_php_usage");
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

                if(!(inputData.getPsiFile() instanceof PhpFile)) {
                    return map;
                }

                psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(element instanceof MethodReference) {
                            visitMethodReference((MethodReference) element);
                        }
                        super.visitElement(element);
                    }

                    public void visitMethodReference(MethodReference methodReference) {
                        String methodName = methodReference.getName();
                        if(!"render".equals(methodName) && !"renderView".equals(methodName) && !"renderResponse".equals(methodName)) {
                            return;
                        }

                        PsiElement[] parameters = methodReference.getParameters();
                        if(parameters.length == 0 || !(parameters[0] instanceof StringLiteralExpression)) {
                            return;
                        }

                        String contents = ((StringLiteralExpression) parameters[0]).getContents();
                        if(StringUtils.isBlank(contents) || !contents.endsWith(".html.twig")) {
                            return;
                        }

                        map.put(TwigHelper.normalizeTemplateName(contents), null);

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
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
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



