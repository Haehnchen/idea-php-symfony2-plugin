package fr.adrienbrault.idea.symfony2plugin.tests.ux.variable.collector

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.ux.variable.collector.UxComponentVariableCollector

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see UxComponentVariableCollector
 */
class UxComponentVariableCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json")
    }

    override fun getTestDataPath(): String =
        "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/ux/variable/collector/fixtures"

    fun testPublicFieldIsAvailableAsVariable() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/Alert.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "message")
    }

    fun testPublicIntFieldIsAvailableAsVariable() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/Alert.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "count")
    }

    fun testPrivateFieldIsNotAvailableAsVariable() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/Alert.html.twig")
        myFixture.completeBasic()

        assertDoesntContain(myFixture.lookupElementStrings ?: emptyList(), "secret")
    }

    fun testThisVariableIsAvailable() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/Alert.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "this")
    }

    fun testAttributesVariableIsAvailable() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/Alert.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "attributes")
    }

    fun testNoVariablesForNonComponentTemplate() {
        myFixture.addFileToProject("templates/other/SomeTemplate.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/other/SomeTemplate.html.twig")
        myFixture.completeBasic()

        assertDoesntContain(myFixture.lookupElementStrings ?: emptyList(), "message", "this", "attributes")
    }
}
