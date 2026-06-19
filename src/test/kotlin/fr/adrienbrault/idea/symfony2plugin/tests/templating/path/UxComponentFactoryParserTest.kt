package fr.adrienbrault.idea.symfony2plugin.tests.templating.path

import fr.adrienbrault.idea.symfony2plugin.templating.path.UxComponentFactoryParser
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import java.io.ByteArrayInputStream

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see UxComponentFactoryParser
 */
class UxComponentFactoryParserTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testComponentsAndClassMapAreExtracted() {
        val parser = parse(xml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="Shop:Card" type="collection">
                        <argument key="key">Shop:Card</argument>
                        <argument key="class">App\Twig\Components\ShopCard</argument>
                        <argument key="template">components/shop/Card.html.twig</argument>
                    </argument>
                    <argument key="AlertBanner" type="collection">
                        <argument key="key">AlertBanner</argument>
                        <argument key="class">App\Twig\Components\AlertBanner</argument>
                        <argument key="template">twig-components/AlertBanner.html.twig</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\Twig\Components\ShopCard">Shop:Card</argument>
                    <argument key="App\Twig\Components\AlertBanner">AlertBanner</argument>
                </argument>
                <argument type="service" id="twig"/>
            </service>
        """))

        val shopCard = parser.components["Shop:Card"]
        assertNotNull(shopCard)
        assertEquals("\\App\\Twig\\Components\\ShopCard", shopCard!!.phpClass())
        assertEquals("components/shop/Card.html.twig", shopCard.template())

        val alertBanner = parser.components["AlertBanner"]
        assertNotNull(alertBanner)
        assertEquals("\\App\\Twig\\Components\\AlertBanner", alertBanner!!.phpClass())
        assertEquals("twig-components/AlertBanner.html.twig", alertBanner.template())

        assertEquals("Shop:Card", parser.classMap["\\App\\Twig\\Components\\ShopCard"])
        assertEquals("AlertBanner", parser.classMap["\\App\\Twig\\Components\\AlertBanner"])
    }

    fun testTemplateFromMethodIsMarkedWithoutGuessingTemplate() {
        val parser = parse(xml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="DynamicCard" type="collection">
                        <argument key="class">App\Twig\Components\DynamicCard</argument>
                        <argument key="template_from_method">getTemplate</argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\Twig\Components\DynamicCard">DynamicCard</argument>
                </argument>
            </service>
        """))

        val dynamicCard = parser.components["DynamicCard"]
        assertNotNull(dynamicCard)
        assertNull(dynamicCard!!.template())
        assertEquals("getTemplate", dynamicCard.templateFromMethod())
    }

    fun testNestedArgumentsDoNotAffectTopLevelArgumentPositions() {
        val parser = parse(xml("""
            <service id="ux.twig_component.component_factory">
                <argument type="service" id="ux.twig_component.component_template_finder"/>
                <argument type="service" id=".service_locator.demo"/>
                <argument type="service" id="property_accessor"/>
                <argument type="service" id="event_dispatcher"/>
                <argument type="collection">
                    <argument key="WithMount" type="collection">
                        <argument key="class">App\Twig\Components\WithMount</argument>
                        <argument key="template">components/WithMount.html.twig</argument>
                        <argument key="mount" type="collection">
                            <argument>mount</argument>
                        </argument>
                    </argument>
                </argument>
                <argument type="collection">
                    <argument key="App\Twig\Components\WithMount">WithMount</argument>
                </argument>
            </service>
        """))

        assertEquals("components/WithMount.html.twig", parser.components["WithMount"]!!.template())
        assertEquals("WithMount", parser.classMap["\\App\\Twig\\Components\\WithMount"])
    }

    private fun parse(xml: String): UxComponentFactoryParser {
        val parser = UxComponentFactoryParser()
        val containerFile = myFixture.addFileToProject("var/cache/dev/container.xml", xml).virtualFile
        parser.parser(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), containerFile, project)
        return parser
    }

    companion object {
        fun xml(services: String): String =
            """<?xml version="1.0" encoding="utf-8"?><container><services>$services</services></container>"""
    }
}
