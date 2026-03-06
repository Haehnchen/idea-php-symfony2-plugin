package fr.adrienbrault.idea.symfony2plugin.tests.templating.inlay;

import fr.adrienbrault.idea.symfony2plugin.templating.inlay.TwigVariableInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see TwigVariableInsertHandler
 */
public class TwigVariableInsertHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    // --- insertPrintBlock ---

    public void testInsertPrintBlockInsertsPrintExpression() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertPrintBlock("product"));
        assertContent("\n{{ product }}");
    }

    public void testInsertPrintBlockWithPropertyPath() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertPrintBlock("user.email"));
        assertContent("\n{{ user.email }}");
    }

    public void testInsertPrintBlockAppendsAfterExistingContent() {
        myFixture.configureByText("test.html.twig", "{{ title }}<caret>");
        invoke(h -> h.insertPrintBlock("name"));
        assertContent("{{ title }}\n{{ name }}");
    }

    // --- insertIfStatement ---

    public void testInsertIfStatementForBoolVariable() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertIfStatement("isActive"));
        assertContent("\n{% if isActive %}\n    \n{% endif %}");
    }

    // --- insertEmptyForeach ---

    public void testInsertEmptyForeachPluralS() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("products"));
        assertContent("\n{% for product in products %}\n    \n{% endfor %}");
    }

    public void testInsertEmptyForeachPluralIes() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("categories"));
        assertContent("\n{% for category in categories %}\n    \n{% endfor %}");
    }

    public void testInsertEmptyForeachPluralEs() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("churches"));
        assertContent("\n{% for church in churches %}\n    \n{% endfor %}");
    }

    public void testInsertEmptyForeachNoPlural() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertEmptyForeach("data"));
        assertContent("\n{% for dataItem in data %}\n    \n{% endfor %}");
    }

    // --- insertFilledForeach ---

    public void testInsertFilledForeachWithArrayProperty() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("user", "tags"));
        assertContent("\n{% for tag in user.tags %}\n    {{ tag }}\n{% endfor %}");
    }

    public void testInsertFilledForeachSingularizeEndsWithEs() {
        // "roles" singularizes to "role"
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("user", "roles"));
        assertContent("\n{% for role in user.roles %}\n    {{ role }}\n{% endfor %}");
    }

    public void testInsertFilledForeachWithIesProperty() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertFilledForeach("shop", "categories"));
        assertContent("\n{% for category in shop.categories %}\n    {{ category }}\n{% endfor %}");
    }

    // --- insertForeachPropertyAccess ---

    public void testInsertForeachPropertyAccessUsesParentCollection() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertForeachPropertyAccess("dates", "foo"));
        assertContent("\n{% for date in dates %}\n    {{ date.foo }}\n{% endfor %}");
    }

    public void testInsertForeachPropertyAccessWithNonPluralParent() {
        myFixture.configureByText("test.html.twig", "<caret>");
        invoke(h -> h.insertForeachPropertyAccess("data", "name"));
        assertContent("\n{% for dataItem in data %}\n    {{ dataItem.name }}\n{% endfor %}");
    }

    // --- helpers ---

    private void invoke(java.util.function.Consumer<TwigVariableInsertHandler> action) {
        action.accept(new TwigVariableInsertHandler(myFixture.getEditor(), myFixture.getFile()));
    }

    private void assertContent(String expected) {
        assertEquals(expected, myFixture.getEditor().getDocument().getText());
    }
}
