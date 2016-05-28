package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
 * @see com.jetbrains.php.lang.psi.stubs.indexes.StringSetDataExternalizer
 */
public class StringSetDataExternalizer implements DataExternalizer<Set<String>> {

    public synchronized void save(@NotNull DataOutput out, Set<String> value) throws IOException {
        out.writeInt(value.size());
        Iterator var = value.iterator();

        while(var.hasNext()) {
            String s = (String)var.next();
            EnumeratorStringDescriptor.INSTANCE.save(out, s);
        }
    }

    public synchronized Set<String> read(@NotNull DataInput in) throws IOException {
        Set<String> set = new THashSet<>();

        for(int r = in.readInt(); r > 0; --r) {
            set.add(EnumeratorStringDescriptor.INSTANCE.read(in));
        }

        return set;
    }
}