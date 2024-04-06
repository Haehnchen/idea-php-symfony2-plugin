package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.ServicePropertyInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ServicePropertyInsertUtil
 */
public class ServicePropertyInsertUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ServicePropertyInsertUtil.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    public void testAppendPropertyInjection() {
        PhpClass fromText = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "class Foobar\n" +
            "{\n" +
            "    public function __construct(private readonly \\DateTime $d)\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        ServicePropertyInsertUtil.appendPropertyInjection(fromText, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text = fromText.getText();
        assertTrue(text.contains("public function __construct(private readonly \\DateTime $d,private readonly UrlGeneratorInterface $router)"));

        PhpClass fromText2 = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "class Foobar\n" +
            "{\n" +
            "}"
        );

        ServicePropertyInsertUtil.appendPropertyInjection(fromText2, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text2 = fromText2.getText();
        assertTrue(text2.contains("public function __construct(UrlGeneratorInterface $router)"));


        PhpClass fromText3 = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "readonly class Foobar\n" +
            "{\n" +
            "    public function __construct(private readonly \\DateTime $d)\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        ServicePropertyInsertUtil.appendPropertyInjection(fromText3, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text3 = fromText3.getText();
        assertTrue(text3.contains("public function __construct(private readonly \\DateTime $d,private UrlGeneratorInterface $router)"));
    }

    public void testInjectionService() {
        List<String> classes1 = ServicePropertyInsertUtil.getInjectionService(getProject(), "router");
        assertContainsElements(classes1, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes2 = ServicePropertyInsertUtil.getInjectionService(getProject(), "urlgenerator");
        assertContainsElements(classes2, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes3 = ServicePropertyInsertUtil.getInjectionService(getProject(), "urlGenerator");
        assertContainsElements(classes3, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes4 = ServicePropertyInsertUtil.getInjectionService(getProject(), "_urlGenerator");
        assertContainsElements(classes4, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes5 = ServicePropertyInsertUtil.getInjectionService(getProject(), "__url_generator");
        assertContainsElements(classes5, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes6 = ServicePropertyInsertUtil.getInjectionService(getProject(), "_router");
        assertContainsElements(classes6, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes7 = ServicePropertyInsertUtil.getInjectionService(getProject(), "foobar");
        assertContainsElements(classes7, "\\App\\Service\\FoobarInterface");

        List<String> classes8 = ServicePropertyInsertUtil.getInjectionService(getProject(), "_routerInterface");
        assertContainsElements(classes8, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes9 = ServicePropertyInsertUtil.getInjectionService(getProject(), "foobarCar");
        assertContainsElements(classes9, "\\App\\Service\\InterfaceFoobarCar");

        List<String> classes10 = ServicePropertyInsertUtil.getInjectionService(getProject(), "foobarCarInterface");
        assertContainsElements(classes10, "\\App\\Service\\InterfaceFoobarCar");

        List<String> classes11 = ServicePropertyInsertUtil.getInjectionService(getProject(), "fooBarLogger");
        assertContainsElements(classes11, "\\Psr\\Log\\LoggerInterface");

        List<String> classes12 = ServicePropertyInsertUtil.getInjectionService(getProject(), "foobarLongClassNameServiceFactory");
        assertContainsElements(classes12, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes13 = ServicePropertyInsertUtil.getInjectionService(getProject(), "longClassNameServiceFactory");
        assertContainsElements(classes13, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes14 = ServicePropertyInsertUtil.getInjectionService(getProject(), "nameServiceFactory");
        assertContainsElements(classes14, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes15 = ServicePropertyInsertUtil.getInjectionService(getProject(), "serviceFactory");
        assertFalse(classes15.contains("\\App\\Service\\FoobarLongClassNameServiceFactory"));

        List<String> classes16 = ServicePropertyInsertUtil.getInjectionService(getProject(), "_name_Service__Factory");
        assertContainsElements(classes16, "\\App\\Service\\FoobarLongClassNameServiceFactory");
    }

    public void testInjectionServiceWithName() {
        List<String> classes1 = ServicePropertyInsertUtil.getInjectionService(getProject(), "urlGenerator", "foobarUnknown");
        assertContainsElements(classes1, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes2 = ServicePropertyInsertUtil.getInjectionService(getProject(), "urlGenerator", "generate");
        assertContainsElements(classes2, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");
    }
}
