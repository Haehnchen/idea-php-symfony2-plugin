package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineXmlMappingDriver implements DoctrineMappingDriverInterface {

    private static final Collection<String> RELATIONS = Arrays.asList("reference-one", "reference-many", "embed-many", "embed-one");

    @Nullable
    public DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments args) {

        PsiFile psiFile = args.getPsiFile();
        if(!(psiFile instanceof XmlFile)) {
            return null;
        }

        XmlTag rootTag = ((XmlFile) psiFile).getRootTag();
        if(rootTag == null || !rootTag.getName().matches(DoctrineMetadataPattern.DOCTRINE_MAPPING)) {
            return null;
        }

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (XmlTag xmlTag : rootTag.getSubTags()) {
            String name = xmlTag.getAttributeValue("name");
            if(name == null) {
                continue;
            }

            if("entity".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                // Doctrine ORM
                fields.addAll(EntityHelper.getXmlModelFields(xmlTag));
                addEmbeddedFields(args, rootTag, xmlTag, fields);

                // get table for dbal
                String table = xmlTag.getAttributeValue("table");
                if(StringUtils.isNotBlank(table)) {
                    model.setTable(table);
                }
            } else if("embeddable".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                fields.addAll(EntityHelper.getXmlModelFields(xmlTag));
            } else if("document".equals(xmlTag.getName()) && args.isEqualClass(name)) {
                // Doctrine ODM
                getOdmFields(xmlTag, fields);
            }
        }

        if(model.isEmpty()) {
            return null;
        }

        return model;
    }

    private void addEmbeddedFields(
        @NotNull DoctrineMappingDriverArguments args,
        @NotNull XmlTag rootTag,
        @NotNull XmlTag entityTag,
        @NotNull Collection<DoctrineModelField> fields
    ) {
        String entityClass = entityTag.getAttributeValue("name");
        if (StringUtils.isBlank(entityClass)) {
            return;
        }

        for (XmlTag embeddedTag : entityTag.findSubTags("embedded")) {
            String propertyName = embeddedTag.getAttributeValue("name");
            String embeddedClass = embeddedTag.getAttributeValue("class");
            if (StringUtils.isBlank(propertyName) || StringUtils.isBlank(embeddedClass)) {
                continue;
            }

            String resolvedClass = resolveEmbeddedClass(entityClass, embeddedClass);
            DoctrineMetadataModel embeddedMetadata = getLocalEmbeddableMetadata(rootTag, resolvedClass);
            if (embeddedMetadata == null) {
                embeddedMetadata = DoctrineMetadataUtil.getModelFields(args.getProject(), resolvedClass);
            }

            if (embeddedMetadata == null) {
                continue;
            }

            String columnPrefix = getColumnPrefix(embeddedTag, propertyName);
            for (DoctrineModelField embeddedField : embeddedMetadata.getFields()) {
                fields.add(copyEmbeddedField(propertyName, columnPrefix, embeddedField));
            }
        }
    }

    @Nullable
    private DoctrineMetadataModel getLocalEmbeddableMetadata(@NotNull XmlTag rootTag, @NotNull String className) {
        for (XmlTag embeddableTag : rootTag.findSubTags("embeddable")) {
            String name = embeddableTag.getAttributeValue("name");
            if (name == null || !StringUtils.stripStart(name, "\\").equals(StringUtils.stripStart(className, "\\"))) {
                continue;
            }

            Collection<DoctrineModelField> fields = EntityHelper.getXmlModelFields(embeddableTag);
            return fields.isEmpty() ? null : new DoctrineMetadataModel(fields);
        }

        return null;
    }

    @NotNull
    private String resolveEmbeddedClass(@NotNull String entityClass, @NotNull String embeddedClass) {
        String normalizedEmbeddedClass = StringUtils.stripStart(embeddedClass, "\\");
        if (normalizedEmbeddedClass.contains("\\")) {
            return normalizedEmbeddedClass;
        }

        String normalizedEntityClass = StringUtils.stripStart(entityClass, "\\");
        int namespaceSeparator = normalizedEntityClass.lastIndexOf('\\');
        if (namespaceSeparator < 0) {
            return normalizedEmbeddedClass;
        }

        return normalizedEntityClass.substring(0, namespaceSeparator + 1) + normalizedEmbeddedClass;
    }

    @NotNull
    private String getColumnPrefix(@NotNull XmlTag embeddedTag, @NotNull String propertyName) {
        if ("false".equalsIgnoreCase(embeddedTag.getAttributeValue("use-column-prefix"))) {
            return "";
        }

        String columnPrefix = embeddedTag.getAttributeValue("column-prefix");
        return columnPrefix != null ? columnPrefix : propertyName + "_";
    }

    @NotNull
    private DoctrineModelField copyEmbeddedField(
        @NotNull String propertyName,
        @NotNull String columnPrefix,
        @NotNull DoctrineModelField embeddedField
    ) {
        DoctrineModelField field = new DoctrineModelField(propertyName + "." + embeddedField.getName(), embeddedField.getTypeName());
        field.setEnumType(embeddedField.getEnumType());
        field.setRelation(embeddedField.getRelation());
        field.setRelationType(embeddedField.getRelationType());
        field.setPropertyTypes(new ArrayList<>(embeddedField.getPropertyTypes()));

        String column = embeddedField.getColumn();
        field.setColumn(columnPrefix + (StringUtils.isNotBlank(column) ? column : embeddedField.getName()));
        embeddedField.getTargets().forEach(field::addTarget);

        return field;
    }

    private void getOdmFields(@NotNull XmlTag xmlTag, @NotNull Collection<DoctrineModelField> fields) {
        for (XmlTag tag : xmlTag.getSubTags()) {
            String tagName = tag.getName();
            if("field".equals(tagName)) {
                String name = tag.getAttributeValue("name");
                if(StringUtils.isNotBlank(name)) {
                    DoctrineModelField type = new DoctrineModelField(name, tag.getAttributeValue("type"));
                    type.setColumn(tag.getAttributeValue("fieldName"));
                    fields.add(type);
                }
            } else if(RELATIONS.contains(tagName)) {
                String field = tag.getAttributeValue("field");
                if(StringUtils.isNotBlank(field)) {
                    DoctrineModelField type = new DoctrineModelField(field);
                    type.setRelationType(tagName);
                    type.setRelation(tag.getAttributeValue("target-document"));
                    fields.add(type);
                }
            }
        }
    }
}
