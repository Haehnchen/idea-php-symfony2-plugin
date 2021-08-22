package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php;

import fr.adrienbrault.idea.symfony2plugin.intentions.php.XmlServiceArgumentIntention;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class XmlServiceArgumentIntentionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testIntentionStartInReadThread() {
        var intention = new XmlServiceArgumentIntention();

        // Prevents "AWT events are not allowed inside write action" exception
        // while creating dialog window for resolving arguments ambiguity
        assertFalse(intention.startInWriteAction());
    }
}
