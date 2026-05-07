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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("index"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("show"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("show"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("index"));

        assertContainsElements(vars.keySet(), "headline", "items");
    }

    /**
     * @see PhpMethodVariableResolveUtil#collectMethodVariables
     */
    public void testCollectMethodVariablesForLocalMethodControllerReturn() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "class ProductController\n" +
            "{\n" +
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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("index"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("index"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("index"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("show"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(findFunction("show"));

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

        assertContainsElements(vars.keySet(), "item", "deleteForm");
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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

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

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

        assertContainsElements(vars.keySet(), "foobar");
    }
}
