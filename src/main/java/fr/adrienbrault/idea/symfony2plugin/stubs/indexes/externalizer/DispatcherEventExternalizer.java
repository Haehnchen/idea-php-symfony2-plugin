package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.DispatcherEvent;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DispatcherEventExternalizer implements DataExternalizer<DispatcherEvent> {

    public static final DispatcherEventExternalizer INSTANCE = new DispatcherEventExternalizer();

    @Override
    public void save(@NotNull DataOutput out, DispatcherEvent value) throws IOException {
        writeNullableString(out, value.getFqn());
        writeNullableString(out, value.getInstance());
    }

    @Override
    public DispatcherEvent read(@NotNull DataInput in) throws IOException {
        String fqn = readNullableString(in);
        String instance = readNullableString(in);
        DispatcherEvent event = new DispatcherEvent();
        if (fqn != null) {
            return new DispatcherEvent(fqn, instance);
        }
        return event;
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
