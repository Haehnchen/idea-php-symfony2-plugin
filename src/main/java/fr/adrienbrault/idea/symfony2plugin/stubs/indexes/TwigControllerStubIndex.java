package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {{ controller("foo::action") }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigControllerStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_controller_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return inputData -> {
            final Map<String, Void> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!(psiFile instanceof TwigFile)) {
                return map;
            }

            // collect: controller()
            PsiElement[] psiElements = PsiTreeUtil.collectElements(psiFile, psiElement -> {
                if (psiElement.getNode().getElementType() != TwigElementTypes.FUNCTION_CALL) {
                    return false;
                }

                PsiElement firstChild = psiElement.getFirstChild();
                if (firstChild.getNode().getElementType() != TwigTokenTypes.IDENTIFIER) {
                    return false;
                }

                return "controller".equalsIgnoreCase(firstChild.getText());
            });

            // find parameter: controller("foobar::action")
            for (PsiElement functionCall: psiElements) {
                PsiElement includeTag = PsiElementUtils.getChildrenOfType(functionCall, TwigPattern.getPrintBlockOrTagFunctionPattern("controller"));
                if(includeTag != null) {
                    String controllerName = includeTag.getText();
                    if(StringUtils.isNotBlank(controllerName)) {
                        // escaping
                        // "Foobar\\Test"
                        map.put(controllerName.replace("\\\\", "\\"), null);
                    }
                }
            }

            return map;
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
        return VoidDataExternalizer.INSTANCE;
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

    @Override
    public int getVersion() {
        return 1;
    }
}
