package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DataExternalizer for List<String> with proper equals/hashCode support
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StringListDataExternalizer implements DataExternalizer<List<String>> {

    public static final StringListDataExternalizer INSTANCE = new StringListDataExternalizer();

    @Override
    public synchronized void save(@NotNull DataOutput out, List<String> value) throws IOException {
        out.writeInt(value.size());

        for (String s : value) {
            EnumeratorStringDescriptor.INSTANCE.save(out, s);
        }
    }

    @Override
    public synchronized List<String> read(@NotNull DataInput in) throws IOException {
        List<String> list = new ArrayList<>();

        for (int r = in.readInt(); r > 0; --r) {
            list.add(EnumeratorStringDescriptor.INSTANCE.read(in));
        }

        return list;
    }
}
