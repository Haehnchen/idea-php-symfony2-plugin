package de.espend.idea.php.drupal.tests.index;

import de.espend.idea.php.drupal.index.MenuIndex;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see MenuIndex
 */
public class MenuIndexTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("search.menu.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/index/fixtures";
    }

    public void testThatMappingValueIsInIndex() {
        assertIndexContains(MenuIndex.KEY, "search.view");
        assertIndexContains(MenuIndex.KEY, "entity.search_page.collection");
    }
}
