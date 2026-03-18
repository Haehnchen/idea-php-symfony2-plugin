package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StimulusControllerExternalizer implements DataExternalizer<StimulusController> {

    public static final StimulusControllerExternalizer INSTANCE = new StimulusControllerExternalizer();

    private static final StimulusController.SourceType[] SOURCE_TYPE_VALUES = StimulusController.SourceType.values();

    @Override
    public void save(@NotNull DataOutput out, StimulusController value) throws IOException {
        out.writeUTF(value.name());
        out.writeByte(value.sourceType().ordinal());
        writeNullableString(out, value.originalName());
    }

    @Override
    public StimulusController read(@NotNull DataInput in) throws IOException {
        String name = in.readUTF();
        StimulusController.SourceType sourceType = SOURCE_TYPE_VALUES[in.readByte()];
        String originalName = readNullableString(in);
        return new StimulusController(name, sourceType, originalName);
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
