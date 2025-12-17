package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ObjectStreamDataExternalizer<T extends Serializable> implements DataExternalizer<T> {

    @Override
    public void save(@NotNull DataOutput out, T value) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutput output = new ObjectOutputStream(stream);

        output.writeObject(value);

        out.writeInt(stream.size());
        out.write(stream.toByteArray());
    }

    @Override
    public T read(@NotNull DataInput in) throws IOException {
        int bufferSize = in.readInt();
        byte[] buffer = new byte[bufferSize];
        in.readFully(buffer, 0, bufferSize);

        ByteArrayInputStream stream = new ByteArrayInputStream(buffer);
        ObjectInput input  = new ObjectInputStream(stream);

        T object = null;
        try {
            @SuppressWarnings("unchecked")
            T readObject = (T) input.readObject();
            object = readObject;
        } catch (ClassNotFoundException | ClassCastException ignored) {
        }

        return object;
    }
}
