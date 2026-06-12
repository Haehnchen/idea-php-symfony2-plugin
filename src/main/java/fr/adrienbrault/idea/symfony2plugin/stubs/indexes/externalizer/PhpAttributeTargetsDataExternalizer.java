package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Externalizer for typed PHP attribute index targets.
 */
public class PhpAttributeTargetsDataExternalizer implements DataExternalizer<List<PhpAttributeIndex.AttributeTarget>> {

    public static final PhpAttributeTargetsDataExternalizer INSTANCE = new PhpAttributeTargetsDataExternalizer();

    @Override
    public synchronized void save(@NotNull DataOutput out, List<PhpAttributeIndex.AttributeTarget> value) throws IOException {
        out.writeInt(value.size());

        for (PhpAttributeIndex.AttributeTarget target : value) {
            EnumeratorStringDescriptor.INSTANCE.save(out, target.scope().name());
            EnumeratorStringDescriptor.INSTANCE.save(out, target.classFqn());
            out.writeBoolean(target.memberName() != null);
            if (target.memberName() != null) {
                EnumeratorStringDescriptor.INSTANCE.save(out, target.memberName());
            }

            out.writeInt(target.data().size());
            for (String data : target.data()) {
                EnumeratorStringDescriptor.INSTANCE.save(out, data);
            }
        }
    }

    @Override
    public synchronized List<PhpAttributeIndex.AttributeTarget> read(@NotNull DataInput in) throws IOException {
        List<PhpAttributeIndex.AttributeTarget> targets = new ArrayList<>();

        for (int i = in.readInt(); i > 0; --i) {
            PhpAttributeIndex.TargetScope scope = PhpAttributeIndex.TargetScope.valueOf(EnumeratorStringDescriptor.INSTANCE.read(in));
            String classFqn = EnumeratorStringDescriptor.INSTANCE.read(in);
            String memberName = in.readBoolean() ? EnumeratorStringDescriptor.INSTANCE.read(in) : null;

            List<String> data = new ArrayList<>();
            for (int r = in.readInt(); r > 0; --r) {
                data.add(EnumeratorStringDescriptor.INSTANCE.read(in));
            }

            targets.add(new PhpAttributeIndex.AttributeTarget(scope, classFqn, memberName, data));
        }

        return targets;
    }
}
