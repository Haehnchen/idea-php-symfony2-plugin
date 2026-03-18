package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceExternalizer implements DataExternalizer<FileResource> {

    public static final FileResourceExternalizer INSTANCE = new FileResourceExternalizer();

    private static final FileResourceContextTypeEnum[] CONTEXT_TYPE_VALUES = FileResourceContextTypeEnum.values();

    @Override
    public void save(@NotNull DataOutput out, FileResource value) throws IOException {
        writeNullableString(out, value.getResource());
        FileResourceContextTypeEnum contextType = value.getContextType();
        out.writeBoolean(contextType != null);
        if (contextType != null) {
            out.writeByte(contextType.ordinal());
        }
        TreeMap<String, String> contextValues = value.getContextValues();
        out.writeBoolean(contextValues != null);
        if (contextValues != null) {
            out.writeInt(contextValues.size());
            for (Map.Entry<String, String> entry : contextValues.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }
        }
    }

    @Override
    public FileResource read(@NotNull DataInput in) throws IOException {
        String resource = readNullableString(in);
        FileResourceContextTypeEnum contextType = null;
        if (in.readBoolean()) {
            contextType = CONTEXT_TYPE_VALUES[in.readByte()];
        }
        TreeMap<String, String> contextValues = null;
        if (in.readBoolean()) {
            int size = in.readInt();
            contextValues = new TreeMap<>();
            for (int i = 0; i < size; i++) {
                contextValues.put(in.readUTF(), in.readUTF());
            }
        }
        return new FileResource(resource, contextType, contextValues);
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
