package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateInclude;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateIncludeExternalizer implements DataExternalizer<TemplateInclude> {

    public static final TemplateIncludeExternalizer INSTANCE = new TemplateIncludeExternalizer();

    private static final TYPE[] TYPE_VALUES = TYPE.values();

    @Override
    public void save(@NotNull DataOutput out, TemplateInclude value) throws IOException {
        out.writeUTF(value.getTemplate());
        TYPE type = value.getType();
        out.writeBoolean(type != null);
        if (type != null) {
            out.writeByte(type.ordinal());
        }
    }

    @Override
    public TemplateInclude read(@NotNull DataInput in) throws IOException {
        String template = in.readUTF();
        TYPE type = null;
        if (in.readBoolean()) {
            type = TYPE_VALUES[in.readByte()];
        }
        return new TemplateInclude(template, type != null ? type : TYPE.INCLUDE);
    }
}
