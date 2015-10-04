package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dic.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

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

        Collection<DoctrineModelField> fields = new ArrayList<DoctrineModelField>();

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

        return new DoctrineMetadataModel(fields);
    }
}
