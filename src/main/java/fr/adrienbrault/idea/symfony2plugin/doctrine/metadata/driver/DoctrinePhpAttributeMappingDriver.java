package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * example:
 *  - "#[Column(type: "decimal", precision: 2, scale: 1)]"
 *
 * @link https://www.doctrine-project.org/projects/doctrine-orm/en/2.9/reference/attributes-reference.html#attrref_table
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrinePhpAttributeMappingDriver implements DoctrineMappingDriverInterface {
    @Override
    public DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments arguments) {
        PsiFile psiFile = arguments.getPsiFile();
        if(!(psiFile instanceof PhpFile)) {
            return null;
        }

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(arguments.getProject(), arguments.getClassName())) {
            for (PhpAttribute attribute : phpClass.getAttributes()) {
                String fqn = attribute.getFQN();
                if (fqn == null) {
                    continue;
                }

                if (!PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Table")) {
                    continue;
                }

                String name = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
                if (name != null) {
                    model.setTable(name);
                }
            }

            Map<String, Map<String, String>> maps = new HashMap<>();
            for(Field field: phpClass.getFields()) {
                if (field.isConstant()) {
                    continue;
                }

                DoctrineModelField doctrineModelField = new DoctrineModelField(field.getName());
                doctrineModelField.addTarget(field);

                boolean isField = false;
                for (PhpAttribute attribute : field.getAttributes()) {
                    String fqn = attribute.getFQN();
                    if (fqn == null) {
                        continue;
                    }

                    if (PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Column")) {
                        isField = true;

                        String name = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
                        if (name != null) {
                            doctrineModelField.setColumn(name);
                        }

                        String type = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 1, "type");
                        if (type != null) {
                            doctrineModelField.setTypeName(type);
                        }

                        // Enum type (PHP 8.1+)
                        String enumType = PhpElementsUtil.findAttributeArgumentByNameAsClassFqn("enumType", attribute);
                        if (enumType != null) {
                            doctrineModelField.setEnumType("\\" + StringUtils.stripStart(enumType, "\\"));
                        }
                    }

                    if (PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Embedded")) {
                        addEmbeddedFields(arguments, phpClass, field, attribute, fields);
                    }

                    if (PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\OneToOne", "\\Doctrine\\ORM\\Mapping\\ManyToOne", "\\Doctrine\\ORM\\Mapping\\OneToMany", "\\Doctrine\\ORM\\Mapping\\ManyToMany")) {
                        isField = true;

                        String substring = fqn.substring(fqn.lastIndexOf("\\") + 1);
                        doctrineModelField.setRelationType(substring);

                        // not resolving same entity namespace prefix: EntityHelper.resolveDoctrineLikePropertyClass
                        // possible not a wide range usages for attributes
                        String targetEntity = PhpElementsUtil.getAttributeArgumentStringByName(attribute, "targetEntity");
                        if (StringUtils.isNotBlank(targetEntity)) {
                            doctrineModelField.setRelation("\\" + StringUtils.stripStart(targetEntity, "\\"));
                        } else {
                            // #[ORM\ManyToOne]
                            // private ?MyBike $myBike;
                            PhpTypeDeclaration typeDeclaration = field.getTypeDeclaration();
                            if (typeDeclaration != null) {
                                Collection<ClassReference> classReferences = typeDeclaration.getClassReferences().stream()
                                    .filter(classReference -> !"null".equals(classReference.getCanonicalText()))
                                    .toList();

                                if (!classReferences.isEmpty()) {
                                    String fqnClass = classReferences.iterator().next().getFQN();
                                    if (fqnClass != null) {
                                        doctrineModelField.setRelation(fqnClass);
                                    }
                                }
                            }
                        }
                    }
                }

                if (isField) {
                    String typeString = field.getType().toString();
                    if (StringUtils.isNotBlank(typeString)) {
                        doctrineModelField.setPropertyTypes(Arrays.asList(typeString.split("\\|")));
                    }

                    fields.add(doctrineModelField);
                }
            }
        }

        return model;
    }

    private void addEmbeddedFields(
        @NotNull DoctrineMappingDriverArguments arguments,
        @NotNull PhpClass containingClass,
        @NotNull Field field,
        @NotNull PhpAttribute attribute,
        @NotNull Collection<DoctrineModelField> fields
    ) {
        String embeddedClass = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "class");
        if (StringUtils.isBlank(embeddedClass)) {
            embeddedClass = getDeclaredClass(field);
        }

        if (StringUtils.isBlank(embeddedClass)) {
            return;
        }

        String propertyName = field.getName();
        String columnPrefix = getColumnPrefix(attribute, propertyName);
        DoctrineEmbeddedFieldUtil.addEmbeddedFields(
            containingClass.getPresentableFQN(),
            List.of(new DoctrineEmbeddedFieldUtil.Mapping(propertyName, embeddedClass, columnPrefix)),
            className -> DoctrineMetadataUtil.getModelFields(arguments.getProject(), className),
            fields
        );
    }

    private String getDeclaredClass(@NotNull Field field) {
        PhpTypeDeclaration typeDeclaration = field.getTypeDeclaration();
        if (typeDeclaration == null) {
            return null;
        }

        for (ClassReference classReference : typeDeclaration.getClassReferences()) {
            if (!"null".equals(classReference.getCanonicalText()) && StringUtils.isNotBlank(classReference.getFQN())) {
                return classReference.getFQN();
            }
        }

        return null;
    }

    @NotNull
    private String getColumnPrefix(@NotNull PhpAttribute attribute, @NotNull String propertyName) {
        PsiElement columnPrefixElement = PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 1, "columnPrefix");
        if (columnPrefixElement != null && "false".equalsIgnoreCase(columnPrefixElement.getText())) {
            return "";
        }

        String columnPrefix = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 1, "columnPrefix");
        return columnPrefix != null ? columnPrefix : propertyName + "_";
    }

}
