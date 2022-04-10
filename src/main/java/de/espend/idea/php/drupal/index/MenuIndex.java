package de.espend.idea.php.drupal.index;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MenuIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("de.espend.idea.php.drupal.menu_keys");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {

        return inputData -> {

            Map<String, Void> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof YAMLFile) || !psiFile.getName().endsWith(".menu.yml")) {
                return map;
            }

            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues((YAMLFile) psiFile)) {
                String keyText = yamlKeyValue.getKeyText();
                if(StringUtils.isBlank(keyText)) {
                    continue;
                }

                map.put(keyText, null);
            }

            return map;
        };
    }

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }


    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == YAMLFileType.YML;
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
