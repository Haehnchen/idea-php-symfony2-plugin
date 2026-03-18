package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.UxComponent;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxComponentExternalizer implements DataExternalizer<UxComponent> {

    public static final UxComponentExternalizer INSTANCE = new UxComponentExternalizer();

    private static final UxUtil.TwigComponentType[] TYPE_VALUES = UxUtil.TwigComponentType.values();

    @Override
    public void save(@NotNull DataOutput out, UxComponent value) throws IOException {
        writeNullableString(out, value.name());
        out.writeUTF(value.phpClass());
        writeNullableString(out, value.template());
        out.writeByte(value.type().ordinal());
    }

    @Override
    public UxComponent read(@NotNull DataInput in) throws IOException {
        String name = readNullableString(in);
        String phpClass = in.readUTF();
        String template = readNullableString(in);
        UxUtil.TwigComponentType type = TYPE_VALUES[in.readByte()];
        return new UxComponent(name, phpClass, template, type);
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
