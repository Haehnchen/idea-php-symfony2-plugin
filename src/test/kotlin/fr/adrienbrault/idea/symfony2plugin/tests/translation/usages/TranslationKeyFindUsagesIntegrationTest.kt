package fr.adrienbrault.idea.symfony2plugin.tests.translation.usages

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.navigation.actions.GotoDeclarationOrUsageHandler2
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.lang.xml.XMLLanguage
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTargetUtil
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyFindUsagesHandlerFactory
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyUsageUtil
import fr.adrienbrault.idea.symfony2plugin.translation.usages.TranslationKeyUsageTargetProvider

class TranslationKeyFindUsagesIntegrationTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testStartsFindUsagesFromHtmlTranslator() {
        assertFindUsagesFromAdditionalTranslationCalls(listOf(
            "messages" to "\$translator->trans('notification_<caret>summary', []);",
            "notifications" to "\$translator->trans('notification_<caret>summary', [], ['name' => 'Example'], 'notifications');",
            "notifications" to "\$translator->trans(domain: 'notifications', tags: [], id: 'notification_<caret>summary');",
        ))
    }

    fun testStartsFindUsagesFromTranslatableMessage() {
        assertFindUsagesFromAdditionalTranslationCalls(listOf(
            "messages" to "new TranslatableMessage('notification_<caret>summary');",
            "notifications" to "new TranslatableMessage('notification_<caret>summary', ['name' => 'Example'], 'notifications');",
            "notifications" to "new TranslatableMessage(domain: 'notifications', message: 'notification_<caret>summary');",
        ))
    }

    fun testStartsFindUsagesFromTranslatorPropertyCall() {
        addTranslatorFixture()
        registerLocalizationFileType()
        configureByProjectPath(
            "translations/messages.en.xlf",
            """<xliff version="1.2"><file><body><trans-unit id="1"><source>notification_<caret>summary</source><target>Summary</target></trans-unit></body></file></xliff>""",
        )
        val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!.element
        myFixture.addFileToProject("templates/summary.html.twig", "{{ 'notification_summary'|trans }}")
        configureByProjectPath(
            "src/NotificationService.php",
            $$"""
            <?php
            use Symfony\Contracts\Translation\TranslatorInterface;
            class NotificationService {
                public function __construct(private TranslatorInterface $translator) {}
                public function summary(?\DateTimeImmutable $date): string {
                    return $this->translator->trans('notification_<caret>summary', [
                        'prefix' => 'Example',
                        'name' => $this->getDisplayName(),
                        'date' => $date?->format('d.m.Y') ?? '',
                    ]);
                }
            }
            """.trimIndent()
        )

        val usages = findUsagesFromEditor(declaration)
        assertContainsUsageFile(usages, "src/NotificationService.php")
        assertContainsUsageFile(usages, "templates/summary.html.twig")
    }

    fun testTranslationCallUsesPositionalAndNamedDomain() {
        addTranslatorFixture()
        myFixture.addFileToProject("translations/messages.en.yaml", "notification_summary: Default summary")
        configureByProjectPath("translations/notifications.en.yaml", "notification_<caret>summary: Summary")
        val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!.element
        myFixture.addFileToProject("templates/summary.html.twig", "{{ 'notification_summary'|trans({}, 'notifications') }}")
        myFixture.addFileToProject("templates/default.html.twig", "{{ 'notification_summary'|trans }}")

        for ((index, call) in listOf(
            "\$translator->trans('notification_<caret>summary', [], 'notifications');",
            "\$translator->trans(domain: 'notifications', id: 'notification_<caret>summary');",
        ).withIndex()) {
            configureByProjectPath(
                "src/Translation$index.php",
                "<?php /** @var \\Symfony\\Contracts\\Translation\\TranslatorInterface \$translator */ $call",
            )
            val usages = findUsagesFromEditor(declaration)
            assertContainsUsageFile(usages, "src/Translation$index.php")
            assertContainsUsageFile(usages, "templates/summary.html.twig")
            assertFalse(usages.any { it.virtualFile?.path?.endsWith("/templates/default.html.twig") == true })
        }
    }

    fun testTranslationCallIgnoresParameterValuesAndUnrelatedMethods() {
        addTranslatorFixture()
        myFixture.addFileToProject("translations/messages.en.yaml", "notification_summary: Summary")
        for ((index, call) in listOf(
            "\$translator->trans('notification_summary', ['name' => 'notification_<caret>summary']);",
            "\$translator->trans('notification_summary', [], 'notification_<caret>summary');",
            "\$translator->trans('notification_summary', locale: 'notification_<caret>summary');",
            "\$other->trans('notification_<caret>summary');",
        ).withIndex()) {
            configureByProjectPath(
                "src/Other$index.php",
                "<?php /** @var \\Symfony\\Contracts\\Translation\\TranslatorInterface \$translator */ $call",
            )
            assertNull(TranslationKeyUsageTargetProvider().getTargets(myFixture.editor, myFixture.file))
        }
    }

    fun testStartsFindUsagesFromFormLabel() {
        registerLocalizationFileType()
        addFormBuilderFixture()
        configureByProjectPath(
            "translations/messages.en.xlf",
            """<xliff version="1.2"><file><body><trans-unit id="1"><source>form_quantity_<caret>label</source><target>Quantity</target></trans-unit></body></file></xliff>""",
        )
        val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!.element
        myFixture.addFileToProject("templates/form.html.twig", "{{ 'form_quantity_label'|trans }}")
        configureByProjectPath(
            "src/Form/ExampleType.php",
            $$"""
            <?php
            namespace App\Form;
            use Symfony\Component\Form\AbstractType;
            use Symfony\Component\Form\FormBuilderInterface;
            use Symfony\Component\Form\Extension\Core\Type\NumberType;
            use Symfony\Component\Validator\Constraints\GreaterThanOrEqual;
            class ExampleType extends AbstractType {
                public function buildForm(FormBuilderInterface $builder, array $options): void {
                    $builder->add('quantity', NumberType::class, [
                        'label' => 'form_quantity_<caret>label',
                        'required' => false,
                        'scale' => 4,
                        'strip_decimal_places' => true,
                        'constraints' => [new GreaterThanOrEqual(value: 0.0)],
                    ]);
                }
            }
            """.trimIndent()
        )
        val usages = findUsagesFromEditor(declaration)
        assertContainsUsageFile(usages, "src/Form/ExampleType.php")
        assertContainsUsageFile(usages, "templates/form.html.twig")
    }

    fun testFormLabelUsesDeclaredDomainAndNormalizesYamlAndPhpTargets() {
        addFormBuilderFixture()
        myFixture.addFileToProject("translations/messages.en.yaml", "form.quantity_label: Default domain")
        myFixture.addFileToProject("templates/forms.html.twig", "{{ 'form.quantity_label'|trans({}, 'forms') }}")
        myFixture.addFileToProject("templates/default.html.twig", "{{ 'form.quantity_label'|trans }}")

        for ((extension, content) in listOf(
            "yaml" to "form:\n    quantity_<caret>label: Quantity",
            "php" to "<?php return ['form' => ['quantity_<caret>label' => 'Quantity']];",
        )) {
            configureByProjectPath("translations/forms.en.$extension", content)
            val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!
            configureByProjectPath(
                "src/Form/Label$extension.php",
                $$"""
                <?php
                /** @var \Symfony\Component\Form\FormBuilderInterface $builder */
                $builder->add('quantity', null, [
                    'label' => 'form.quantity_<caret>label',
                    'translation_domain' => 'forms',
                ]);
                """.trimIndent()
            )

            val targets = TranslationKeyUsageTargetProvider().getTargets(myFixture.editor, myFixture.file)!!
            assertEquals("Locales and formats must share one key/domain search", 1, targets.size)
            val target = (targets.single() as PsiElementUsageTarget).element!!
            val resolvedDeclaration = TranslationKeyUsageUtil.getDeclaration(target)!!
            assertEquals(declaration.key, resolvedDeclaration.key)
            assertEquals("forms", resolvedDeclaration.domain)
            val usages = findUsagesFromEditor(target)
            assertContainsUsageFile(usages, "templates/forms.html.twig")
            assertFalse(usages.any { it.virtualFile?.path?.endsWith("/templates/default.html.twig") == true })
        }
    }

    fun testUnrelatedPhpStringsDoNotStartTranslationUsages() {
        addFormBuilderFixture()
        myFixture.addFileToProject("translations/messages.en.yaml", "form.quantity_label: Quantity")
        for ((index, content) in listOf(
            "<?php \$options = ['label' => 'form.quantity_<caret>label'];",
            "<?php /** @var \\Symfony\\Component\\Form\\FormBuilderInterface \$builder */ \$builder->add('form.quantity_<caret>label', null, []);",
            "<?php /** @var \\Symfony\\Component\\Form\\FormBuilderInterface \$builder */ \$builder->add('quantity', null, ['label' => 'form.quantity_<caret>label', 'translation_domain' => 'missing']);",
        ).withIndex()) {
            configureByProjectPath("src/Example$index.php", content)
            assertNull(TranslationKeyUsageTargetProvider().getTargets(myFixture.editor, myFixture.file))
        }
    }

    fun testLocalizationFileTypeSupportsCtrlClickAndFindUsages() {
        val fileType = registerLocalizationFileType()
        myFixture.addFileToProject("templates/filter.html.twig", "{{ 'translation.key'|trans }}")

        val units = listOf(
            """<trans-unit id="translation.key"><source>translation.<caret>key</source><target>Translated text</target></trans-unit>""",
            """<trans-unit id="1"><source><![CDATA[translation.<caret>key]]></source><target>Translated text</target></trans-unit>""",
            """<trans-unit id="1" resname="translation.<caret>key"><source>translation.key</source><target>Translated text</target></trans-unit>""",
        )

        for (extension in listOf("xlf", "xliff")) {
            for ((index, unit) in units.withIndex()) {
                configureByProjectPath(
                    "translations/$index/messages.en.$extension",
                    """<xliff version="1.2"><file><body>$unit</body></file></xliff>""",
                )
                assertSame(fileType, myFixture.file.virtualFile.fileType)
                assertEquals("Ctrl+Click in $extension: $unit", GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
                assertContainsUsageFile(findUsagesFromEditor(), "templates/filter.html.twig")
            }
        }
    }

    fun testLocalizationFileTypeIgnoresUnrelatedXmlValues() {
        registerLocalizationFileType()
        val units = listOf(
            """<trans-unit id="arbitrary<caret>_id"><source>translation.key</source></trans-unit>""",
            """<trans-unit id="translation.<caret>key"><source>translation.key</source></trans-unit>""",
            """<trans-unit id="translation.<caret>key"><source>other.key</source></trans-unit><trans-unit id="2"><source>translation.key</source></trans-unit>""",
            """<trans-unit id="translation.key"><source>translation.key</source><target>Translated <caret>text</target></trans-unit>""",
        )
        for ((index, unit) in units.withIndex()) {
            configureByProjectPath(
                "translations/$index/messages.en.xlf",
                """<xliff version="1.2"><file><body>$unit</body></file></xliff>""",
            )
            assertNull(TranslationKeyUsageTargetProvider().getTargets(myFixture.editor, myFixture.file))
        }
    }

    fun testXmlFileTypeSupportsFindUsagesOnSourceAndResname() {
        withXliffAsXml {
            myFixture.addFileToProject("templates/filter.html.twig", "{{ 'translation.key'|trans }}")
            for ((index, unit) in listOf(
                """<trans-unit id="translation.key"><source>translation.<caret>key</source></trans-unit>""",
                """<trans-unit id="1" resname="translation.<caret>key"><source>translation.key</source></trans-unit>""",
            ).withIndex()) {
                configureByProjectPath(
                    "translations/$index/messages.en.xlf",
                    """<xliff version="1.2"><file><body>$unit</body></file></xliff>""",
                )
                assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
                assertContainsUsageFile(findUsagesFromEditor(), "templates/filter.html.twig")
            }
        }
    }

    fun testCtrlClickOnYamlKeyShowsUsages() {
        configureByProjectPath("translations/messages.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
    }

    fun testCtrlClickOnPhpTranslationKeyShowsUsages() {
        configureByProjectPath("translations/messages.en.php", "<?php return ['home.oss.li<caret>ne.on' => 'on'];")

        assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
    }

    fun testCtrlClickOnXliffSourceAndResnameShowsUsages() {
        withXliffAsXml {
            val content = """
                <?xml version="1.0"?>
                <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                    <file source-language="en" datatype="plaintext" original="file.ext">
                        <body>
                            <trans-unit id="a1" resname="RESNAME"><source>SOURCE</source><target>on</target></trans-unit>
                        </body>
                    </file>
                </xliff>
            """.trimIndent()

            configureByProjectPath("translations/messages.en.xlf", content.replace("SOURCE", "home.oss.li<caret>ne.on").replace("RESNAME", "home.oss.line.on"))
            assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())

            configureByProjectPath("translations/validators.en.xlf", content.replace("RESNAME", "name.not_bl<caret>ank").replace("SOURCE", "name.not_blank"))
            assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.SU, gtduOutcome())
        }
    }

    fun testXliffSourceUsagesAreFound() {
        withXliffAsXml {
            configureByProjectPath(
                "translations/messages.en.xlf",
                """
                <?xml version="1.0"?>
                <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                    <file source-language="en" datatype="plaintext" original="file.ext">
                        <body>
                            <trans-unit id="a1"><source>home.oss.li<caret>ne.on</source><target>on</target></trans-unit>
                        </body>
                    </file>
                </xliff>
                """.trimIndent()
            )
            myFixture.addFileToProject("templates/filter.html.twig", "{{ 'home.oss.line.on'|trans }}")

            assertContainsUsageFile(findUsages(), "templates/filter.html.twig")
        }
    }

    fun testFindUsagesTargetPresentation() {
        configureByProjectPath("translations/messages+intl-icu.en.yaml", "home.oss.li<caret>ne.on: 'on'")

        val handler = createHandler()
        val primaryTarget = handler.primaryElements[0] as NavigationItem

        assertEquals("home.oss.line.on", primaryTarget.presentation!!.presentableText)
        assertEquals("messages, translations/messages+intl-icu.en.yaml", primaryTarget.presentation!!.locationString)
    }

    fun testNonTranslationYamlKeyIsIgnored() {
        configureByProjectPath("config/packages/framework.yaml", "framework:\n    sec<caret>ret: foo")

        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        assertNull(TranslationKeyUsageUtil.getDeclaration(element))
        assertFalse(TranslationKeyFindUsagesHandlerFactory().canFindUsages(element.parent))
    }

    private fun gtduOutcome(): GotoDeclarationOrUsageHandler2.GTDUOutcome? =
        GotoDeclarationOrUsageHandler2.testGTDUOutcome(myFixture.editor, myFixture.file, myFixture.caretOffset)

    private fun createHandler() = TranslationKeyFindUsagesHandlerFactory().let { factory ->
        val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)
        assertNotNull(declaration)
        assertTrue(factory.canFindUsages(declaration!!.element))
        factory.createFindUsagesHandler(declaration.element, false)!!
    }

    private fun findUsages(): List<UsageInfo> {
        val handler = createHandler()
        return findUsages(handler)
    }

    private fun findUsagesFromEditor(expectedDeclaration: PsiElement = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!.element): List<UsageInfo> {
        val targetElement = TargetElementUtil.findTargetElement(myFixture.editor, TargetElementUtil.getInstance().referenceSearchFlags)
        val targets = UsageTargetUtil.findUsageTargets(myFixture.editor, myFixture.file, targetElement).orEmpty()
        // Find Usages uses the first target.
        assertEquals("Find Usages must target the translation key", expectedDeclaration, (targets.firstOrNull() as? PsiElementUsageTarget)?.element)
        val handler = TranslationKeyFindUsagesHandlerFactory().createFindUsagesHandler((targets.first() as PsiElementUsageTarget).element!!, false)!!
        return findUsages(handler)
    }

    private fun findUsages(handler: FindUsagesHandler): List<UsageInfo> {
        val options = handler.findUsagesOptions
        options.searchScope = GlobalSearchScope.projectScope(project)
        options.isSearchForTextOccurrences = false
        options.isUsages = true

        val usages = mutableListOf<UsageInfo>()
        assertTrue(handler.processElementUsages(handler.primaryElements[0], { usages.add(it) }, options))
        return usages
    }

    private fun configureByProjectPath(filePath: String, content: String) {
        val caretOffset = content.indexOf("<caret>")
        assertTrue("Missing <caret> marker", caretOffset >= 0)

        myFixture.addFileToProject(filePath, content.replace("<caret>", ""))
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir(filePath))
        myFixture.editor.caretModel.moveToOffset(caretOffset)
    }

    private fun withXliffAsXml(block: () -> Unit) {
        WriteAction.runAndWait<RuntimeException> { FileTypeManagerEx.getInstanceEx().associateExtension(XmlFileType.INSTANCE, "xlf") }
        try {
            block()
        } finally {
            WriteAction.runAndWait<RuntimeException> { FileTypeManager.getInstance().removeAssociatedExtension(XmlFileType.INSTANCE, "xlf") }
        }
    }

    private fun assertContainsUsageFile(usages: List<UsageInfo>, relativePath: String) {
        val paths = usages.map { it.virtualFile?.path ?: "<no-path>" }
        assertTrue("Expected usage in $relativePath; actual: $paths", paths.any { it.endsWith("/$relativePath") })
    }

    private class LocalizationFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
        override fun getName() = "XLF"
        override fun getDescription() = "XLIFF"
        override fun getDefaultExtension() = "xliff"
        override fun getIcon() = XmlFileType.INSTANCE.icon
    }

    private fun registerLocalizationFileType(): LocalizationFileType {
        // Mirrors the Localization plugin's file type.
        val fileType = LocalizationFileType()
        val manager = FileTypeManager.getInstance()
        for (extension in listOf("xlf", "xliff")) {
            val original = manager.getFileTypeByExtension(extension)
            WriteAction.runAndWait<RuntimeException> { manager.associateExtension(fileType, extension) }
            Disposer.register(testRootDisposable) {
                WriteAction.runAndWait<RuntimeException> {
                    manager.removeAssociatedExtension(fileType, extension)
                    if (original != UnknownFileType.INSTANCE) manager.associateExtension(original, extension)
                }
            }
        }
        return fileType
    }

    private fun addFormBuilderFixture() {
        myFixture.addFileToProject(
            "vendor/symfony/form.php",
            $$"""
            <?php
            namespace Symfony\Component\Form;
            interface FormBuilderInterface {
                public function add(string $child, ?string $type = null, array $options = []);
            }
            abstract class AbstractType {}
            """.trimIndent()
        )
    }

    private fun addTranslatorFixture() {
        myFixture.addFileToProject(
            "vendor/symfony/translation.php",
            $$"""
            <?php
            namespace Symfony\Contracts\Translation;
            interface TranslatorInterface {
                public function trans(string $id, array $parameters = [], ?string $domain = null, ?string $locale = null): string;
            }
            """.trimIndent()
        )
    }

    private fun assertFindUsagesFromAdditionalTranslationCalls(calls: List<Pair<String, String>>) {
        myFixture.addFileToProject(
            "vendor/symfony/translation.php",
            $$"""
            <?php
            namespace Symfony\Component\Translation;
            class HtmlTranslator {
                public function trans(string $id, array $tags, array $parameters = [], ?string $domain = null, ?string $locale = null): string {}
            }
            class TranslatableMessage {
                public function __construct(string $message, array $parameters = [], ?string $domain = null) {}
            }
            """.trimIndent()
        )
        val declarations = listOf("messages", "notifications").associateWith { domain ->
            configureByProjectPath("translations/$domain.en.yaml", "notification_<caret>summary: Summary")
            val declaration = TranslationKeyUsageUtil.getDeclaration(myFixture.file.findElementAt(myFixture.caretOffset)!!)!!.element
            myFixture.addFileToProject("templates/$domain.html.twig", "{{ 'notification_summary'|trans({}, '$domain') }}")
            declaration
        }

        for ((index, entry) in calls.withIndex()) {
            val (domain, call) = entry
            configureByProjectPath(
                "src/Translation$index.php",
                $$"""
                <?php
                use Symfony\Component\Translation\TranslatableMessage;
                /** @var \Symfony\Component\Translation\HtmlTranslator $translator */
                $$call
                """.trimIndent()
            )
            val usages = findUsagesFromEditor(declarations.getValue(domain))
            assertContainsUsageFile(usages, "src/Translation$index.php")
            assertContainsUsageFile(usages, "templates/$domain.html.twig")
            val otherDomain = if (domain == "messages") "notifications" else "messages"
            assertFalse(usages.any { it.virtualFile?.path?.endsWith("/templates/$otherDomain.html.twig") == true })
        }
    }
}
