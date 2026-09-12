package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateInclude;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import static fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateIncludeExternalizer implements DataExternalizer<TemplateInclude> {

    public static final TemplateIncludeExternalizer INSTANCE = new TemplateIncludeExternalizer();

    // Positions are persistent IDs: append only, never reorder or reuse.
    private static final TYPE[] SERIALIZED_TYPES = {
        TYPE.INCLUDE, TYPE.INCLUDE_FUNCTION, TYPE.EMBED, TYPE.IMPORT,
        TYPE.FROM, TYPE.FORM_THEME, TYPE.BLOCK_FUNCTION, TYPE.SOURCE_FUNCTION
    };

    @Override
    public void save(@NotNull DataOutput out, TemplateInclude value) throws IOException {
        out.writeUTF(value.getTemplate());
        Set<TYPE> types = value.getTypes();
        out.writeByte(types.size());
        for (int id = 0; id < SERIALIZED_TYPES.length; id++) {
            if (types.contains(SERIALIZED_TYPES[id])) {
                out.writeByte(id);
            }
        }
    }

    @Override
    public TemplateInclude read(@NotNull DataInput in) throws IOException {
        String template = in.readUTF();
        int count = in.readUnsignedByte();
        if (count == 0 || count > SERIALIZED_TYPES.length) {
            throw new IOException("Invalid template include type count: " + count);
        }

        EnumSet<TYPE> types = EnumSet.noneOf(TYPE.class);
        for (int i = 0; i < count; i++) {
            int id = in.readUnsignedByte();
            if (id >= SERIALIZED_TYPES.length || !types.add(SERIALIZED_TYPES[id])) {
                throw new IOException("Invalid or duplicate template include type ID: " + id);
            }
        }
        return new TemplateInclude(template, types);
    }
}
