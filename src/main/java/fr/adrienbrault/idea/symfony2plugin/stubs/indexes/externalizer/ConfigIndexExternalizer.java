package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.ConfigIndex;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigIndexExternalizer implements DataExternalizer<ConfigIndex> {

    public static final ConfigIndexExternalizer INSTANCE = new ConfigIndexExternalizer();

    @Override
    public void save(@NotNull DataOutput out, ConfigIndex value) throws IOException {
        out.writeUTF(value.getName());
        TreeMap<String, TreeMap<String, String>> configs = (TreeMap<String, TreeMap<String, String>>) value.getConfigs();
        out.writeInt(configs.size());
        for (Map.Entry<String, TreeMap<String, String>> entry : configs.entrySet()) {
            out.writeUTF(entry.getKey());
            TreeMap<String, String> inner = entry.getValue();
            out.writeInt(inner.size());
            for (Map.Entry<String, String> innerEntry : inner.entrySet()) {
                out.writeUTF(innerEntry.getKey());
                out.writeUTF(innerEntry.getValue());
            }
        }
        Set<String> values = value.getValues();
        out.writeInt(values.size());
        for (String v : values) {
            out.writeUTF(v);
        }
    }

    @Override
    public ConfigIndex read(@NotNull DataInput in) throws IOException {
        String name = in.readUTF();
        int configsSize = in.readInt();
        TreeMap<String, TreeMap<String, String>> configs = new TreeMap<>();
        for (int i = 0; i < configsSize; i++) {
            String key = in.readUTF();
            int innerSize = in.readInt();
            TreeMap<String, String> inner = new TreeMap<>();
            for (int j = 0; j < innerSize; j++) {
                inner.put(in.readUTF(), in.readUTF());
            }
            configs.put(key, inner);
        }
        int valuesSize = in.readInt();
        Set<String> values = new HashSet<>(valuesSize);
        for (int i = 0; i < valuesSize; i++) {
            values.add(in.readUTF());
        }
        return new ConfigIndex(name, configs, values);
    }
}
