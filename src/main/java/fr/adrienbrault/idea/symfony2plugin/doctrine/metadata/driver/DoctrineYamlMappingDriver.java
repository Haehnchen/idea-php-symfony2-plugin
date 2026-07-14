package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineYamlMappingDriver implements DoctrineMappingDriverInterface {

    @Nullable
    public DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments args) {

        PsiFile psiFile = args.getPsiFile();
        if(!(psiFile instanceof YAMLFile)) {
            return null;
        }

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues((YAMLFile) psiFile)) {
            // first line is class name; check of we are right
            if(args.isEqualClass(YamlHelper.getYamlKeyName(yamlKeyValue))) {
                model.setTable(YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "table"));
                fields.addAll(EntityHelper.getModelFieldsSet(yamlKeyValue));
                addEmbeddedFields(args, (YAMLFile) psiFile, yamlKeyValue, fields);
            }
        }

        if(model.isEmpty()) {
            return null;
        }

        return model;
    }

    private void addEmbeddedFields(
        @NotNull DoctrineMappingDriverArguments args,
        @NotNull YAMLFile yamlFile,
        @NotNull YAMLKeyValue modelKeyValue,
        @NotNull Collection<DoctrineModelField> fields
    ) {
        String modelClass = YamlHelper.getYamlKeyName(modelKeyValue);
        YAMLKeyValue embeddedKeyValue = YamlHelper.getYamlKeyValue(modelKeyValue, "embedded");
        if (StringUtils.isBlank(modelClass) || embeddedKeyValue == null || !(embeddedKeyValue.getValue() instanceof YAMLMapping mapping)) {
            return;
        }

        Collection<DoctrineEmbeddedFieldUtil.Mapping> mappings = new ArrayList<>();
        for (YAMLKeyValue propertyKeyValue : mapping.getKeyValues()) {
            String propertyName = YamlHelper.getYamlKeyName(propertyKeyValue);
            String embeddedClass = YamlHelper.getYamlKeyValueAsString(propertyKeyValue, "class");
            if (propertyName != null && embeddedClass != null) {
                mappings.add(new DoctrineEmbeddedFieldUtil.Mapping(propertyName, embeddedClass, getColumnPrefix(propertyKeyValue, propertyName)));
            }
        }

        DoctrineEmbeddedFieldUtil.addEmbeddedFields(modelClass, mappings, className -> {
            DoctrineMetadataModel metadata = getLocalEmbeddableMetadata(yamlFile, className);
            return metadata != null ? metadata : DoctrineMetadataUtil.getModelFields(args.getProject(), className);
        }, fields);
    }

    @Nullable
    private DoctrineMetadataModel getLocalEmbeddableMetadata(@NotNull YAMLFile yamlFile, @NotNull String className) {
        for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues(yamlFile)) {
            String name = YamlHelper.getYamlKeyName(yamlKeyValue);
            if (name == null || !StringUtils.stripStart(name, "\\").equals(StringUtils.stripStart(className, "\\"))) {
                continue;
            }

            Collection<DoctrineModelField> fields = EntityHelper.getModelFieldsSet(yamlKeyValue);
            return fields.isEmpty() ? null : new DoctrineMetadataModel(fields);
        }

        return null;
    }

    @NotNull
    private String getColumnPrefix(@NotNull YAMLKeyValue propertyKeyValue, @NotNull String propertyName) {
        String columnPrefix = YamlHelper.getYamlKeyValueAsString(propertyKeyValue, "columnPrefix");
        if ("false".equalsIgnoreCase(columnPrefix)) {
            return "";
        }

        return columnPrefix != null ? columnPrefix : propertyName + "_";
    }

}
