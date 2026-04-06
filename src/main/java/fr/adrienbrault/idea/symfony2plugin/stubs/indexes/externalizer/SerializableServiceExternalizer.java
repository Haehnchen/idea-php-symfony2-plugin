package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SerializableServiceExternalizer implements DataExternalizer<ServiceSerializable> {

    public static final SerializableServiceExternalizer INSTANCE = new SerializableServiceExternalizer();

    @Override
    public void save(@NotNull DataOutput out, ServiceSerializable value) throws IOException {
        if (!(value instanceof SerializableService service)) {
            throw new IOException("Unexpected type: " + value.getClass());
        }
        out.writeUTF(service.getId());
        writeNullableString(out, service.getClassName());
        writeNullableBoolean(out, service.isPublicNullable());
        writeNullableBoolean(out, service.isLazyNullable());
        writeNullableBoolean(out, service.isAbstractNullable());
        writeNullableBoolean(out, service.isAutowireNullable());
        writeNullableBoolean(out, service.isAutoconfigureNullable());
        writeNullableBoolean(out, service.isDeprecatedNullable());
        writeNullableString(out, service.getAlias());
        writeNullableString(out, service.getDecorates());
        writeNullableString(out, service.getDecorationInnerName());
        writeNullableString(out, service.getParent());
        writeStringCollection(out, service.getResource());
        writeStringCollection(out, service.getExclude());
        writeStringCollection(out, service.getTags());
    }

    @Override
    public SerializableService read(@NotNull DataInput in) throws IOException {
        SerializableService service = new SerializableService(in.readUTF());
        service.setClassName(readNullableString(in));
        service.setIsPublic(readNullableBoolean(in));
        service.setIsLazy(readNullableBoolean(in));
        service.setIsAbstract(readNullableBoolean(in));
        service.setIsAutowire(readNullableBoolean(in));
        service.setIsAutoconfigure(readNullableBoolean(in));
        service.setIsDeprecated(readNullableBoolean(in));
        service.setAlias(readNullableString(in));
        service.setDecorates(readNullableString(in));
        service.setDecorationInnerName(readNullableString(in));
        service.setParent(readNullableString(in));
        service.setResource(readStringCollection(in));
        service.setExclude(readStringCollection(in));
        service.setTags(readStringCollection(in));
        return service;
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

    private static void writeNullableBoolean(@NotNull DataOutput out, Boolean value) throws IOException {
        if (value == null) {
            out.writeByte(0);
        } else if (value) {
            out.writeByte(1);
        } else {
            out.writeByte(2);
        }
    }

    private static Boolean readNullableBoolean(@NotNull DataInput in) throws IOException {
        byte b = in.readByte();
        if (b == 0) return null;
        return b == 1;
    }

    private static void writeStringCollection(@NotNull DataOutput out, Collection<String> values) throws IOException {
        out.writeInt(values.size());
        for (String value : values) {
            out.writeUTF(value);
        }
    }

    private static Collection<String> readStringCollection(@NotNull DataInput in) throws IOException {
        int size = in.readInt();
        Collection<String> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(in.readUTF());
        }
        return result;
    }
}
