package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex
 */
public class StimulusControllerStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatStimulusControllerIsIndexed() {
        myFixture.addFileToProject("assets/controllers/hello_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {\n" +
            "        this.element.textContent = 'Hello Stimulus!';\n" +
            "    }\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertFalse("Index should contain 'hello', but got: " + allKeys, allKeys.isEmpty());
        assertContainsElements(allKeys, "hello");
    }

    public void testThatStimulusControllerWithUnderscoreIsIndexed() {
        myFixture.addFileToProject("assets/controllers/my_component_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'my-component', but got: " + allKeys, allKeys.contains("my-component"));
    }

    public void testThatStimulusControllerWithDashNamingIsIndexed() {
        myFixture.addFileToProject("assets/controllers/search-form-controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    search() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'search-form', but got: " + allKeys, allKeys.contains("search-form"));
    }

    public void testThatNonStimulusControllerIsNotIndexed() {
        myFixture.addFileToProject("assets/controllers/other_controller.js",
            "import { SomeOtherClass } from 'some-library';\n" +
            "\n" +
            "export default class extends SomeOtherClass {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        assertIndexNotContains(StimulusControllerStubIndex.KEY, "other");
    }

    public void testThatFileWithoutControllerSuffixIsNotIndexed() {
        myFixture.addFileToProject("assets/controllers/hello.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        assertIndexNotContains(StimulusControllerStubIndex.KEY, "hello");
    }

    public void testThatStimulusImportVariantIsIndexed() {
        myFixture.addFileToProject("assets/controllers/legacy_controller.js",
            "import { Controller } from 'stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'legacy', but got: " + allKeys, allKeys.contains("legacy"));
    }

    public void testThatSubfolderControllerIsIndexedWithNamespace() {
        myFixture.addFileToProject("assets/controllers/users/list_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'users--list', but got: " + allKeys, allKeys.contains("users--list"));
    }

    public void testThatNestedSubfolderControllerIsIndexedWithNamespace() {
        myFixture.addFileToProject("assets/controllers/admin/settings/form_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'admin--settings--form', but got: " + allKeys, allKeys.contains("admin--settings--form"));
    }

    public void testThatSubfolderWithUnderscoreIsConvertedToDash() {
        myFixture.addFileToProject("assets/controllers/user_admin/profile_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'user-admin--profile', but got: " + allKeys, allKeys.contains("user-admin--profile"));
    }

    public void testThatTypeScriptControllerIsIndexed() {
        myFixture.addFileToProject("assets/controllers/hello_controller.ts",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "interface HelloControllerTargets {\n" +
            "    output: HTMLElement;\n" +
            "    input: HTMLInputElement;\n" +
            "}\n" +
            "\n" +
            "interface HelloControllerValues {\n" +
            "    name: string;\n" +
            "    count: number;\n" +
            "}\n" +
            "\n" +
            "export default class extends Controller<HTMLElement> {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'hello', but got: " + allKeys, allKeys.contains("hello"));
    }

    public void testThatTypeScriptControllerWithNamespaceIsIndexed() {
        myFixture.addFileToProject("assets/controllers/users/list_controller.ts",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "interface ListControllerTargets {\n" +
            "    list: HTMLElement;\n" +
            "}\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'users--list', but got: " + allKeys, allKeys.contains("users--list"));
    }

    public void testThatControllersJsonIsIndexed() {
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true,\n" +
            "        \"fetch\": \"eager\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'symfony--ux-chartjs--chart', but got: " + allKeys, allKeys.contains("symfony--ux-chartjs--chart"));
    }

    public void testThatControllersJsonWithMultipleControllersIsIndexed() {
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true,\n" +
            "        \"fetch\": \"eager\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"@symfony/ux-dropzone\": {\n" +
            "      \"dropzone\": {\n" +
            "        \"enabled\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'symfony--ux-chartjs--chart', but got: " + allKeys, allKeys.contains("symfony--ux-chartjs--chart"));
        assertTrue("Index should contain 'symfony--ux-dropzone--dropzone', but got: " + allKeys, allKeys.contains("symfony--ux-dropzone--dropzone"));
    }

    public void testThatControllersJsonWithDisabledControllerIsNotIndexed() {
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": false,\n" +
            "        \"fetch\": \"eager\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        assertIndexNotContains(StimulusControllerStubIndex.KEY, "symfony--ux-chartjs--chart");
    }

    public void testThatControllersJsonWithNoEnabledFieldIsIndexed() {
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"fetch\": \"eager\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'symfony--ux-chartjs--chart', but got: " + allKeys, allKeys.contains("symfony--ux-chartjs--chart"));
    }

    public void testThatControllersJsonAndJsFilesAreBothIndexed() {
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        myFixture.addFileToProject("assets/controllers/hello_controller.js",
            "import { Controller } from '@hotwired/stimulus';\n" +
            "\n" +
            "export default class extends Controller {\n" +
            "    connect() {}\n" +
            "}\n"
        );

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, getProject());
        assertTrue("Index should contain 'symfony--ux-chartjs--chart', but got: " + allKeys, allKeys.contains("symfony--ux-chartjs--chart"));
        assertTrue("Index should contain 'hello', but got: " + allKeys, allKeys.contains("hello"));
    }
}
