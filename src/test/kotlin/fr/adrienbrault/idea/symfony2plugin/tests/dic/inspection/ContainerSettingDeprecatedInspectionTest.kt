package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.ContainerSettingDeprecatedInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.ContainerSettingDeprecatedInspection
 */
class ContainerSettingDeprecatedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testThatOldXmlFactoryPatternShouldProvideDeprecatedHighlight() {
        assertLocalInspectionContains(ContainerSettingDeprecatedInspection.ContainerSettingDeprecatedInspectionXml::class.java, "service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service fac<caret>tory-class=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: this factory pattern is deprecated use 'factory' instead"
        )

        assertLocalInspectionNotContains(ContainerSettingDeprecatedInspection.ContainerSettingDeprecatedInspectionXml::class.java, "service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service factory-class=\"Date<caret>Time\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: this factory pattern is deprecated use 'factory' instead"
        )

        assertLocalInspectionContains(ContainerSettingDeprecatedInspection.ContainerSettingDeprecatedInspectionXml::class.java, "service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service fact<caret>ory-method=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: this factory pattern is deprecated use 'factory' instead"
        )

        assertLocalInspectionContains(ContainerSettingDeprecatedInspection.ContainerSettingDeprecatedInspectionXml::class.java, "service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service fact<caret>ory-service=\"DateTime\"/>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: this factory pattern is deprecated use 'factory' instead"
        )
    }

    fun testThatOldYmlFactoryPatternShouldProvideDeprecatedHighlight() {

        val strings = arrayOf(
            "factory<caret>_class: foo",
            "factory<caret>_method: foo",
            "factory<caret>_service: foo"
        )

        for (s in strings) {
            assertLocalInspectionContains(ContainerSettingDeprecatedInspection.ContainerSettingDeprecatedInspectionYaml::class.java, "services.yml", "" +
                "services:\n" +
                "   foo:\n" +
                "       " + s + "\n",
                "Symfony: this factory pattern is deprecated use 'factory' instead"
            )
        }

    }
}
