package fr.adrienbrault.idea.symfony2plugin.tests.expressionLanguage;

import com.intellij.testFramework.ParsingTestCase;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguageParserDefinition;

public class ExpressionLanguageParsingTest extends ParsingTestCase {

    public ExpressionLanguageParsingTest() {
        super("", "test", new ExpressionLanguageParserDefinition());
    }

    public void testNotExpr() {
        doTest(true);
    }

    public void testNotKwExpr() {
        doTest(true);
    }

    public void testUnaryMinus() {
        doTest(true);
    }

    public void testUnaryPlus() {
        doTest(true);
    }

    public void testDivExpr() {
        doTest(true);
    }

    public void testMulExpr() {
        doTest(true);
    }

    public void testModExpr() {
        doTest(true);
    }

    public void testPlusExpr() {
        doTest(true);
    }

    public void testMinusExpr() {
        doTest(true);
    }

    public void testRangeExpr() {
        doTest(true);
    }

    public void testIdenticalExpr() {
        doTest(true);
    }

    public void testNotIdenticalExpr() {
        doTest(true);
    }

    public void testEqExpr() {
        doTest(true);
    }

    public void testNotEqExpr() {
        doTest(true);
    }

    public void testGtExpr() {
        doTest(true);
    }

    public void testGteExpr() {
        doTest(true);
    }

    public void testLtExpr() {
        doTest(true);
    }

    public void testLteExpr() {
        doTest(true);
    }

    public void testNotInExpr() {
        doTest(true);
    }

    public void testInExpr() {
        doTest(true);
    }

    public void testMatchesExpr() {
        doTest(true);
    }

    public void testOrExpr() {
        doTest(true);
    }

    public void testAndExpr() {
        doTest(true);
    }

    public void testBitAndExpr() {
        doTest(true);
    }

    public void testBitOrExpr() {
        doTest(true);
    }

    public void testBitXorExpr() {
        doTest(true);
    }

    public void testExpExpr() {
        doTest(true);
    }

    public void testParenExpr() {
        doTest(true);
    }

    public void testNumberLiteral() {
        doTest(true);
    }

    public void testNumberLiteralWithFraction() {
        doTest(true);
    }

    public void testExponentialNotationLiteral() {
        doTest(true);
    }

    public void testStringDoubleQuoteLiteral() {
        doTest(true);
    }

    public void testEmptyStringDoubleQuoteLiteral() {
        doTest(true);
    }

    public void testStringSingleQuoteLiteral() {
        doTest(true);
    }

    public void testEmptyStringSingleQuoteLiteral() {
        doTest(true);
    }

    public void testTrueLiteral() {
        doTest(true);
    }

    public void testTrueUppercaseLiteral() {
        doTest(true);
    }

    public void testFalseLiteral() {
        doTest(true);
    }

    public void testFalseUppercaseLiteral() {
        doTest(true);
    }

    public void testNullLiteral() {
        doTest(true);
    }

    public void testNullUppercaseLiteral() {
        doTest(true);
    }

    public void testEmptyArrayLiteral() {
        doTest(true);
    }

    public void testArrayLiteral() {
        doTest(true);
    }

    public void testEmptyHashLiteral() {
        doTest(true);
    }

    public void testHashLiteral() {
        doTest(true);
    }

    public void testFunctionCallWithoutArgs() {
        doTest(true);
    }

    public void testFunctionCallWithArgs() {
        doTest(true);
    }

    public void testMethodCallWithoutArgs() {
        doTest(true);
    }

    public void testMethodCallWithArgs() {
        doTest(true);
    }

    public void testTernaryExpr() {
        doTest(true);
    }

    public void testElvisExpr() {
        doTest(true);
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/expressionLanguage/testData";
    }
}
