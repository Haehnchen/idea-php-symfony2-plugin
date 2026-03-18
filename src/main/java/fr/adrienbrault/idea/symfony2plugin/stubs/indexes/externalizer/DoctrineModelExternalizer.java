package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelExternalizer implements DataExternalizer<DoctrineModelSerializable> {

    public static final DoctrineModelExternalizer INSTANCE = new DoctrineModelExternalizer();

    @Override
    public void save(@NotNull DataOutput out, DoctrineModelSerializable value) throws IOException {
        out.writeUTF(value.getClassName());
        writeNullableString(out, value.getRepositoryClass());
        writeNullableString(out, value.getTableName());
    }

    @Override
    public DoctrineModelSerializable read(@NotNull DataInput in) throws IOException {
        return new DoctrineModel(in.readUTF(), readNullableString(in), readNullableString(in));
    }

    private static void writeNullableString(@NotNull DataOutput out, String value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) {
            out.writeUTF(value);
        }
    }

    private static String readNullableString(@NotNull DataInput in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }
}
