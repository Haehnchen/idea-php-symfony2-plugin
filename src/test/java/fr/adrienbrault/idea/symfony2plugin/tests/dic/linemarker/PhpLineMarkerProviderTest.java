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
