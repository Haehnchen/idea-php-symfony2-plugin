package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes.externalizer;

import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateInclude;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.TemplateIncludeExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;
import org.junit.Test;

import java.io.*;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TemplateIncludeExternalizerTest {
    @Test
    public void roundTripEveryTypeAndAllTypes() throws IOException {
        TemplateInclude include = new TemplateInclude("shared.html.twig", Set.of(TYPE.INCLUDE));
        assertEquals(include, read(save(include)));

        TemplateInclude includeFunction = new TemplateInclude("shared.html.twig", Set.of(TYPE.INCLUDE_FUNCTION));
        assertEquals(includeFunction, read(save(includeFunction)));

        TemplateInclude embed = new TemplateInclude("shared.html.twig", Set.of(TYPE.EMBED));
        assertEquals(embed, read(save(embed)));

        TemplateInclude importTag = new TemplateInclude("shared.html.twig", Set.of(TYPE.IMPORT));
        assertEquals(importTag, read(save(importTag)));

        TemplateInclude from = new TemplateInclude("shared.html.twig", Set.of(TYPE.FROM));
        assertEquals(from, read(save(from)));

        TemplateInclude formTheme = new TemplateInclude("shared.html.twig", Set.of(TYPE.FORM_THEME));
        assertEquals(formTheme, read(save(formTheme)));

        TemplateInclude blockFunction = new TemplateInclude("shared.html.twig", Set.of(TYPE.BLOCK_FUNCTION));
        assertEquals(blockFunction, read(save(blockFunction)));

        TemplateInclude sourceFunction = new TemplateInclude("shared.html.twig", Set.of(TYPE.SOURCE_FUNCTION));
        assertEquals(sourceFunction, read(save(sourceFunction)));

        TemplateInclude value = new TemplateInclude("@Foo/shared.html.twig", EnumSet.allOf(TYPE.class));
        assertEquals(value, read(save(value)));
    }

    @Test
    public void serializationIsOrderIndependent() throws IOException {
        TemplateInclude value = new TemplateInclude("shared.html.twig", new LinkedHashSet<>(List.of(TYPE.INCLUDE, TYPE.EMBED)));
        TemplateInclude reversed = new TemplateInclude("shared.html.twig", new LinkedHashSet<>(List.of(TYPE.EMBED, TYPE.INCLUDE)));

        assertArrayEquals(save(value), save(reversed));
        assertArrayEquals(encoded(2, 0, 2), save(value));
    }

    @Test
    public void persistentIdsAreStable() throws IOException {
        assertEquals(Set.of(TYPE.INCLUDE), read(encoded(1, 0)).getTypes());
        assertEquals(Set.of(TYPE.INCLUDE_FUNCTION), read(encoded(1, 1)).getTypes());
        assertEquals(Set.of(TYPE.EMBED), read(encoded(1, 2)).getTypes());
        assertEquals(Set.of(TYPE.IMPORT), read(encoded(1, 3)).getTypes());
        assertEquals(Set.of(TYPE.FROM), read(encoded(1, 4)).getTypes());
        assertEquals(Set.of(TYPE.FORM_THEME), read(encoded(1, 5)).getTypes());
        assertEquals(Set.of(TYPE.BLOCK_FUNCTION), read(encoded(1, 6)).getTypes());
        assertEquals(Set.of(TYPE.SOURCE_FUNCTION), read(encoded(1, 7)).getTypes());
    }

    @Test
    public void corruptDataIsRejected() throws IOException {
        assertThrows(IOException.class, () -> read(encoded(0)));
        assertThrows(IOException.class, () -> read(encoded(9)));
        assertThrows(IOException.class, () -> read(encoded(255)));
        assertThrows(IOException.class, () -> read(encoded(1, 8)));
        assertThrows(IOException.class, () -> read(encoded(1, 255)));
        assertThrows(IOException.class, () -> read(encoded(2, 0, 0)));
        assertThrows(IOException.class, () -> read(encoded(2, 0)));
        assertThrows(IOException.class, () -> read(new byte[0]));
    }

    private byte[] encoded(int count, int... ids) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeUTF("shared.html.twig");
        out.writeByte(count);
        for (int id : ids) {
            out.writeByte(id);
        }
        return bytes.toByteArray();
    }

    private byte[] save(TemplateInclude value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TemplateIncludeExternalizer.INSTANCE.save(new DataOutputStream(bytes), value);
        return bytes.toByteArray();
    }

    private TemplateInclude read(byte[] bytes) throws IOException {
        return TemplateIncludeExternalizer.INSTANCE.read(new DataInputStream(new ByteArrayInputStream(bytes)));
    }
}
