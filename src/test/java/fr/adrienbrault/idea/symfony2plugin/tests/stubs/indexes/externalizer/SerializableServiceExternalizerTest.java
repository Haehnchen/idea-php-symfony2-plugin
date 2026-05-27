package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes.externalizer;

import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.SerializableServiceExternalizer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class SerializableServiceExternalizerTest extends Assert {

    @Test
    public void testSaveSkipsNullStringCollectionValues() throws IOException {
        SerializableService service = new SerializableService("app.service")
            .setResource(Arrays.asList("../src/*", null))
            .setExclude(Collections.singletonList(null))
            .setTags(Arrays.asList("console.command", null));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SerializableServiceExternalizer.INSTANCE.save(new DataOutputStream(bytes), service);

        ServiceSerializable deserialized = SerializableServiceExternalizer.INSTANCE.read(
            new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))
        );

        assertEquals("app.service", deserialized.getId());
        assertEquals(1, deserialized.getResource().size());
        assertTrue(deserialized.getResource().contains("../src/*"));
        assertFalse(deserialized.getResource().contains(null));
        assertTrue(deserialized.getExclude().isEmpty());
        assertEquals(1, deserialized.getTags().size());
        assertTrue(deserialized.getTags().contains("console.command"));
        assertFalse(deserialized.getTags().contains(null));
    }
}
