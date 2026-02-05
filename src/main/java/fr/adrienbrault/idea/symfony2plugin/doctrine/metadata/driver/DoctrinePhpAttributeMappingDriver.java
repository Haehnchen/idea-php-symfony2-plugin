package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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

                String name = PhpElementsUtil.findAttributeArgumentByNameAsString("name", attribute);
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

                        String name = PhpElementsUtil.findAttributeArgumentByNameAsString("name", attribute);
                        if (name != null) {
                            doctrineModelField.setColumn(name);
                        }

                        String type = PhpElementsUtil.findAttributeArgumentByNameAsString("type", attribute);
                        if (type != null) {
                            doctrineModelField.setTypeName(type);
                        }

                        // Enum type (PHP 8.1+)
                        String enumType = PhpElementsUtil.findAttributeArgumentByNameAsClassFqn("enumType", attribute);
                        if (enumType != null) {
                            doctrineModelField.setEnumType("\\" + StringUtils.stripStart(enumType, "\\"));
                        }
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
}
