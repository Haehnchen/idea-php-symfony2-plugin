package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Function;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

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
            "$x->render('foo', $var);\n" +
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
            "$x->render('foo', ['foobar' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

        assertContainsElements(vars.keySet(), "foobar");
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
            "$x->render('foo', array_merge($var, ['foobar1' => $myVar]));\n" +
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
    public void testCollectMethodVariablesForBinaryExpression() {
        Function function = PhpPsiElementFactory.createFunction(getProject(), "function foobar() {\n" +
            "$myVar = new \\MyVars\\MyVar();\n" +
            "$var['foobar'] = $myVar;\n" +
            "\n" +
            "/** @var $x \\Symfony\\Component\\Templating\\EngineInterface */\n" +
            "$x->render('foo', $var + ['foobar1' => $myVar]);\n" +
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
            "$x->render('foo', $var += ['foobar1' => $myVar]);\n" +
            "\n" +
            "}"
        );

        Map<String, PsiVariable> vars = PhpMethodVariableResolveUtil.collectMethodVariables(function);

        assertContainsElements(vars.keySet(), "foobar");
        assertContainsElements(vars.keySet(), "foobar1");
    }
}
