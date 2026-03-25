package fr.adrienbrault.idea.symfony2plugin.tests.dic.linemarker;

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.dic.linemarker.PhpLineMarkerProvider
 */
public class PhpLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatPhpArrayDecoratesOutsideServicesDoesNotProvideMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "return [\n" +
            "    'not_services' => [\n" +
            "        'app.decorator' => [\n" +
            "            'decorates' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }

    public void testThatAssignedServicesArrayDoesNotProvideMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "$foo = [\n" +
            "    'services' => [\n" +
            "        'app.decorator' => [\n" +
            "            'decorates' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }

    public void testThatAssignedAppConfigArrayDoesNotProvideMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "$foo = App::config([\n" +
            "    'services' => [\n" +
            "        'app.decorator' => [\n" +
            "            'decorates' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }

    public void testThatPhpArrayDecoratesProvidesForwardMarkerForStringServiceId() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'app.mailer' => null,\n" +
            "        'app.decorator' => [\n" +
            "            'decorates' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decorated service"));
        assertLineMarker(myFixture.getFile(), new LineMarker.TargetAcceptsPattern("Navigate to decorated service",
            PlatformPatterns.psiElement().withText("'app.mailer'")
        ));
    }

    public void testThatAppConfigPhpArrayDecoratesProvidesForwardMarkerForStringServiceId() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        'app.mailer' => null,\n" +
            "        'app.decorator' => [\n" +
            "            'decorates' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decorated service"));
    }

    public void testThatPhpArrayParentProvidesForwardMarkerForStringServiceId() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'app.mailer' => null,\n" +
            "        'app.child' => [\n" +
            "            'parent' => 'app.mail<caret>er',\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to parent service"));
    }

    public void testThatPhpArrayDecoratesProvidesForwardMarkerForClassConstant() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "use App\\DecoratingMailer;\n" +
            "use App\\Mailer;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        Mailer::class => null,\n" +
            "        DecoratingMailer::class => [\n" +
            "            'decorates' => Mail<caret>er::class,\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decorated service"));
    }

    public void testThatPhpArrayDecoratedServiceProvidesReverseMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "use App\\DecoratingMailer;\n" +
            "use App\\Mailer;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        Mail<caret>er::class => null,\n" +
            "        DecoratingMailer::class => [\n" +
            "            'decorates' => Mailer::class,\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decoration"));
    }

    public void testThatPhpArrayParentServiceProvidesReverseMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "use App\\ChildMailer;\n" +
            "use App\\Mailer;\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        Mail<caret>er::class => null,\n" +
            "        ChildMailer::class => [\n" +
            "            'parent' => Mailer::class,\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to parent"));
    }

    public void testThatPhpFluentDecorateProvidesForwardMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "use App\\DecoratingMailer;\n" +
            "use App\\Mailer;\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $services = $container->services();\n" +
            "    $services->set(DecoratingMailer::class)\n" +
            "        ->decorate(Mail<caret>er::class);\n" +
            "};");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decorated service"));
    }

    public void testThatPhpFluentParentProvidesForwardMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "use App\\ChildMailer;\n" +
            "use App\\Mailer;\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $services = $container->services();\n" +
            "    $services->set(ChildMailer::class)\n" +
            "        ->parent(Mail<caret>er::class);\n" +
            "};");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to parent service"));
    }

    public void testThatPhpFluentDecoratedServiceProvidesReverseMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "use App\\DecoratingMailer;\n" +
            "use App\\Mailer;\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $services = $container->services();\n" +
            "    $services->set(Mail<caret>er::class);\n" +
            "    $services->set(DecoratingMailer::class)\n" +
            "        ->decorate(Mailer::class);\n" +
            "};");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to decoration"));
    }

    public void testThatPhpFluentParentServiceProvidesReverseMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "use App\\ChildMailer;\n" +
            "use App\\Mailer;\n" +
            "return static function (ContainerConfigurator $container): void {\n" +
            "    $services = $container->services();\n" +
            "    $services->set(Mail<caret>er::class);\n" +
            "    $services->set(ChildMailer::class)\n" +
            "        ->parent(Mailer::class);\n" +
            "};");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarker(myFixture.getFile(), new LineMarker.ToolTipEqualsAssert("Navigate to parent"));
    }

    public void testThatNonSymfonyFluentDecorateDoesNotProvideMarker() {
        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace App;\n" +
            "class FakeServices {\n" +
            "    public function set(string $id): self { return $this; }\n" +
            "    public function decorate(string $id): self { return $this; }\n" +
            "}\n" +
            "function test(): void {\n" +
            "    $services = new FakeServices();\n" +
            "    $services->set('app.decorator')->decorate('app.mail<caret>er');\n" +
            "}\n");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        assertLineMarkerIsEmpty(myFixture.getFile());
    }

    public void testThatPhpArrayServiceResourceIsHavingLinemarker() {
        myFixture.addFileToProject("src/Service/ResourceLineMarkerFoo.php", "<?php\n" +
            "namespace App\\Service;\n" +
            "class ResourceLineMarkerFoo {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        'App\\\\Service\\\\<caret>' => [\n" +
            "            'resource' => '../src/Service/*',\n" +
            "            'exclude' => '../src/DependencyInjection/',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        PsiElement phpConfig = myFixture.getFile();
        assertNotNull(phpConfig);

        assertLineMarker(phpConfig, new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(phpConfig, new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(@NotNull PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Service\\ResourceLineMarkerFoo".equals(phpClass.getFQN());
                }
            }))
        );
    }

    public void testThatPhpArrayServiceResourceUsesDefaultsAutowireParsingPath() {
        myFixture.addFileToProject("src/Service/ResourceLineMarkerFoo.php", "<?php\n" +
            "namespace App\\Service;\n" +
            "class ResourceLineMarkerFoo {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        '_defaults' => ['autowire' => true],\n" +
            "        'App\\\\Service\\\\<caret>' => [\n" +
            "            'resource' => '../src/Service/*',\n" +
            "        ],\n" +
            "    ],\n" +
            "]);");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        PsiElement phpConfig = myFixture.getFile();
        assertNotNull(phpConfig);

        assertLineMarker(phpConfig, new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(phpConfig, new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(@NotNull PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Service\\ResourceLineMarkerFoo".equals(phpClass.getFQN());
                }
            }))
        );
    }

    public void testThatDirectPhpArrayServiceResourceIsHavingLinemarker() {
        myFixture.addFileToProject("src/Service/ResourceLineMarkerFoo.php", "<?php\n" +
            "namespace App\\Service;\n" +
            "class ResourceLineMarkerFoo {}\n");

        PsiFile configFile = myFixture.addFileToProject("config/services.php", "<?php\n" +
            "return [\n" +
            "    'services' => [\n" +
            "        'App\\\\Service\\\\<caret>' => [\n" +
            "            'resource' => '../src/Service/*',\n" +
            "        ],\n" +
            "    ],\n" +
            "];");
        myFixture.configureFromExistingVirtualFile(configFile.getVirtualFile());

        PsiElement phpConfig = myFixture.getFile();
        assertNotNull(phpConfig);

        assertLineMarker(phpConfig, new LineMarker.ToolTipEqualsAssert("Navigate to class"));
        assertLineMarker(phpConfig, new LineMarker.TargetAcceptsPattern("Navigate to class",
            PlatformPatterns.psiElement(PhpClass.class).with(new PatternCondition<>("fqn") {
                @Override
                public boolean accepts(@NotNull PhpClass phpClass, ProcessingContext context) {
                    return "\\App\\Service\\ResourceLineMarkerFoo".equals(phpClass.getFQN());
                }
            }))
        );
    }
}
