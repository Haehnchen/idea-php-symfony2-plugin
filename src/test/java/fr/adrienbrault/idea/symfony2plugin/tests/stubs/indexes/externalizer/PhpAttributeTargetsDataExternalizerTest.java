package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes.externalizer;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.AttributeTarget;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.TargetScope;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.PhpAttributeTargetsDataExternalizer;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PhpAttributeTargetsDataExternalizerTest {
    @Test
    public void testRoundTripPreservesAllScopesAndData() throws IOException {
        List<AttributeTarget> targets = List.of(
            new AttributeTarget(TargetScope.PHP_CLASS, "App\\AccountData", null, List.of()),
            new AttributeTarget(TargetScope.PROPERTY, "App\\AccountData", "internalName", List.of()),
            new AttributeTarget(TargetScope.METHOD, "App\\Extension", "filter", List.of("filter_name")),
            new AttributeTarget(TargetScope.PHP_CLASS, "App\\Command", null, List.of())
        );
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PhpAttributeTargetsDataExternalizer.INSTANCE.save(new DataOutputStream(bytes), targets);
        assertEquals(targets, PhpAttributeTargetsDataExternalizer.INSTANCE.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));
    }
}
