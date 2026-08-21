package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
 * @see fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.IncludeVariableCollector
 * @see fr.adrienbrault.idea.symfony2plugin.twig.variable.collector.ControllerVariableCollector
 */
public class TwigVariableContextTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ide-twig.json");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test that variables are NOT inherited when include uses 'only' keyword.
     */
    public void testIncludeWithOnlyBlocksParentContext() {
        myFixture.addFileToProject(
            "templates/parent_with_only.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{# @var title string #}\n" +
            "{% include 'partials/_isolated.html.twig' only %}\n"
        );

        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "{# Template: partials/_isolated.html.twig #}\n" +
            "{{ <caret> }}",
            "user",
            "title"
        );
    }

    public void testTagIncludeDynamicTargetsInheritParentVariablesForEveryResolvedTemplate() {
        myFixture.addFileToProject(
            "templates/parent_ternary_include.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include ajax ? 'ajax.html.twig' : 'not_ajax.html.twig' %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_ternary_ignore_missing_include.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include ajax ? 'ajax_ignore_missing.html.twig' : 'not_ajax_ignore_missing.html.twig' ignore missing %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_ternary_ignore_missing_only_include.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include ajax ? 'ajax_ignore_missing_only.html.twig' : 'not_ajax_ignore_missing_only.html.twig' ignore missing only %}\n"
        );

        assertPathScopeContains("templates/ajax.html.twig", "{{ <caret> }}", "user");
        assertPathScopeContains("templates/not_ajax.html.twig", "{{ <caret> }}", "user");
        assertPathScopeContains("templates/ajax_ignore_missing.html.twig", "{{ <caret> }}", "user");
        assertPathScopeContains("templates/not_ajax_ignore_missing.html.twig", "{{ <caret> }}", "user");
        assertPathScopeNotContains("templates/ajax_ignore_missing_only.html.twig", "{{ <caret> }}", "user");
        assertPathScopeNotContains("templates/not_ajax_ignore_missing_only.html.twig", "{{ <caret> }}", "user");
    }

    public void testTagIncludeArrayTargetsInheritParentVariablesForEveryResolvedTemplate() {
        myFixture.addFileToProject(
            "templates/parent_array_include.html.twig",
            "{# @var page \\App\\Entity\\Page #}\n" +
            "{% include ['page_detailed.html.twig', 'page.html.twig'] %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_array_ignore_missing_include.html.twig",
            "{# @var page \\App\\Entity\\Page #}\n" +
            "{% include ['page_detailed_ignore_missing.html.twig', 'page_ignore_missing.html.twig'] ignore missing %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_array_ignore_missing_only_include.html.twig",
            "{# @var page \\App\\Entity\\Page #}\n" +
            "{% include ['page_detailed_ignore_missing_only.html.twig', 'page_ignore_missing_only.html.twig'] ignore missing only %}\n"
        );

        assertPathScopeContains("templates/page_detailed.html.twig", "{{ <caret> }}", "page");
        assertPathScopeContains("templates/page.html.twig", "{{ <caret> }}", "page");
        assertPathScopeContains("templates/page_detailed_ignore_missing.html.twig", "{{ <caret> }}", "page");
        assertPathScopeContains("templates/page_ignore_missing.html.twig", "{{ <caret> }}", "page");
        assertPathScopeNotContains("templates/page_detailed_ignore_missing_only.html.twig", "{{ <caret> }}", "page");
        assertPathScopeNotContains("templates/page_ignore_missing_only.html.twig", "{{ <caret> }}", "page");
    }

    public void testTagIncludeIgnoreMissingAndOnlyContextVariants() {
        myFixture.addFileToProject(
            "templates/parent_only_without_with.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{# @var title string #}\n" +
            "{# no variables will be accessible #}\n" +
            "{% include 'template_only.html.twig' only %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_ignore_missing.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include 'sidebar_ignore_missing.html.twig' ignore missing %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_ignore_missing_with.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include 'sidebar_ignore_missing_with.html.twig' ignore missing with {'name': 'Fabien'} %}\n"
        );

        myFixture.addFileToProject(
            "templates/parent_ignore_missing_only.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{% include 'sidebar_ignore_missing_only.html.twig' ignore missing only %}\n"
        );

        assertPathScopeNotContains("templates/template_only.html.twig", "{{ <caret> }}", "user", "title");
        assertPathScopeContains("templates/sidebar_ignore_missing.html.twig", "{{ <caret> }}", "user");
        assertPathScopeContains("templates/sidebar_ignore_missing_with.html.twig", "{{ <caret> }}", "user", "name");
        assertPathScopeNotContains("templates/sidebar_ignore_missing_only.html.twig", "{{ <caret> }}", "user");
    }

    /**
     * Test function-style include with with_context = false.
     */
    public void testIncludeFunctionWithContextFalse() {
        myFixture.addFileToProject(
            "templates/parent_func.html.twig",
            "{# @var user \\App\\Entity\\User #}\n" +
            "{{ include('partials/_widget.html.twig', {}, with_context = false) }}\n"
        );

        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "{# Template: partials/_widget.html.twig #}\n" +
            "{{ <caret> }}",
            "user"
        );
    }

    /**
     * Test that embed with 'only' blocks parent context.
     */
    public void testEmbedWithOnlyBlocksParentContext() {
        myFixture.addFileToProject(
            "templates/parent_embed_only.html.twig",
            "{# @var article \\App\\Entity\\Article #}\n" +
            "{% embed 'partials/_isolated_card.html.twig' only %}\n" +
            "    {% block content %}{% endblock %}\n" +
            "{% endembed %}\n"
        );

        assertCompletionNotContains(
            TwigFileType.INSTANCE,
            "{# Template: partials/_isolated_card.html.twig #}\n" +
            "{% block content %}{{ <caret> }}{% endblock %}",
            "article"
        );
    }

    public void testTagIncludeWithLiteralParameterProvidesChildVariable() {
        myFixture.addFileToProject(
            "templates/parent_literal.html.twig",
            "{% include 'partials/_card.html.twig' with {'title': 'Example'} %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            "title"
        );
    }

    public void testTagIncludeOnlyKeepsExplicitLiteralParameter() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_literal_only.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{% include 'partials/_card.html.twig' with {'title': 'Example'} only %}\n"
        );

        assertPathCompletionContainsAndNotContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            new String[] {"title"},
            new String[] {"item"}
        );
    }

    public void testTagIncludeOnlyKeepsAliasWithoutParentContext() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_alias_only.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{% include 'partials/_card.html.twig' with {itemAlias: item} only %}\n"
        );

        assertPathCompletionContainsAndNotContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            new String[] {"itemAlias"},
            new String[] {"item"}
        );
    }

    public void testFunctionIncludeWithLiteralParameterProvidesChildVariable() {
        myFixture.addFileToProject(
            "templates/parent_function_literal.html.twig",
            "{{ include('partials/_card.html.twig', {title: 'Example'}) }}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            "title"
        );
    }

    public void testFunctionIncludeWithContextFalseKeepsExplicitLiteralParameter() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_literal_isolated.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{{ include('partials/_card.html.twig', {title: 'Example'}, with_context: false) }}\n"
        );

        assertPathCompletionContainsAndNotContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            new String[] {"title"},
            new String[] {"item"}
        );
    }

    public void testFunctionIncludeWithContextFalseWithoutParametersBlocksParentContext() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_without_params.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{{ include('partials/_card.html.twig', with_context: false) }}\n"
        );

        assertPathCompletionNotContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            "item"
        );
    }

    public void testFunctionIncludeSupportsMixedParameters() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_mixed.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{# @var itemCollection \\App\\Entity\\Product[] #}\n" +
            "{{ include('partials/_card.html.twig', {\n" +
            "    item: item,\n" +
            "    itemAlias: item,\n" +
            "    itemCollection: itemCollection,\n" +
            "    currentView: 'current'\n" +
            "}) }}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            "item",
            "itemAlias",
            "itemCollection",
            "currentView"
        );
    }

    /**
     * Function include keeps complex explicit values visible as untyped child variables.
     */
    public void testFunctionIncludeSupportsComplexParameterValues() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_complex_values.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{{ include('partials/_card.html.twig', {\n" +
            "    product: item,\n" +
            "    title: 'Current ' ~ item.name,\n" +
            "    options: {label: item.name, flags: {'active': true}}\n" +
            "}, with_context: false) }}\n"
        );

        assertPathCompletionContainsAndNotContains(
            "templates/partials/_card.html.twig",
            "{{ <caret> }}",
            new String[] {"product", "title", "options"},
            new String[] {"item"}
        );
    }

    public void testTagIncludeAliasKeepsParentVariableType() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_tag_alias_type.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{% include 'partials/_card.html.twig' with {'product': item} %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ product.<caret> }}",
            "name",
            "sku"
        );
    }

    public void testFunctionIncludeAliasKeepsParentVariableType() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_alias_type.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{{ include('partials/_card.html.twig', {product: item}) }}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ product.<caret> }}",
            "name",
            "sku"
        );
    }

    public void testFunctionIncludePropertyPathKeepsResolvedType() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_property_path.html.twig",
            "{# @var item \\App\\Entity\\OrderItem #}\n" +
            "{{ include('partials/_card.html.twig', {product: item.product}) }}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ product.<caret> }}",
            "name",
            "sku"
        );
    }

    public void testTagIncludePropertyPathKeepsResolvedType() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_tag_property_path.html.twig",
            "{# @var item \\App\\Entity\\OrderItem #}\n" +
            "{% include 'partials/_card.html.twig' with {product: item.product} %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_card.html.twig",
            "{{ product.<caret> }}",
            "name",
            "sku"
        );
    }

    public void testTagIncludeAliasTypeNavigatesToPhpMethod() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_tag_alias_navigation.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{% include 'partials/_card.html.twig' with {'product': item} %}\n"
        );

        assertPathNavigationMatch(
            "templates/partials/_card.html.twig",
            "{{ product.getNa<caret>me() }}",
            PlatformPatterns.psiElement(Method.class).withName("getName")
        );
    }

    public void testFunctionIncludeAliasTypeNavigatesToPhpMethod() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_alias_navigation.html.twig",
            "{# @var item \\App\\Entity\\Product #}\n" +
            "{{ include('partials/_card.html.twig', {'product': item}) }}\n"
        );

        assertPathNavigationMatch(
            "templates/partials/_card.html.twig",
            "{{ product.getSk<caret>u() }}",
            PlatformPatterns.psiElement(Method.class).withName("getSku")
        );
    }

    public void testFunctionIncludePropertyPathTypeNavigatesToPhpMethod() {
        addProductFixture();
        myFixture.addFileToProject(
            "templates/parent_function_property_navigation.html.twig",
            "{# @var item \\App\\Entity\\OrderItem #}\n" +
            "{{ include('partials/_card.html.twig', {product: item.product}) }}\n"
        );

        assertPathNavigationMatch(
            "templates/partials/_card.html.twig",
            "{{ product.getNa<caret>me() }}",
            PlatformPatterns.psiElement(Method.class).withName("getName")
        );
    }

    /**
     * Test that block-scoped variables are collected correctly.
     */
    public void testBlockScopeVariableCollection() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var global_var \\App\\Entity\\Global #}\n" +
            "{% block outer %}\n" +
            "    {# @var block_var \\App\\Entity\\Block #}\n" +
            "    {% block inner %}\n" +
            "        {{ <caret> }}\n" +
            "    {% endblock %}\n" +
            "{% endblock %}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertContainsElements(vars.keySet(), "global_var", "block_var");
    }

    /**
     * Test that file-level docblock variables are collected.
     */
    public void testFileDocBlockVariableCollection() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var user \\App\\Entity\\User #}\n" +
            "{# @var product \\App\\Entity\\Product #}\n" +
            "{{ <caret> }}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertContainsElements(vars.keySet(), "user", "product");
    }

    /**
     * Test that deprecated inline variable syntax is still supported.
     */
    public void testDeprecatedInlineVariableSyntax() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# user \\App\\Entity\\User #}\n" +
            "{# title string #}\n" +
            "{{ <caret> }}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertContainsElements(vars.keySet(), "user", "title");
    }

    /**
     * Test variable completion in same file with docblock.
     */
    public void testVariableCompletionWithDocBlock() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var userName \\App\\Entity\\User #}\n" +
            "{# @var pageTitle string #}\n" +
            "{{ <caret> }}",
            "userName",
            "pageTitle"
        );
    }

    /**
     * Test method completion on typed variable.
     */
    public void testMethodCompletionOnTypedVariable() {
        myFixture.addFileToProject("src/Entity/Order.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Order\n" +
            "{\n" +
            "    public function getId(): int {}\n" +
            "    public function getStatus(): string {}\n" +
            "    public function getTotal(): float {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var order \\App\\Entity\\Order #}\n" +
            "{{ order.<caret> }}",
            "id",
            "status",
            "total"
        );
    }

    /**
     * Test navigation from variable to class.
     */
    public void testVariableTypeNavigation() {
        myFixture.addFileToProject("src/Service/Calculator.php", "<?php\n" +
            "namespace App\\Service;\n" +
            "\n" +
            "class Calculator\n" +
            "{\n" +
            "    public function add(int $a, int $b): int {}\n" +
            "}\n"
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{# @var calc \\App\\Service\\Calculator #}\n" +
            "{{ cal<caret>c.add(1, 2) }}",
            PlatformPatterns.psiElement(PhpClass.class).withName("Calculator")
        );
    }

    /**
     * Test navigation from method call to PHP method.
     */
    public void testMethodNavigationOnVariable() {
        myFixture.addFileToProject("src/Entity/Customer.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Customer\n" +
            "{\n" +
            "    public function getFullName(): string {}\n" +
            "    public function getEmail(): string {}\n" +
            "}\n"
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{# @var customer \\App\\Entity\\Customer #}\n" +
            "{{ customer.getFullNa<caret>me() }}",
            PlatformPatterns.psiElement(Method.class).withName("getFullName")
        );
    }

    /**
     * Test array type variable completion in for loop.
     */
    public void testArrayTypeVariableCompletion() {
        myFixture.addFileToProject("src/Entity/Item.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Item\n" +
            "{\n" +
            "    public function getName(): string {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var items \\App\\Entity\\Item[] #}\n" +
            "{% for item in items %}\n" +
            "    {{ item.<caret> }}\n" +
            "{% endfor %}",
            "name"
        );
    }

    /**
     * Test that multiple docblocks for same variable merge types.
     */
    public void testMultipleDocBlockMerging() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var data \\App\\Entity\\Base #}\n" +
            "{% block content %}\n" +
            "    {# @var data \\App\\Entity\\Extended #}\n" +
            "    {{ <caret> }}\n" +
            "{% endblock %}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertTrue("Variable should exist", vars.containsKey("data"));
        assertTrue("Should have merged types", vars.get("data").getTypes().size() >= 1);
    }

    /**
     * Test for loop with key variable.
     */
    public void testForLoopWithKeyVariable() {
        myFixture.addFileToProject("src/Entity/Category.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Category\n" +
            "{\n" +
            "    public function getName(): string {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var categories \\App\\Entity\\Category[] #}\n" +
            "{% for key, category in categories %}\n" +
            "    {{ category.<caret> }}\n" +
            "{% endfor %}",
            "name"
        );
    }

    /**
     * Test nested for loops with typed arrays.
     */
    public void testNestedForLoopVariableCompletion() {
        myFixture.addFileToProject("src/Entity/OrderItem.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class OrderItem\n" +
            "{\n" +
            "    public function getProductName(): string {}\n" +
            "    public function getQuantity(): int {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var items \\App\\Entity\\OrderItem[] #}\n" +
            "{% for order in orders %}\n" +
            "    {% for item in items %}\n" +
            "        {{ item.<caret> }}\n" +
            "    {% endfor %}\n" +
            "{% endfor %}",
            "productName",
            "quantity"
        );
    }

    /**
     * Test for loop method navigation.
     */
    public void testForLoopMethodNavigation() {
        myFixture.addFileToProject("src/Entity/Report.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Report\n" +
            "{\n" +
            "    public function getTitle(): string {}\n" +
            "    public function getSummary(): string {}\n" +
            "}\n"
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{# @var reports \\App\\Entity\\Report[] #}\n" +
            "{% for report in reports %}\n" +
            "    {{ report.getTit<caret>le() }}\n" +
            "{% endfor %}",
            PlatformPatterns.psiElement(Method.class).withName("getTitle")
        );
    }

    /**
     * Test embed with variable context - parent variables available inside embed override blocks.
     */
    public void testEmbedVariableContextInOverrideBlock() {
        myFixture.addFileToProject("src/Entity/News.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class News\n" +
            "{\n" +
            "    public function getHeadline(): string {}\n" +
            "    public function getBody(): string {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var news \\App\\Entity\\News #}\n" +
            "{% embed 'components/card.html.twig' %}\n" +
            "    {% block header %}\n" +
            "        {{ news.<caret> }}\n" +
            "    {% endblock %}\n" +
            "{% endembed %}",
            "headline",
            "body"
        );
    }

    /**
     * Test for else block variable scope.
     */
    public void testForElseBlockVariableScope() {
        myFixture.addFileToProject("src/Entity/Message.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Message\n" +
            "{\n" +
            "    public function getText(): string {}\n" +
            "}\n"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var messages \\App\\Entity\\Message[] #}\n" +
            "{% for message in messages %}\n" +
            "    {{ message.<caret> }}\n" +
            "{% else %}\n" +
            "    No messages\n" +
            "{% endfor %}",
            "text"
        );
    }

    /**
     * Test that embed inherits parent variables in override blocks within same file.
     */
    public void testEmbedInheritsParentVariables() {
        myFixture.addFileToProject("src/Entity/Widget.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Widget\n" +
            "{\n" +
            "    public function getTitle(): string {}\n" +
            "    public function getContent(): string {}\n" +
            "}\n"
        );

        // Variables should be available in embed override block in same file
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{# @var widget \\App\\Entity\\Widget #}\n" +
            "{% embed 'widget/_panel.html.twig' %}\n" +
            "    {% block body %}\n" +
            "        {{ widget.<caret> }}\n" +
            "    {% endblock %}\n" +
            "{% endembed %}",
            "title",
            "content"
        );
    }

    /**
     * Helper for completion tests that need files at specific paths.
     * Creates file at path, configures editor, runs completion, and checks results.
     */
    private void assertPathCompletionContains(String path, String content, String... expected) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.completeBasic();
        assertContainsElements(myFixture.getLookupElementStrings(), expected);
    }

    /**
     * Creates a Twig file at a template path and asserts root completion exclusions.
     */
    private void assertPathCompletionNotContains(String path, String content, String... unexpected) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.completeBasic();
        assertCompletionResultsDoNotContain(unexpected);
    }

    /**
     * Creates a Twig file at a template path and checks include-visible and isolated variables.
     */
    private void assertPathCompletionContainsAndNotContains(String path, String content, String[] expected, String[] unexpected) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.completeBasic();
        assertContainsElements(myFixture.getLookupElementStrings(), expected);
        assertCompletionResultsDoNotContain(unexpected);
    }

    /**
     * Creates a Twig file at a template path and asserts variables collected for its current scope.
     */
    private void assertPathScopeContains(String path, String content, String... expected) {
        assertContainsElements(collectPathScopeVariables(path, content).keySet(), expected);
    }

    /**
     * Creates a Twig file at a template path and asserts variables are absent from its current scope.
     */
    private void assertPathScopeNotContains(String path, String content, String... unexpected) {
        Map<String, PsiVariable> vars = collectPathScopeVariables(path, content);
        for (String item: unexpected) {
            assertFalse("Scope should not contain: " + item, vars.containsKey(item));
        }
    }

    /**
     * Creates a Twig file at a template path and returns variables collected for its current scope.
     */
    @NotNull
    private Map<String, PsiVariable> collectPathScopeVariables(String path, String content) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        return TwigTypeResolveUtil.collectScopeVariables(psiElement);
    }

    /**
     * Asserts that current lookup results do not contain specific variable names.
     */
    private void assertCompletionResultsDoNotContain(String... unexpected) {
        List<String> lookupElementStrings = myFixture.getLookupElementStrings();
        if (lookupElementStrings == null) {
            return;
        }

        for (String item: unexpected) {
            assertFalse("Completion should not contain: " + item, lookupElementStrings.contains(item));
        }
    }

    /**
     * Creates a Twig file at a template path and asserts goto target matching.
     */
    private void assertPathNavigationMatch(String path, String content, ElementPattern<?> pattern) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        assertNavigationMatch(pattern);
    }

    /**
     * Adds PHP classes used for include alias type completion and navigation tests.
     */
    private void addProductFixture() {
        myFixture.addFileToProject("src/Entity/Product.php", "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "class Product\n" +
            "{\n" +
            "    public function getName(): string {}\n" +
            "    public function getSku(): string {}\n" +
            "}\n" +
            "\n" +
            "class OrderItem\n" +
            "{\n" +
            "    public function getProduct(): Product {}\n" +
            "}\n"
        );
    }

    /**
     * Test that included template inherits parent template variables via TwigIncludeStubIndex.
     * Uses VFS fallback pattern from commit fadd9bf2 - files at proper paths enable template name resolution.
     */
    public void testIncludeInheritsParentVariablesViaIndex() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");

        // Create parent template that includes child with typed variables
        myFixture.addFileToProject(
            "templates/dashboard/main.html.twig",
            "{# @var user \\Foo\\Template\\Foobar #}\n" +
            "{% include 'dashboard/_stats.html.twig' %}\n"
        );

        assertPathCompletionContains(
            "templates/dashboard/_stats.html.twig",
            "{{ <caret> }}",
            "user"
        );
    }

    /**
     * Test that template rendered by controller inherits controller variables.
     * Uses VFS fallback - template at templates/invoice/view.html.twig matches render() call.
     */
    public void testControllerRenderVariableInheritanceViaIndex() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");

        myFixture.addFileToProject(
            "src/Controller/InvoiceController.php",
            "<?php\n" +
            "class InvoiceController\n" +
            "{\n" +
            "    public function viewAction()\n" +
            "    {\n" +
            "        $foobar = new \\Foo\\Template\\Foobar();\n" +
            "        return $this->render('invoice/view.html.twig', [\n" +
            "            'myInvoice' => $foobar,\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n"
        );

        assertPathCompletionContains(
            "templates/invoice/view.html.twig",
            "{{ <caret> }}",
            "myInvoice"
        );
    }

    /**
     * Test that template rendered by controller inherits variables from spread context arrays.
     */
    public void testControllerRenderVariableInheritanceWithArraySpreadViaIndex() {
        myFixture.copyFileToProject("ide-twig.json");

        myFixture.addFileToProject(
            "src/Controller/ProductController.php",
            "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function showAction()\n" +
            "    {\n" +
            "        $item = new \\App\\Entity\\Product();\n" +
            "        $baseData = [\n" +
            "            'item' => $item,\n" +
            "        ];\n" +
            "\n" +
            "        return $this->render('product/show.html.twig', [\n" +
            "            'headline' => 'Product',\n" +
            "            ...$baseData,\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "namespace App\\Entity;\n" +
            "class Product\n" +
            "{\n" +
            "    public function getName(): string {}\n" +
            "}\n"
        );

        assertPathCompletionContains(
            "templates/product/show.html.twig",
            "{{ <caret> }}",
            "headline",
            "item"
        );
    }

    public void testControllerRenderVariableInheritanceDoesNotLeakBetweenTemplatesInSameMethod() {
        myFixture.copyFileToProject("ide-twig.json");

        myFixture.addFileToProject(
            "src/Controller/ProductController.php",
            "<?php\n" +
            "class ProductController\n" +
            "{\n" +
            "    public function showAction()\n" +
            "    {\n" +
            "        $product = new \\Foo\\Template\\Foobar();\n" +
            "        $form = new \\Foo\\Template\\Foobar();\n" +
            "\n" +
            "        if ($edit) {\n" +
            "            return $this->render('product/edit.html.twig', [\n" +
            "                'editForm' => $form,\n" +
            "            ]);\n" +
            "        }\n" +
            "\n" +
            "        return $this->render('product/show.html.twig', [\n" +
            "            'product' => $product,\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n"
        );

        assertPathCompletionContainsAndNotContains(
            "templates/product/show.html.twig",
            "{{ <caret> }}",
            new String[] {"product"},
            new String[] {"editForm"}
        );

        assertPathCompletionContainsAndNotContains(
            "templates/product/edit.html.twig",
            "{{ <caret> }}",
            new String[] {"editForm"},
            new String[] {"product"}
        );
    }

    /**
     * Test that @Template annotation provides variable context.
     */
    public void testTemplateAnnotationVariableInheritanceViaIndex() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");

        myFixture.addFileToProject(
            "src/Controller/ArticleController.php",
            "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class ArticleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template(\"article/detail.html.twig\")\n" +
            "     */\n" +
            "    public function detailAction()\n" +
            "    {\n" +
            "        $foobar = new \\Foo\\Template\\Foobar();\n" +
            "        return [\n" +
            "            'myArticle' => $foobar,\n" +
            "        ];\n" +
            "    }\n" +
            "}\n"
        );

        assertPathCompletionContains(
            "templates/article/detail.html.twig",
            "{{ <caret> }}",
            "myArticle"
        );
    }

    /**
     * Test controller variable type resolution for method completion.
     */
    public void testControllerVariableMethodCompletionInTemplateViaIndex() {
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");

        myFixture.addFileToProject(
            "src/Controller/SubscriptionController.php",
            "<?php\n" +
            "class SubscriptionController\n" +
            "{\n" +
            "    public function statusAction()\n" +
            "    {\n" +
            "        $foobar = new \\Foo\\Template\\Foobar();\n" +
            "        return $this->render('subscription/status.html.twig', [\n" +
            "            'mySubscription' => $foobar,\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n"
        );

        assertPathCompletionContains(
            "templates/subscription/status.html.twig",
            "{{ mySubscription.<caret> }}",
            "ready", "readyStatus"
        );
    }

    public void testControllerVariableForLoopCompletionUsesPromotedConstructorParamArrayType() {
        myFixture.copyFileToProject("ide-twig.json");

        myFixture.addFileToProject(
            "src/Controller/FooController.php",
            "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class FooController\n" +
            "{\n" +
            "    public function fooAction()\n" +
            "    {\n" +
            "        return $this->render('foo/index.html.twig', [\n" +
            "            'foo' => new \\Foo\\Dto\\FooDto(new \\Foo\\Dto\\FoobarDto([])),\n" +
            "        ]);\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "namespace Foo\\Dto;\n" +
            "class FooDto {\n" +
            "  public function __construct(public FoobarDto $foobar) {}\n" +
            "}\n" +
            "class FoobarDto {\n" +
            "  /**\n" +
            "   * @param BarDto[] $bars\n" +
            "   */\n" +
            "  public function __construct(public array $bars) {}\n" +
            "}\n" +
            "class BarDto {\n" +
            "  public function getBaz() {}\n" +
            "}\n"
        );

        assertPathCompletionContains(
            "templates/foo/index.html.twig",
            "{% for bar in foo.foobar.bars %}\n" +
            "  {{ bar.<caret> }}\n" +
            "{% endfor %}",
            "baz"
        );
    }
}
