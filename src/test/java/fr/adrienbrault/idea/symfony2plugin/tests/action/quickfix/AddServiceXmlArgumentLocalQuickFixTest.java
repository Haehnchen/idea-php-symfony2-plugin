package fr.adrienbrault.idea.symfony2plugin.tests.action.quickfix;

import fr.adrienbrault.idea.symfony2plugin.action.quickfix.AddServiceXmlArgumentLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;

public class AddServiceXmlArgumentLocalQuickFixTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testStartInWriteAction() {
        var quickfix = new AddServiceXmlArgumentLocalQuickFix(new ArrayList<>());

        // Prevents "AWT events are not allowed inside write action" exception
        // while creating dialog window for resolving arguments ambiguity
        assertFalse(quickfix.startInWriteAction());
    }
}
