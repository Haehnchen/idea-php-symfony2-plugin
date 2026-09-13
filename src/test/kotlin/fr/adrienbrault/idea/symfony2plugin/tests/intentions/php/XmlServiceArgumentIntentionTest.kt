package fr.adrienbrault.idea.symfony2plugin.tests.intentions.php

import fr.adrienbrault.idea.symfony2plugin.intentions.php.XmlServiceArgumentIntention
import junit.framework.TestCase

class XmlServiceArgumentIntentionTest : TestCase() {
    fun testIntentionStartInReadThread() {
        val intention = XmlServiceArgumentIntention()

        // Prevents "AWT events are not allowed inside write action" exception
        // while creating dialog window for resolving arguments ambiguity
        assertFalse(intention.startInWriteAction())
    }
}
