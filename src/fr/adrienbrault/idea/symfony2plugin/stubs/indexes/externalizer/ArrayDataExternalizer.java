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
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see com.jetbrains.php.lang.psi.stubs.indexes.PhpTraitUsageIndex
 * @deprecated because of results into "violates equals / hashCode"
 */
public class ArrayDataExternalizer implements DataExternalizer<String[]> {

    private final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();

    public synchronized void save(@NotNull DataOutput out, String[] values) throws IOException {

        out.writeInt(values.length);
        for(String value: values) {
            this.myStringEnumerator.save(out, value != null ? value : "");
        }

    }

    public synchronized String[] read(@NotNull DataInput in) throws IOException {
        List<String> list = new ArrayList<String>();
        int r = in.readInt();
        while (r > 0) {
            list.add(this.myStringEnumerator.read(in));
            r--;
        }

        return list.toArray(new String[list.size()]);
    }

}
