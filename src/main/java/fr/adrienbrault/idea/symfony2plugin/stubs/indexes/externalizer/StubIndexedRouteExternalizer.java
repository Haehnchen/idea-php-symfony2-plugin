package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer;

import com.intellij.util.io.DataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class StubIndexedRouteExternalizer implements DataExternalizer<StubIndexedRoute> {

    public static final StubIndexedRouteExternalizer INSTANCE = new StubIndexedRouteExternalizer();

    @Override
    public void save(@NotNull DataOutput out, StubIndexedRoute value) throws IOException {
        out.writeUTF(value.getName());
        writeNullableString(out, value.getController());
        writeNullableString(out, value.getPath());
        Collection<String> methods = value.getMethods();
        out.writeInt(methods.size());
        for (String method : methods) {
            out.writeUTF(method);
        }
    }

    @Override
    public StubIndexedRoute read(@NotNull DataInput in) throws IOException {
        StubIndexedRoute route = new StubIndexedRoute(in.readUTF());
        route.setController(readNullableString(in));
        route.setPath(readNullableString(in));
        int size = in.readInt();
        Collection<String> methods = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            methods.add(in.readUTF());
        }
        route.setMethods(methods);
        return route;
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
