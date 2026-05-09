package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Function;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMethodVariableResolveUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("PhpMethodVariableResolveUtilTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures";
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForVariablesReferences() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "$var = ['foobar1' => $myVar];\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', $var);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArrayCreation() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', ['foobar' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesCarriesPrimitiveFormTypeFqns() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form { interface FormTypeInterface {} }\n" +
            "namespace Symfony\\Bundle\\FrameworkBundle\\Controller { class AbstractController { public function createForm($type) {} } }\n" +
            "namespace App\\Form { class ProductType implements \\Symfony\\Component\\Form\\FormTypeInterface {} }\n" +
            "namespace App\\Controller {\n" +
            "  class ProductController extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController {\n" +
            "    public function index() {\n" +
            "      $form = $this->createForm(\\App\\Form\\ProductType::class);\n" +
            "      return $this->render('product.html.twig', ['form' => $form->createView()]);\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("index"), "product.html.twig");

        assertContainsElements(vars.keySet(), "form");
        assertContainsElements(vars.get("form").getFormTypeFqns(), "\\App\\Form\\ProductType");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArrayMerge() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', array_merge($var, ['foobar1' => $myVar]));\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForDirectArrayReplaceAndMergeRecursive() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', array_replace(array_merge_recursive($var, ['foobar1' => $myVar]), ['foobar2' => $myVar]));\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar", "foobar1", "foobar2");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArraySpreadWithVariable() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$baseData = ['item' => $myVar];\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', [\n" +
            "    'headline' => 'Foo',\n" +
            "    ...$baseData,\n" +
            "]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "headline", "item");
        assertContainsElements(vars.get("item").getTypes(), "\\MyVars\\MyVar");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArraySpreadWithLocalMethodReturn() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function show()\n" +
            "    {\n" +
            "        $item = new \\MyVars\\MyVar();\n" +
            "        return $this->render('foo.html.twig', [\n" +
            "            'item' => $item,\n" +
            "            ...$this->createSidebarData($item),\n" +
            "        ]);\n" +
            "    }\n" +
            "\n" +
            "    private function createSidebarData(\\MyVars\\MyVar $item): array\n" +
            "    {\n" +
            "        return [\n" +
            "            'sidebarTitle' => 'Related products',\n" +
            "            'relatedItems' => $this->repository->findRelated($item),\n" +
            "        ];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("show"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "item", "sidebarTitle", "relatedItems");
        assertContainsElements(vars.get("item").getTypes(), "\\MyVars\\MyVar");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForMultipleArraySpreads() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function show()\n" +
            "    {\n" +
            "        $baseData = ['headline' => 'Product'];\n" +
            "        return $this->render('foo.html.twig', [\n" +
            "            ...$baseData,\n" +
            "            ...$this->createDetailsData(),\n" +
            "            'activeTab' => 'details',\n" +
            "        ]);\n" +
            "    }\n" +
            "\n" +
            "    private function createDetailsData(): array\n" +
            "    {\n" +
            "        return ['details' => 'Details'];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("show"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "headline", "details", "activeTab");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForDirectLocalMethodContext() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function index()\n" +
            "    {\n" +
            "        return $this->render('foo.html.twig', $this->createTemplateData());\n" +
            "    }\n" +
            "\n" +
            "    private function createTemplateData(): array\n" +
            "    {\n" +
            "        return [\n" +
            "            'headline' => 'Products',\n" +
            "            'items' => $this->repository->findAll(),\n" +
            "        ];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("index"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "headline", "items");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForLocalMethodControllerReturn() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "class ProductController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template(\"product/index.html.twig\")\n" +
            "     */\n" +
            "    public function index()\n" +
            "    {\n" +
            "        return $this->createTemplateData();\n" +
            "    }\n" +
            "\n" +
            "    private function createTemplateData(): array\n" +
            "    {\n" +
            "        return ['headline' => 'Products'];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("index"), "product/index.html.twig");

        assertContainsElements(vars.keySet(), "headline");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForLocalMethodReturningVariableContext() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function index()\n" +
            "    {\n" +
            "        return $this->render('foo.html.twig', $this->createTemplateData());\n" +
            "    }\n" +
            "\n" +
            "    private function createTemplateData(): array\n" +
            "    {\n" +
            "        $data = ['headline' => 'Products'];\n" +
            "        $data['table'] = $this->tableFactory->create();\n" +
            "        return $data;\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("index"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "headline", "table");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForVariableAssignedFromLocalMethodContext() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function index()\n" +
            "    {\n" +
            "        $templateData = $this->createTemplateData();\n" +
            "        $templateData['contextNotification'] = $this->notificationRenderer->render();\n" +
            "        return $this->render('foo.html.twig', $templateData);\n" +
            "    }\n" +
            "\n" +
            "    private function createTemplateData(): array\n" +
            "    {\n" +
            "        return [\n" +
            "            'headline' => 'Products',\n" +
            "            'items' => $this->repository->findAll(),\n" +
            "        ];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("index"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "headline", "items", "contextNotification");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArrayMergeAssignment() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function show()\n" +
            "    {\n" +
            "        $item = new \\MyVars\\MyVar();\n" +
            "        $templateData = ['item' => $item];\n" +
            "        $templateData = array_merge(\n" +
            "            $templateData,\n" +
            "            $this->createPaginationData(),\n" +
            "            ['activeTab' => 'overview']\n" +
            "        );\n" +
            "        return $this->render('foo.html.twig', $templateData);\n" +
            "    }\n" +
            "\n" +
            "    private function createPaginationData(): array\n" +
            "    {\n" +
            "        return [\n" +
            "            'previousUrl' => '/previous',\n" +
            "            'nextUrl' => '/next',\n" +
            "        ];\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("show"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "item", "previousUrl", "nextUrl", "activeTab");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForArrayReplaceAndMergeRecursiveAssignments() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function show()\n" +
            "    {\n" +
            "        $filters = [];\n" +
            "        $templateData = ['item' => new \\MyVars\\MyVar()];\n" +
            "        $templateData = array_replace($templateData, ['activeTab' => 'overview']);\n" +
            "        $templateData = array_merge_recursive($templateData, ['filters' => $filters]);\n" +
            "        return $this->render('foo.html.twig', $templateData);\n" +
            "    }\n" +
            "}\n"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(findFunction("show"), "foo.html.twig");

        assertContainsElements(vars.keySet(), "item", "activeTab", "filters");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForConditionalArrayIndexAssignment() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$templateData = ['item' => new \\MyVars\\MyVar()];\n" +
            "if (true) {\n" +
            "    $templateData['deleteForm'] = $this->createDeleteForm()->createView();\n" +
            "}\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', $templateData);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "item", "deleteForm");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesFiltersRenderCallsByTemplateName() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function show() {\n" +
            "$product = new \\MyVars\\MyVar();\n" +
            "$form = new \\MyVars\\MyVar();\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('product/show.html.twig', ['product' => $product]);\n" +
            "$x->render('product/edit.html.twig', ['form' => $form]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> showVars = collectMethodVariables(function, "product/show.html.twig");
        assertContainsElements(showVars.keySet(), "product");
        assertFalse(showVars.containsKey("form"));

        Map<String, PsiVariable> editVars = collectMethodVariables(function, "product/edit.html.twig");
        assertContainsElements(editVars.keySet(), "form");
        assertFalse(editVars.containsKey("product"));
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesSharesConditionalRenderCallForAllResolvedTemplates() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function show() {\n" +
            "$product = new \\MyVars\\MyVar();\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render($preview ? 'product/preview.html.twig' : 'product/show.html.twig', ['product' => $product]);\n" +
            "\n" +
            "}"
        );

        assertContainsElements(collectMethodVariables(function, "product/preview.html.twig").keySet(), "product");
        assertContainsElements(collectMethodVariables(function, "product/show.html.twig").keySet(), "product");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesSupportsNamedAndReorderedArguments() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function show() {\n" +
            "$product = new \\MyVars\\MyVar();\n" +
            "$card = new \\MyVars\\MyVar();\n" +
            "$value = new \\MyVars\\MyVar();\n" +
            "\n" +
            "$this->render(parameters: ['product' => $product], view: 'product/show.html.twig');\n" +
            "$twig->render(context: ['card' => $card], name: 'widget/card.html.twig');\n" +
            "$this->renderBlock(parameters: ['blockVar' => $value], block: 'content', view: 'product/block.html.twig');\n" +
            "$this->renderBlock(block: 'content.html.twig', parameters: ['wrong' => $value], view: 'product/named-block.html.twig');\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> showVars = collectMethodVariables(function, "product/show.html.twig");
        assertContainsElements(showVars.keySet(), "product");
        assertFalse(showVars.containsKey("card"));
        assertFalse(showVars.containsKey("blockVar"));

        Map<String, PsiVariable> cardVars = collectMethodVariables(function, "widget/card.html.twig");
        assertContainsElements(cardVars.keySet(), "card");
        assertFalse(cardVars.containsKey("product"));

        assertContainsElements(collectMethodVariables(function, "product/block.html.twig").keySet(), "blockVar");
        assertFalse(collectMethodVariables(function, "content.html.twig").containsKey("wrong"));
    }

    @NotNull
    private Map<String, PsiVariable> collectMethodVariables(@NotNull Function function, @NotNull String... templateNames) {
        return PhpMethodVariableResolveUtil.collectMethodVariables(function, Arrays.asList(templateNames));
    }

    @NotNull
    private Function findFunction(@NotNull String name) {
        for (PsiElement psiElement : PsiTreeUtil.collectElementsOfType(myFixture.getFile(), Function.class)) {
            Function function = (Function) psiElement;
            if (name.equals(function.getName())) {
                return function;
            }
        }

        fail("Function not found: " + name);
        throw new IllegalStateException(name);
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForBinaryExpression() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', $var + ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForOperatorSelfAssignment() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo.html.twig', $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForTernary() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render(true === true ? 'foo.html.twig' : 'foo', $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForCoalesce() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$test = 'foo.html.twig'\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render($foobar ?? $test, $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForVariable() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$test = 'foo.html.twig'\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render($test, $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForVariableWithInvalidTemplateNameString() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$test = 'foo.html'\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render($test, $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertFalse(vars.containsKey("foobar"));
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForRenderBlock() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$x->renderBlock('foo.html.twig', 'content', ['foobar' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForRenderBlockView() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$x->renderBlockView('foo.html.twig', 'content', ['foobar' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = collectMethodVariables(function, "foo.html.twig");

        assertContainsElements(vars.keySet(), "foobar");
    }
}
