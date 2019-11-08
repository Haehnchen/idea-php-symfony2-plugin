package fr.adrienbrault.idea.symfonyplugin.stubs.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfonyplugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfonyplugin.templating.util.PhpMethodVariableResolveUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTwigTemplateUsageStubIndex extends FileBasedIndexExtension<String, TemplateUsage> {

    public static final ID<String, TemplateUsage> KEY = ID.create("fr.adrienbrault.idea.symfonyplugin.twig_php_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static int MAX_FILE_BYTE_SIZE = 2097152;
    private static ObjectStreamDataExternalizer<TemplateUsage> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, TemplateUsage> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, TemplateUsage, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            if(!(inputData.getPsiFile() instanceof PhpFile) && isValidForIndex(inputData)) {
                return Collections.emptyMap();
            }

            Map<String, Set<String>> items = new HashMap<>();

            PhpMethodVariableResolveUtil.visitRenderTemplateFunctions(psiFile, triple -> {
                String templateName = triple.getFirst();

                if(!items.containsKey(templateName)) {
                    items.put(templateName, new HashSet<>());
                }

                items.get(templateName).add(StringUtils.stripStart(triple.getSecond().getFQN(), "\\"));
            });

            Map<String, TemplateUsage> map = new HashMap<>();

            items.forEach(
                (key, value) -> map.put(key, new TemplateUsage(key, value))
            );

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
    public DataExternalizer<TemplateUsage> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
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
        return 3;
    }

    private static boolean isValidForIndex(FileContent inputData) {
        return inputData.getFile().getLength() < MAX_FILE_BYTE_SIZE;
    }
}



