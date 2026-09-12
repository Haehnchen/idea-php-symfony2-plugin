package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.TemplateIncludeExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigIncludeStubIndex extends FileBasedIndexExtension<String, TemplateInclude> {

    public static final ID<String, TemplateInclude> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_include_tags");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final TemplateIncludeExternalizer EXTERNALIZER = TemplateIncludeExternalizer.INSTANCE;

    @NotNull
    @Override
    public ID<String, TemplateInclude> getName() {
        return KEY;
    }

    @NotNull
    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, TemplateInclude, FileContent> getIndexer() {
        return inputData -> {
            final Map<String, TemplateInclude> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!(psiFile instanceof TwigFile)) {
                return map;
            }

            Map<String, EnumSet<TYPE>> typesByTemplate = new HashMap<>();
            TwigUtil.visitTemplateIncludes((TwigFile) psiFile, templateInclude -> {
                if (templateInclude.getTemplateName().length() < 255) {
                    String name = TwigUtil.normalizeTemplateName(templateInclude.getTemplateName());
                    typesByTemplate.computeIfAbsent(name, key -> EnumSet.noneOf(TYPE.class)).add(templateInclude.getType());
                }
            });
            typesByTemplate.forEach((name, types) -> map.put(name, new TemplateInclude(name, types)));

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
    public DataExternalizer<TemplateInclude> getValueExternalizer() {
        return EXTERNALIZER;
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
        return 9;
    }

}

