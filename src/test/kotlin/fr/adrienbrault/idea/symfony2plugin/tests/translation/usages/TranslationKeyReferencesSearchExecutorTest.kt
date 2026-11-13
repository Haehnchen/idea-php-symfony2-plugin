package fr.adrienbrault.idea.symfony2plugin.tests.translation.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyReferencesSearchExecutor
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyUsageUtil

class TranslationKeyReferencesSearchExecutorTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.addFileToProject(
            "vendor/symfony/translation.php",
            $$"""
            <?php
            namespace Symfony\Contracts\Translation {
                interface TranslatorInterface {
                    public function trans(string $id, array $parameters = [], ?string $domain = null, ?string $locale = null): string;
                }
            }
            namespace Symfony\Component\Translation {
                class TranslatableMessage {
                    public function __construct(string $message, array $parameters = [], ?string $domain = null) {}
                }
                function t(string $message, array $parameters = [], ?string $domain = null): TranslatableMessage {}
            }
            """.trimIndent()
        )
    }

    fun testFindsPhpUsagesOfYamlTranslationKey() {
        val target = configureDeclaration("translations/messages.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        myFixture.addFileToProject(
            "src/Translator.php",
            $$"""
            <?php
            /** @var \Symfony\Contracts\Translation\TranslatorInterface $translator */
            $translator->trans('home.oss.line.on');
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "src/TranslatableMessage.php",
            "<?php new \\Symfony\\Component\\Translation\\TranslatableMessage('home.oss.line.on');"
        )
        myFixture.addFileToProject(
            "src/TFunction.php",
            "<?php \\Symfony\\Component\\Translation\\t('home.oss.line.on');"
        )
        myFixture.addFileToProject(
            "src/OtherDomain.php",
            $$"""
            <?php
            /** @var \Symfony\Contracts\Translation\TranslatorInterface $translator */
            $translator->trans('home.oss.line.on', [], 'admin');
            """.trimIndent()
        )
        myFixture.addFileToProject("src/PlainString.php", "<?php \$foo = 'home.oss.line.on';")

        val files = findUsageFiles(target)

        assertUsageIn(files, "src/Translator.php", "src/TranslatableMessage.php", "src/TFunction.php")
        assertNoUsageIn(files, "src/OtherDomain.php", "src/PlainString.php")
    }

    fun testFindsTwigUsagesOfYamlTranslationKey() {
        val target = configureDeclaration("translations/messages.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        myFixture.addFileToProject("templates/filter.html.twig", "{{ 'home.oss.line.on'|trans }}")
        myFixture.addFileToProject("templates/filter_domain.html.twig", "{{ 'home.oss.line.on'|trans({}, 'messages') }}")
        myFixture.addFileToProject("templates/html.html.twig", "{{ 'home.oss.line.on'|trans_html }}")
        myFixture.addFileToProject("templates/tag.html.twig", "{% trans %}home.oss.line.on{% endtrans %}")
        myFixture.addFileToProject("templates/other_domain.html.twig", "{{ 'home.oss.line.on'|trans({}, 'admin') }}")
        myFixture.addFileToProject("templates/default_domain.html.twig", "{% trans_default_domain 'admin' %}{{ 'home.oss.line.on'|trans }}")
        myFixture.addFileToProject("templates/plain.html.twig", "{{ 'home.oss.line.on' }}")

        val files = findUsageFiles(target)

        assertUsageIn(
            files,
            "templates/filter.html.twig",
            "templates/filter_domain.html.twig",
            "templates/html.html.twig",
            "templates/tag.html.twig",
        )
        assertNoUsageIn(files, "templates/other_domain.html.twig", "templates/default_domain.html.twig", "templates/plain.html.twig")
    }

    fun testFindsFormAndConstraintUsages() {
        myFixture.addFileToProject(
            "vendor/symfony/form.php",
            $$"""
            <?php
            namespace Symfony\Component\Form {
                interface FormBuilderInterface {
                    public function add(string $child, ?string $type = null, array $options = []);
                }
            }
            namespace Symfony\Component\Validator {
                abstract class Constraint {
                    public function __construct(mixed $options = null) {}
                }
            }
            namespace Symfony\Component\Validator\Constraints {
                class NotBlank extends \Symfony\Component\Validator\Constraint {
                    public function __construct(mixed $options = null, ?string $message = null) {}
                }
            }
            """.trimIndent()
        )

        val label = configureDeclaration("translations/messages.en.yaml", "form.na<caret>me: Name")
        myFixture.addFileToProject(
            "src/Form.php",
            $$"""
            <?php
            /** @var \Symfony\Component\Form\FormBuilderInterface $builder */
            $builder->add('name', null, ['label' => 'form.name']);
            """.trimIndent()
        )

        assertUsageIn(findUsageFiles(label), "src/Form.php")

        val message = configureDeclaration("translations/validators.en.yaml", "name.not_bl<caret>ank: Required")
        myFixture.addFileToProject(
            "src/Constraint.php",
            "<?php new \\Symfony\\Component\\Validator\\Constraints\\NotBlank(message: 'name.not_blank');"
        )

        assertUsageIn(findUsageFiles(message), "src/Constraint.php")
    }

    fun testIntlIcuFileUsesDomainWithoutSuffix() {
        val target = configureDeclaration("translations/messages+intl-icu.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        myFixture.addFileToProject("templates/filter.html.twig", "{{ 'home.oss.line.on'|trans }}")

        assertUsageIn(findUsageFiles(target), "templates/filter.html.twig")
    }

    fun testNestedYamlKeyUsesFullKey() {
        val target = configureDeclaration(
            "translations/messages.en.yaml",
            """
            home:
                oss:
                    o<caret>n: 'on'
            """.trimIndent()
        )

        myFixture.addFileToProject("templates/full.html.twig", "{{ 'home.oss.on'|trans }}")
        myFixture.addFileToProject("templates/short.html.twig", "{{ 'on'|trans }}")

        val files = findUsageFiles(target)

        assertUsageIn(files, "templates/full.html.twig")
        assertNoUsageIn(files, "templates/short.html.twig")
    }

    fun testFindsUsagesOfPhpTranslationFileKey() {
        val target = configureDeclaration("translations/messages.en.php", "<?php return ['home.oss.li<caret>ne.on' => 'on'];")

        myFixture.addFileToProject("templates/filter.html.twig", "{{ 'home.oss.line.on'|trans }}")

        assertUsageIn(findUsageFiles(target), "templates/filter.html.twig")
    }

    private fun configureDeclaration(path: String, content: String): PsiElement {
        val caret = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caret >= 0)

        myFixture.addFileToProject(path, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(path))

        val leaf = myFixture.file.findElementAt(caret)
        assertNotNull(leaf)

        val declaration = TranslationKeyUsageUtil.getDeclaration(leaf!!)
        assertNotNull("No translation key declaration at caret in $path", declaration)

        return declaration!!.element
    }

    private fun findUsageFiles(target: PsiElement): List<String> {
        return ReferencesSearch.search(target, GlobalSearchScope.projectScope(project)).findAll()
            .filterIsInstance<TranslationKeyReferencesSearchExecutor.TranslationKeyUsageReference>()
            .mapNotNull { it.element.containingFile.virtualFile?.path }
            .distinct()
    }

    private fun assertUsageIn(paths: List<String>, vararg relativePaths: String) {
        for (relativePath in relativePaths) {
            assertTrue("Expected usage in $relativePath; actual: $paths", paths.any { it.endsWith("/$relativePath") })
        }
    }

    private fun assertNoUsageIn(paths: List<String>, vararg relativePaths: String) {
        for (relativePath in relativePaths) {
            assertFalse("Unexpected usage in $relativePath; actual: $paths", paths.any { it.endsWith("/$relativePath") })
        }
    }
}
