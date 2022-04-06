package de.espend.idea.php.drupal.index;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import de.espend.idea.php.drupal.utils.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigEntityTypeAnnotationIndex extends FileBasedIndexExtension<String, String> {

    public static final ID<String, String> KEY = ID.create("de.espend.idea.php.drupal.config_entity_type_annotation");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            final Map<String, String> map = new THashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile)) {
                return map;
            }

            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            if(!IndexUtil.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            psiFile.accept(new MyPsiRecursiveElementWalkingVisitor(map));

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
    public DataExternalizer<String> getValueExternalizer() {
        return StringDataExternalizer.STRING_DATA_EXTERNALIZER;
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
        return 1;
    }

    @NotNull
    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    private static class StringDataExternalizer implements DataExternalizer<String> {

        public static final StringDataExternalizer STRING_DATA_EXTERNALIZER = new StringDataExternalizer();
        private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

        @Override
        public void save(@NotNull DataOutput out, String value) throws IOException {

            if(value == null) {
                value = "";
            }

            this.myStringEnumerator.save(out, value);
        }

        @Override
        public String read(@NotNull DataInput in) throws IOException {

            String value = this.myStringEnumerator.read(in);

            // EnumeratorStringDescriptor writes out "null" as string, so workaround here
            if("null".equals(value)) {
                value = "";
            }

            // it looks like this is our "null keys not supported" #238, #277
            // so dont force null values here

            return value;
        }
    }

    private class MyPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementVisitor {
        @NotNull
        private final Map<String, String> map;

        private MyPsiRecursiveElementWalkingVisitor(@NotNull Map<String, String> map) {
            this.map = map;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(!(element instanceof PhpDocTag)) {
                super.visitElement(element);
                return;
            }

            String annotationName = StringUtils.stripStart(((PhpDocTag) element).getName(), "@");
            if(!"ConfigEntityType".equals(annotationName)) {
                super.visitElement(element);
                return;
            }

            PsiElement phpDocComment = element.getParent();
            if(!(phpDocComment instanceof PhpDocComment)) {
                super.visitElement(element);
                return;
            }

            PhpPsiElement phpClass = ((PhpDocComment) phpDocComment).getNextPsiSibling();
            if(!(phpClass instanceof PhpClass)) {
                super.visitElement(element);
                return;
            }

            String tagValue = element.getText();
            Matcher matcher = Pattern.compile("id\\s*=\\s*\"([\\w\\.-]+)\"").matcher(tagValue);
            if (matcher.find()) {
                map.put(matcher.group(1), StringUtils.stripStart(((PhpClass) phpClass).getFQN(), "\\"));
            }

            super.visitElement(element);
        }
    }
}
