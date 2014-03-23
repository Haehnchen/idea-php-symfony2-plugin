package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationCollector;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVistor;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class YamlTranslationStubIndex extends FileBasedIndexExtension<String, String[]> {

    public static final ID<String, String[]> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.yaml_translations");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public DataIndexer<String, String[], FileContent> getIndexer() {

        return new DataIndexer<String, String[], FileContent>() {
            @NotNull
            @Override
            public Map<String, String[]> map(FileContent inputData) {

                Map<String, String[]> map = new THashMap<String, String[]>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
                    return map;
                }

                if(!(psiFile instanceof YAMLFile)) {
                    return map;
                }

                // dont index all yaml files; "Resources/translations" should be good for now
                String relativePath = VfsUtil.getRelativePath(inputData.getFile(), psiFile.getProject().getBaseDir(), '/');
                if(relativePath == null || !relativePath.contains("Resources/translations")) {
                    return map;
                }

                String fileName = inputData.getFile().getName();
                int domainSplit = fileName.indexOf(".");
                if(domainSplit < 0) {
                    return map;
                }

                String domainName = fileName.substring(0, domainSplit);
                System.out.println(domainName + fileName);

                final Set<String> translationKeySet = new HashSet<String>();
                YamlTranslationVistor.collectFileTranslations((YAMLFile) psiFile, new YamlTranslationCollector() {
                    @Override
                    public boolean collect(@NotNull String keyName, YAMLKeyValue yamlKeyValue) {
                        translationKeySet.add(keyName);
                        return true;
                    }
                });

                if(translationKeySet.size() == 0) {
                    return map;
                }

                map.put(domainName, translationKeySet.toArray(new String[translationKeySet.size()]));

                return map;

            }

        };
    }

    @NotNull
    @Override
    public ID<String, String[]> getName() {
        return KEY;
    }


    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<String[]> getValueExternalizer() {
        return new ServicesDefinitionStubIndex.MySetDataExternalizer();
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(VirtualFile file) {
                return file.getFileType() == YAMLFileType.YML;
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
