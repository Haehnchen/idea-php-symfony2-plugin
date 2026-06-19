package fr.adrienbrault.idea.symfony2plugin.tests.ux.variable.collector

import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.util.getSymfonyVarDirectoryWatcher
import fr.adrienbrault.idea.symfony2plugin.ux.variable.collector.UxComponentVariableCollector

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see UxComponentVariableCollector
 */
class UxComponentVariableCollectorTest : SymfonyLightCodeInsightFixtureTestCase() {
    private var previousContainerFiles: List<ContainerFile> = emptyList()

    override fun setUp() {
        super.setUp()

        val settings = Settings.getInstance(project)
        previousContainerFiles = settings.containerFiles?.toList() ?: emptyList()
        settings.containerFiles = arrayListOf()

        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json")
    }

    @Throws(Exception::class)
    override fun tearDown() {
        try {
            Settings.getInstance(project).containerFiles = ArrayList(previousContainerFiles)
            getSymfonyVarDirectoryWatcher(project).reloadConfiguration()
        } finally {
            super.tearDown()
        }
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

    fun testCompiledContainerTemplateExposesClassVariables() {
        configureContainerXml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="CompiledAlert" type="collection">
                        <argument key="class">App\Twig\Components\CompiledAlert</argument>
                        <argument key="template">components/CompiledAlert.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\Twig\Components\CompiledAlert">CompiledAlert</argument>
                </argument>
            </service>
        """.trimIndent())

        myFixture.addFileToProject("src/Twig/Components/CompiledAlert.php", "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "\n" +
            "class CompiledAlert\n" +
            "{\n" +
            "    public string \$title = '';\n" +
            "    private string \$secret = 'hidden';\n" +
            "}\n"
        )
        myFixture.addFileToProject("templates/components/CompiledAlert.html.twig", "{{ <caret> }}")
        myFixture.configureFromTempProjectFile("templates/components/CompiledAlert.html.twig")
        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings ?: emptyList(), "title", "this", "attributes")
        assertDoesntContain(myFixture.lookupElementStrings ?: emptyList(), "secret")
    }

    private fun configureContainerXml(services: String) {
        val path = "var/cache/dev/${getTestName(false)}Container.xml"
        createFileInProjectRoot(path, """<?xml version="1.0" encoding="utf-8"?><container><services>$services</services></container>""")
        Settings.getInstance(project).containerFiles = arrayListOf(ContainerFile(path))
        getSymfonyVarDirectoryWatcher(project).reloadConfiguration()
    }
}
