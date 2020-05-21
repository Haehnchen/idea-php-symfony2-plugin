package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrinePhpMappingDriver implements DoctrineMappingDriverInterface {

    @Override
    public DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments args) {
        PsiFile psiFile = args.getPsiFile();
        if(!(psiFile instanceof PhpFile)) {
            return null;
        }

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(args.getProject(), args.getClassName())) {

            // remove duplicate code
            // @TODO: fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper.getModelFields()
            PhpDocComment docComment = phpClass.getDocComment();
            if(docComment == null) {
                continue;
            }

            // Doctrine ORM
            // @TODO: external split
            if(AnnotationBackportUtil.hasReference(docComment, "\\Doctrine\\ORM\\Mapping\\Entity", "\\TYPO3\\Flow\\Annotations\\Entity")) {

                // @TODO: reuse annotations plugin
                PhpDocTag phpDocTag = AnnotationBackportUtil.getReference(docComment, "\\Doctrine\\ORM\\Mapping\\Table");
                if(phpDocTag != null) {
                    Matcher matcher = Pattern.compile("name[\\s]*=[\\s]*[\"|']([\\w_\\\\]+)[\"|']").matcher(phpDocTag.getText());
                    if (matcher.find()) {
                        model.setTable(matcher.group(1));
                    }
                }

                Map<String, String> useImportMap = AnnotationUtil.getUseImportMap(docComment);
                for(Field field: phpClass.getFields()) {
                    if (field.isConstant()) {
                        continue;
                    }

                    if (AnnotationBackportUtil.hasReference(field.getDocComment(), EntityHelper.ANNOTATION_FIELDS)) {
                        DoctrineModelField modelField = new DoctrineModelField(field.getName());
                        EntityHelper.attachAnnotationInformation(phpClass, field, modelField.addTarget(field), useImportMap);
                        fields.add(modelField);
                    }
                }
            }
        }

        if(fields.size() == 0) {
            return null;
        }

        return model;
    }
}
