package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigReferenceContributor
 */
public class PhpConfigReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("tags.yml");
        myFixture.copyFileToProject("services.yml");
        myFixture.copyFileToProject("../../../dic/container/util/fixtures/services_array.php", "services_array.php");
        myFixture.addFileToProject("config/php_array_targets.php", "<?php\nnamespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\nreturn App::config([\n    'services' => [\n        'app.mailer' => ['class' => \\App\\Service\\Mailer::class],\n        'app.parent' => ['class' => \\App\\Service\\Mailer::class],\n        'app.logger' => ['class' => \\App\\Service\\Logger::class],\n        'app.factory' => ['class' => \\App\\Factory\\MailerFactory::class],\n    ],\n]);\n");
        myFixture.addFileToProject("config/parameters.yaml", "parameters:\n    mailer.transport: smtp\n");
        myFixture.addFileToProject("src/Service/Mailer.php", "<?php\nnamespace App\\Service;\nclass Mailer {}\n");
        myFixture.addFileToProject("src/Service/DecoratingMailer.php", "<?php\nnamespace App\\Service;\nclass DecoratingMailer extends Mailer {}\n");
        myFixture.addFileToProject("src/Service/Logger.php", "<?php\nnamespace App\\Service;\nclass Logger {}\n");
        myFixture.addFileToProject("src/Service/Foo.php", "<?php\nnamespace App\\Service;\nclass Foo { public function setLogger(Logger $logger): void {} }\n");
        myFixture.addFileToProject("src/Factory/MailerFactory.php", "<?php\nnamespace App\\Factory;\nclass MailerFactory { public function create(): \\App\\Service\\Mailer { return new \\App\\Service\\Mailer(); } }\n");
        myFixture.addFileToProject("src/Form/FooType.php", "<?php\nnamespace espend\\Form\\TypeBundle\\Form;\nclass FooType {}\n");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/php/fixtures";
    }

    public void testTagReferences() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->addTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->clearTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->hasTag('<caret>')",
            "foobar"
        );
    }

    public void testEventNameCompletionForAsEventListener() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: '<caret>')]\n" +
                "final class MyMultiListener implements \\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface\n" +
                "{\n" +
                "\n" +
                "}",
            "yaml_event_2"
        );
    }

    public void testServiceCompletionForPhpArrayConfigServices() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return App::config([\n" +
                "    'services' => [\n" +
                "        'foo' => [\n" +
                "            'arguments' => [service('<caret>')],\n" +
                "        ],\n" +
                "    ],\n" +
                "]);",
            "app.my_service"
        );
    }

    public void testServiceNavigationForPhpArrayConfigServices() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return App::config([\n" +
                "    'services' => [\n" +
                "        'foo' => [\n" +
                "            'arguments' => [service('app.my<caret>_service')],\n" +
                "        ],\n" +
                "    ],\n" +
                "]);");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        StringLiteralExpression stringLiteralExpression = PsiTreeUtil.getParentOfType(psiElement, StringLiteralExpression.class);
        assertNotNull(stringLiteralExpression);

        boolean resolved = false;
        for (PsiReference reference : stringLiteralExpression.getReferences()) {
            if (PlatformPatterns.psiElement().accepts(reference.resolve())) {
                resolved = true;
                break;
            }
        }

        assertTrue(resolved);
    }

    public void testDecoratesReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return App::config([\n" +
                "    'services' => [\n" +
                "        'app.decorator' => [\n" +
                "            'class' => \\App\\Service\\DecoratingMailer::class,\n" +
                "            'decorates' => 'app.mail<caret>er',\n" +
                "        ],\n" +
                "    ],\n" +
                "]);",
            PlatformPatterns.psiElement(PhpClass.class).withName("Mailer")
        );
    }

    public void testParentReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.child' => [\n" +
                "            'parent' => 'app.par<caret>ent',\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Mailer")
        );
    }

    public void testClassStringReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'class' => '\\\\App\\\\Service\\\\Mail<caret>er',\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Mailer")
        );
    }

    public void testArgumentsServiceReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'arguments' => ['@app.log<caret>ger'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Logger")
        );
    }

    public void testArgumentsClassReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'arguments' => ['\\\\App\\\\Service\\\\Mail<caret>er'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Mailer")
        );
    }

    public void testTagsSimpleReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'tags' => ['foo<caret>bar'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement()
        );
    }

    public void testTagsArrayNameReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'tags' => [['name' => 'foo<caret>bar', 'command' => 'app:run']],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement()
        );
    }

    public void testAliasReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.alias' => '@app.log<caret>ger',\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Logger")
        );
    }

    public void testFactoryServiceReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'factory' => ['@app.fact<caret>ory', 'create'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("MailerFactory")
        );
    }

    public void testFactoryMethodReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.mailer' => [\n" +
                "            'factory' => ['@app.factory', 'cr<caret>eate'],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(Method.class).withName("create")
        );
    }

    public void testCallsMethodReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'class' => '\\\\App\\\\Service\\\\Foo',\n" +
                "            'calls' => [['setLog<caret>ger', []]],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(Method.class).withName("setLogger")
        );
    }

    public void testCallsArgumentServiceReference() {
        assertReferenceMatchOnParent(
            PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return [\n" +
                "    'services' => [\n" +
                "        'app.foo' => [\n" +
                "            'class' => '\\\\App\\\\Service\\\\Foo',\n" +
                "            'calls' => [['setLogger', ['@app.log<caret>ger']]],\n" +
                "        ],\n" +
                "    ],\n" +
                "];",
            PlatformPatterns.psiElement(PhpClass.class).withName("Logger")
        );
    }

}
