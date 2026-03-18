package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerBuilderCallExternalizer implements DataExternalizer<ContainerBuilderCall> {

    public static final ContainerBuilderCallExternalizer INSTANCE = new ContainerBuilderCallExternalizer();

    @Override
    public void save(@NotNull DataOutput out, ContainerBuilderCall value) throws IOException {
        writeNullableString(out, value.getScope());
        writeNullableString(out, value.getName());
        Collection<String> parameter = value.getParameter();
        if (parameter == null) {
            out.writeInt(0);
        } else {
            out.writeInt(parameter.size());
            for (String p : parameter) {
                out.writeUTF(p);
            }
        }
    }

    @Override
    public ContainerBuilderCall read(@NotNull DataInput in) throws IOException {
        ContainerBuilderCall call = new ContainerBuilderCall();
        call.setScope(readNullableString(in));
        call.setName(readNullableString(in));
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            call.addParameter(in.readUTF());
        }
        return call;
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
