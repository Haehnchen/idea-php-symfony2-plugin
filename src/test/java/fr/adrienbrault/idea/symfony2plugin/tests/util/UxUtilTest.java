package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import kotlin.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("UxUtil.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    public void testVisitAsTwigComponent() {
        PhpFile phpFile = (PhpFile) PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace App\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n" +
            "\n" +
            "#[AsTwigComponent('Alert2Foobar')]\n" +
            "class Alert2 {}\n" +
            "\n" +
            "#[AsTwigComponent(name: 'Alert3Foobar')]\n" +
            "class Alert3 {}\n"
        );

        Map<String, PhpClass> components = new HashMap<>();
        UxUtil.visitAsTwigComponent(phpFile, pair -> components.put(pair.getFirst(), pair.getSecond()));

        assertEquals("\\App\\Components\\Alert", components.get("Alert").getFQN());
        assertEquals("\\App\\Components\\Alert2", components.get("Alert2Foobar").getFQN());
        assertEquals("\\App\\Components\\Alert3", components.get("Alert3Foobar").getFQN());
    }

    public void testGetTwigComponentNames() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        assertContainsElements(UxUtil.getTwigComponentNames(getProject()), "Alert");
    }

    public void testGetTwigComponentNameTarget() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Components;\n" +
            "\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        assertEquals(
            "\\App\\Components\\Alert",
            UxUtil.getTwigComponentNameTargets(getProject(), "Alert").iterator().next().getFQN()
        );
    }

    public void testVisitComponentVariables() {
        Collection<PhpClass> anyByFQN = PhpIndex.getInstance(getProject()).getAnyByFQN("\\App\\Alert");

        Map<String, PhpNamedElement> map = new HashMap<>();
        UxUtil.visitComponentVariables(anyByFQN.iterator().next(), pair -> map.put(pair.getFirst(), pair.getSecond()));

        assertTrue(map.get("message") instanceof Field);
        assertTrue(map.get("ico") instanceof Field);
        assertTrue(map.get("dismissable") instanceof Method);
        assertTrue(map.get("actions") instanceof Method);
        assertTrue(map.get("alert_type") instanceof Field);assertTrue(map.get("alert_type") instanceof Field);

        assertFalse(map.containsKey("notPublicField"));
        assertFalse(map.containsKey("notPrivateMethod"));
        assertFalse(map.containsKey("notExposedPublicMethod"));
    }
}
