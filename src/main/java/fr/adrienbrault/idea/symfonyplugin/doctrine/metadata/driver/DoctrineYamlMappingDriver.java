package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.driver;

import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfonyplugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfonyplugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

        Collection<DoctrineModelField> fields = new ArrayList<>();
        DoctrineMetadataModel model = new DoctrineMetadataModel(fields);

        for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues((YAMLFile) psiFile)) {
            // first line is class name; check of we are right
            if(args.isEqualClass(YamlHelper.getYamlKeyName(yamlKeyValue))) {
                model.setTable(YamlHelper.getYamlKeyValueAsString(yamlKeyValue, "table"));
                fields.addAll(EntityHelper.getModelFieldsSet(yamlKeyValue));
            }
        }

        if(model.isEmpty()) {
            return null;
        }

        return model;
    }
}
