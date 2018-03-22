package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import org.apache.commons.lang.StringUtils;
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
                // @TODO: refactor allow multiple
                fields.addAll(EntityHelper.getEntityFields((XmlFile) psiFile));

                // get table for dbal
                String table = xmlTag.getAttributeValue("table");
                if(StringUtils.isNotBlank(table)) {
                    model.setTable(table);
                }
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
