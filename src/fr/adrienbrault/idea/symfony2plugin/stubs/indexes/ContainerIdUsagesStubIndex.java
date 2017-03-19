package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerIdUsagesStubIndex extends FileBasedIndexExtension<String, Integer> {

    public static final ID<String, Integer> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_id_usage");

    @NotNull
    @Override
    public ID<String, Integer> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Integer, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Integer> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                map.putAll(getIdUsages((YAMLFile) psiFile));
            } else if(psiFile instanceof XmlFile) {
                map.putAll(getIdUsages((XmlFile) psiFile));
            }

            return map;
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public DataExternalizer<Integer> getValueExternalizer() {
        return EnumeratorIntegerDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == XmlFileType.INSTANCE || file.getFileType() == YAMLFileType.YML;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    private static Map<String, Integer> getIdUsages(@NotNull YAMLFile yamlFile) {
        Map<String, Integer> services = new HashMap<>();

        for(YAMLKeyValue yamlKeyValue: YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            String keyName = yamlKeyValue.getKeyText();
            if(StringUtils.isBlank(keyName)) {
                continue;
            }

            YAMLKeyValue arguments = YamlHelper.getYamlKeyValue(yamlKeyValue, "arguments");
            if(arguments == null) {
                continue;
            }

            YAMLValue value = arguments.getValue();
            if(!(value instanceof YAMLSequence)) {
                continue;
            }

            for (String id : YamlHelper.getYamlArrayValuesAsList((YAMLSequence) value)) {
                String idClean = YamlHelper.trimSpecialSyntaxServiceName(id);
                if(StringUtils.isNotBlank(idClean)) {
                    services.putIfAbsent(idClean, 0);
                    services.put(idClean, services.get(idClean) + 1);
                }
            }
        }

        return services;
    }

    private static Map<String, Integer> getIdUsages(@NotNull XmlFile psiFile) {
        PsiElement firstChild = psiFile.getFirstChild();
        if(!(firstChild instanceof XmlDocument)) {
            return Collections.emptyMap();
        }

        XmlTag rootTag = ((XmlDocument) firstChild).getRootTag();
        if(rootTag == null || !"container".equals(rootTag.getName())) {
            return Collections.emptyMap();
        }

        Map<String, Integer> services = new HashMap<>();

        for (XmlTag xmlTag : rootTag.findSubTags("services")) {
            for (XmlTag service : xmlTag.findSubTags("service")) {
                for (XmlTag argument : service.findSubTags("argument")) {
                    if("service".equalsIgnoreCase(argument.getAttributeValue("type"))) {
                        String id = argument.getAttributeValue("id");
                        if(id != null && StringUtils.isNotBlank(id)) {
                            services.putIfAbsent(id, 0);
                            services.put(id, services.get(id) + 1);
                        }
                    }
                }
            }
        }

        return services;
    }
}