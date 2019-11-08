package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfonyplugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfonyplugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
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
            // @TODO: fr.adrienbrault.idea.symfonyplugin.doctrine.EntityHelper.getModelFields()
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

                for(Field field: phpClass.getFields()) {
                    if(field.isConstant()) {
                        continue;
                    }

                    if(AnnotationBackportUtil.hasReference(field.getDocComment(), EntityHelper.ANNOTATION_FIELDS)) {
                        DoctrineModelField modelField = new DoctrineModelField(field.getName());
                        EntityHelper.attachAnnotationInformation(field, modelField.addTarget(field));
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
