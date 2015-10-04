package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dic;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataModel {

    @NotNull
    private final Collection<DoctrineModelField> modelFields;

    public DoctrineMetadataModel(@NotNull Collection<DoctrineModelField> modelFields) {
        this.modelFields = modelFields;
    }

    @Nullable
    public DoctrineModelField getField(@NotNull String name) {
        for (DoctrineModelField field : this.modelFields) {
            if(name.equals(field.getName())) {
                return field;
            }
        }

        return null;
    }
}
