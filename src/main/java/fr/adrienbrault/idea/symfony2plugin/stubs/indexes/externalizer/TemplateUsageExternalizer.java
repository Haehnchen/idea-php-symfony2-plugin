package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateUsageExternalizer implements DataExternalizer<TemplateUsage> {

    public static final TemplateUsageExternalizer INSTANCE = new TemplateUsageExternalizer();

    @Override
    public void save(@NotNull DataOutput out, TemplateUsage value) throws IOException {
        out.writeUTF(value.getTemplate());
        Collection<String> scopes = value.getScopes();
        out.writeInt(scopes.size());
        for (String scope : scopes) {
            out.writeUTF(scope);
        }
    }

    @Override
    public TemplateUsage read(@NotNull DataInput in) throws IOException {
        String template = in.readUTF();
        int size = in.readInt();
        Collection<String> scopes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            scopes.add(in.readUTF());
        }
        return new TemplateUsage(template, scopes);
    }
}
