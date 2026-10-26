package fr.adrienbrault.idea.symfony2plugin.tests.form.completion;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.completion.FormCompletionContributor
 */
public class FormCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/completion/fixtures";
    }

    public void testClassConstantsCompletionWithoutNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', HiddenType<caret>)",
            "<?php\n /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n $foo->add('', \\Symfony\\Component\\Form\\Extension\\Core\\Type\\HiddenType::class)",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

    public void testClassConstantsCompletionWithNamespaceShouldInsertUseAndStripNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Foo {\n" +
                "  /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $foo->add('', HiddenType<caret>);\n" +
                "};",
            "<?php\n" +
                "namespace Foo {\n\n" +
                "    use Symfony\\Component\\Form\\Extension\\Core\\Type\\HiddenType;\n\n" +
                "    /** @var $foo \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $foo->add('', HiddenType::class);\n" +
                "};",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

    public void testAttributeFormTypeInsertionWithoutNamespace() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n/** @var \\Symfony\\Component\\Form\\FormBuilderInterface $builder */\n$builder->add('account', AccountData<caret>);",
            "<?php\n/** @var \\Symfony\\Component\\Form\\FormBuilderInterface $builder */\n$builder->add('account', \\App\\Form\\AccountData::class);",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

    public void testAttributeFormTypeInsertionWithImport() {
        assertCompletionResultEquals(PhpFileType.INSTANCE,
            "<?php\n" +
                "namespace Foo {\n" +
                "  /** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $builder->add('account', AccountData<caret>);\n" +
                "};",
            "<?php\n" +
                "namespace Foo {\n\n" +
                "    use App\\Form\\AccountData;\n\n" +
                "    /** @var $builder \\Symfony\\Component\\Form\\FormBuilderInterface */\n" +
                "  $builder->add('account', AccountData::class);\n" +
                "};",
            new LookupElementInsert.Icon(Symfony2Icons.FORM_TYPE)
        );
    }

    public void testBuilderTypeArguments() {
        assertAttributeFormTypeCompletion(builderCall("add('account', <caret>)"));
        assertAttributeFormTypeCompletion(builderCall("create(options: [], type: <caret>, name: 'account')"));
        assertAttributeFormTypeCompletion("<?php\n/** @var \\Symfony\\Component\\Form\\FormInterface $form */\n$form->add('account', <caret>);");
    }

    public void testFactoryTypeArguments() {
        assertAttributeFormTypeCompletion(factoryCall("create(<caret>)"));
        assertAttributeFormTypeCompletion(factoryCall("createBuilder(<caret>)"));
        assertAttributeFormTypeCompletion(factoryCall("createNamed('account', <caret>)"));
        assertAttributeFormTypeCompletion(factoryCall("createNamedBuilder(type: <caret>, name: 'account')"));
    }

    public void testControllerTypeArguments() {
        assertAttributeFormTypeCompletion(controllerCall("createForm(<caret>)"));
        assertAttributeFormTypeCompletion(controllerCall("createForm(data: null, type: <caret>)"));
    }

    public void testOtherArgumentsDoNotSuggestFormTypes() {
        assertNoFormTypeCompletion(builderCall("add(<caret>)"), "AccountData");
        assertNoFormTypeCompletion(builderCall("add('account', options: <caret>)"), "AccountData");
        assertNoFormTypeCompletion(factoryCall("create(data: <caret>)"), "AccountData");
    }

    public void testAbstractAndUnattributedClassesAreExcluded() {
        assertNoFormTypeCompletion(builderCall("add('account', <caret>)"), "AbstractAccountData");
        assertNoFormTypeCompletion(builderCall("add('account', <caret>)"), "UnattributedData");
    }

    private void assertAttributeFormTypeCompletion(String code) {
        assertCompletionLookupContainsPresentableItem(PhpFileType.INSTANCE, code, presentation ->
            "AccountData".equals(presentation.getItemText())
                && presentation.getIcon() == Symfony2Icons.FORM_TYPE
                && presentation.getTailText() == null
        );
    }

    private void assertNoFormTypeCompletion(String code, String name) {
        myFixture.configureByText(PhpFileType.INSTANCE, code);
        myFixture.completeBasic();
        assertNotNull(myFixture.getLookupElements());
        assertNull(ContainerUtil.find(myFixture.getLookupElements(), element -> {
            LookupElementPresentation presentation = new LookupElementPresentation();
            element.renderElement(presentation);
            return name.equals(presentation.getItemText()) && presentation.getIcon() == Symfony2Icons.FORM_TYPE;
        }));
    }

    private String builderCall(String call) {
        return "<?php\n/** @var \\Symfony\\Component\\Form\\FormBuilderInterface $builder */\n$builder->" + call + ";";
    }

    private String factoryCall(String call) {
        return "<?php\n/** @var \\Symfony\\Component\\Form\\FormFactoryInterface $factory */\n$factory->" + call + ";";
    }

    private String controllerCall(String call) {
        return "<?php\nclass Controller extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController {\n" +
            "    public function index() { $this->" + call + "; }\n" +
            "}";
    }

}
