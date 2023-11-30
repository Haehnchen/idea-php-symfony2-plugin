package fr.adrienbrault.idea.symfony2plugin.tests.assetMapper;

import fr.adrienbrault.idea.symfony2plugin.assetMapper.AssetMapperUtil;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.AssetMapperModule;
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.MappingFileEnum;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetMapperUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("importmap.php");
        myFixture.copyFileToProject("installed.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/assetMapper/fixtures";
    }

    public void testGetMappingFiles() {
        List<AssetMapperModule> mappingFiles = AssetMapperUtil.getMappingFiles(getProject());

        AssetMapperModule app = mappingFiles.stream().filter(m -> m.sourceType() == MappingFileEnum.IMPORTMAP && m.key().equals("app")).findFirst().get();
        assertTrue(app.entrypoint());
        assertEquals("./assets/app.js", app.path());
        assertNull(app.version());

        AssetMapperModule css = mappingFiles.stream().filter(m -> m.sourceType() == MappingFileEnum.IMPORTMAP && m.key().equals("bootstrap/dist/css/bootstrap.min.css")).findFirst().get();
        assertNull(css.entrypoint());
        assertNull(css.path());
        assertEquals("5.3.2", css.version());
        assertEquals("css", css.type());

        AssetMapperModule lodash = mappingFiles.stream().filter(m -> m.sourceType() == MappingFileEnum.INSTALLED && m.key().equals("lodash")).findFirst().get();
        assertNull(lodash.entrypoint());
        assertNull(lodash.path());
        assertEquals("4.17.21", lodash.version());
    }
}
