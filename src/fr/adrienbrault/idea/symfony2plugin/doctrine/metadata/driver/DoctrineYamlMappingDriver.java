package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dic.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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

        Collection<DoctrineModelField> fields = new ArrayList<DoctrineModelField>();

        PsiElement yamlDocument = psiFile.getFirstChild();
        if(yamlDocument instanceof YAMLDocument) {
            for (YAMLKeyValue yamlKeyValue : PsiTreeUtil.getChildrenOfTypeAsList(yamlDocument, YAMLKeyValue.class)) {
                // first line is class name; check of we are right
                if(args.isEqualClass(YamlHelper.getYamlKeyName(yamlKeyValue))) {
                    fields.addAll(EntityHelper.getModelFieldsSet(yamlKeyValue));
                }
            }
        }

        if(fields.size() == 0) {
            return null;
        }

        return new DoctrineMetadataModel(fields);
    }
}
