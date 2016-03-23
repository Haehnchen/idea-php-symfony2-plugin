package fr.adrienbrault.idea.symfony2plugin.tests.translation.dict;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
 */
public class TranslationUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("apple.de.yml", "Resources/translations/apple.de.yml");
        myFixture.copyFileToProject("car.de.yml", "Resources/translations/car.de.yml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGetDomainFilePsiElements() {
        assertTrue(TranslationUtil.getDomainPsiFiles(getProject(), "apple").size() > 0);
        assertTrue(TranslationUtil.getDomainPsiFiles(getProject(), "car").size() > 0);
    }

    public void testGetTranslationPsiElements() {
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.great", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.greater than", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "yaml_weak.symfony.greater than equals", "apple").length > 0);
        assertTrue(TranslationUtil.getTranslationPsiElements(getProject(), "foo_yaml.symfony.great", "car").length > 0);
    }
}
