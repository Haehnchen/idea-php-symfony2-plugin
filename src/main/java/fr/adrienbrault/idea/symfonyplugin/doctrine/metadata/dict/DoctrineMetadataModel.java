package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.dict;

import fr.adrienbrault.idea.symfonyplugin.doctrine.dict.DoctrineModelField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataModel {

    @NotNull
    private final Collection<DoctrineModelField> modelFields;

    @Nullable
    private String table;

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

    @Nullable
    public String getTable() {
        return table;
    }

    /**
     * make Immutable @TODO:
     */
    public void setTable(@Nullable String table) {
        this.table = table;
    }

    public boolean isEmpty() {
        return this.modelFields.size() == 0 && this.table == null;
    }

    @NotNull
    public Collection<DoctrineModelField> getFields() {
        return modelFields;
    }
}
