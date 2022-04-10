package de.espend.idea.php.drupal.index;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import de.espend.idea.php.drupal.gettext.GettextResourceBundle;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GetTextFileIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("de.espend.idea.php.drupal.gettext");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return fileContent -> {
            PsiFile psiFile = fileContent.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            Map<String, Void> msgId = new HashMap<>();

            try {
                GettextResourceBundle gettextResourceBundle = new GettextResourceBundle(fileContent.getFile().getInputStream());
                Enumeration<String> tests = gettextResourceBundle.getKeys();
                for(String test: new HashSet<>(Collections.list(tests))) {
                    if(StringUtils.isNotBlank(test)) {
                        msgId.put(test, null);
                    }
                }
            } catch (IOException e) {
                return msgId;
            }
            return msgId;
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
        return file -> "po".equals(file.getExtension());
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
