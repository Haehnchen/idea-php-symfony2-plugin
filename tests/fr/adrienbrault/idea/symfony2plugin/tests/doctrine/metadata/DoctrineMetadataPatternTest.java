package fr.adrienbrault.idea.symfony2plugin.tests.doctrine.metadata;

import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern
 */
public class DoctrineMetadataPatternTest extends Assert {

    @Test
    public void testXmlTagPattern() {
        assertTrue("doctrine-mapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertTrue("doctrine-foo-mapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertTrue("doctrine-mongodb-mapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertFalse("doctrine2-foo-mapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertFalse("doctrinemapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertFalse("doctrine2mapping".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
        assertFalse("doctrine-".matches(DoctrineMetadataPattern.DOCTRINE_MAPPING));
    }

}
