package fr.adrienbrault.idea.symfony2plugin.tests.assetMapper

import fr.adrienbrault.idea.symfony2plugin.assetMapper.AssetMapperUtil
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.MappingFileEnum
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class AssetMapperUtilTest : SymfonyLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("importmap.php")
        myFixture.copyFileToProject("installed.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/assetMapper/fixtures"
    }

    fun testGetMappingFiles() {
        val mappingFiles = AssetMapperUtil.getMappingFiles(project)

        val app = mappingFiles.first { it.sourceType == MappingFileEnum.IMPORTMAP && it.key == "app" }
        assertTrue(app.entrypoint!!)
        assertEquals("./assets/app.js", app.path)
        assertNull(app.version)

        val css = mappingFiles.first { it.sourceType == MappingFileEnum.IMPORTMAP && it.key == "bootstrap/dist/css/bootstrap.min.css" }
        assertNull(css.entrypoint)
        assertNull(css.path)
        assertEquals("5.3.2", css.version)
        assertEquals("css", css.type)

        val lodash = mappingFiles.first { it.sourceType == MappingFileEnum.INSTALLED && it.key == "lodash" }
        assertNull(lodash.entrypoint)
        assertNull(lodash.path)
        assertEquals("4.17.21", lodash.version)
    }
}
