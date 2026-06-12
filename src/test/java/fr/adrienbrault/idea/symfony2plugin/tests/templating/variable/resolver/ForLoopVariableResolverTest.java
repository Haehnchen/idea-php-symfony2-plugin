package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable.resolver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.ForLoopVariableResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ForLoopVariableResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ide-twig.json");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testResolveProvidesStaticLoopVariablesWhenLoopContextClassIsMissing() {
        Collection<TwigTypeContainer> targets = new ArrayList<>();
        Collection<TwigTypeContainer> previousElement = TwigTypeContainer.fromCollection(Collections.singleton(new PsiVariable(ForLoopVariableResolver.LOOP_VARIABLE_TYPE)));

        new ForLoopVariableResolver().resolve(getProject(), targets, previousElement, "loop", new ArrayList<>(), null);

        Set<String> loopVariables = targets.stream().map(TwigTypeContainer::getStringElement).collect(Collectors.toSet());
        assertContainsElements(loopVariables, ForLoopVariableResolver.TWIG3_LOOP_VARIABLES);
        assertDoesntContain(loopVariables, "previous", "next", "changed", "cycle", "depth", "depth0");
    }

    public void testResolveExtractsTwig4LoopContextMembersFromPhpClass() {
        addTwig4LoopContextFixture("    public function getFixtureOnly(): string {}\n");

        Collection<String> loopVariables = ForLoopVariableResolver.getLoopVariables(getProject());

        assertContainsElements(loopVariables, "index", "index0", "first", "last", "changed", "previous", "next", "cycle", "depth", "depth0", "fixtureOnly");
    }

    public void testResolveIgnoresNonTwigAccessibleLoopContextMethods() {
        addTwig4LoopContextFixture(
            "    private function getPrivateValue(): string {}\n" +
            "    public function setIgnored(string $value): void {}\n" +
            "    public function __invoke($iterator): iterable {}\n"
        );

        Collection<String> loopVariables = ForLoopVariableResolver.getLoopVariables(getProject());

        assertDoesntContain(loopVariables, "privateValue", "ignored", "__invoke");
    }

    public void testResolveIgnoresNonLoopVariableType() {
        Collection<TwigTypeContainer> targets = new ArrayList<>();
        Collection<TwigTypeContainer> previousElement = TwigTypeContainer.fromCollection(Collections.singleton(new PsiVariable("\\App\\Entity\\Entry")));

        new ForLoopVariableResolver().resolve(getProject(), targets, previousElement, "loop", new ArrayList<>(), null);

        assertEmpty(targets);
    }

    /**
     * @see TwigTypeResolveUtil#collectScopeVariables
     */
    public void testCollectScopeVariablesProvidesLoopVariableInsideForTag() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{% for entry in entries %}{{ <caret> }}{% endfor %}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Map<String, PsiVariable> variables = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertContainsElements(variables.keySet(), "loop");
        assertContainsElements(variables.get("loop").getTypes(), ForLoopVariableResolver.LOOP_VARIABLE_TYPE);
    }

    public void testResolveTwigMethodNameProvidesLoopVariablesInsideForTag() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{% for entry in entries %}{{ loop.i<caret> }}{% endfor %}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(psiElement);
        Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(psiElement, beforeLeaf);

        assertContainsElements(types.stream().map(TwigTypeContainer::getStringElement).collect(Collectors.toSet()), ForLoopVariableResolver.TWIG3_LOOP_VARIABLES);
    }

    public void testForLoopProvidesLoopVariableCompletion() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% for entry in entries %}{{ <caret> }}{% endfor %}",
            "loop"
        );

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% for entry in entries %}{{ loop.<caret> }}{% endfor %}",
            "index", "index0", "revindex", "revindex0", "first", "last", "length", "parent"
        );
    }

    public void testForLoopProvidesLoopVariableCompletionInsideBlock() {
        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% block content %}{% for entry in entries %}{{ loop.<caret> }}{% endfor %}{% endblock %}",
            "index", "index0", "revindex", "revindex0", "first", "last", "length", "parent"
        );
    }

    public void testForLoopProvidesTwig4LoopVariableCompletion() {
        addTwig4LoopContextFixture("");

        assertCompletionContains(
            TwigFileType.INSTANCE,
            "{% for entry in entries %}{{ loop.<caret> }}{% endfor %}",
            "index", "index0", "first", "last", "previous", "next", "changed", "cycle", "depth", "depth0"
        );
    }

    public void testTagIncludeInsideForLoopInheritsLoopVariable() {
        myFixture.addFileToProject(
            "templates/parent_loop_include.html.twig",
            "{% for entry in entries %}\n" +
            "    {% include 'partials/_loop.html.twig' %}\n" +
            "    {% include 'partials/_loop_properties.html.twig' %}\n" +
            "{% endfor %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_loop.html.twig",
            "{{ <caret> }}",
            "loop"
        );

        assertPathCompletionContains(
            "templates/partials/_loop_properties.html.twig",
            "{{ loop.<caret> }}",
            "index", "index0", "revindex", "revindex0", "first", "last", "length", "parent"
        );
    }

    public void testTagIncludeInsideForLoopInheritsTwig4LoopVariableMembers() {
        addTwig4LoopContextFixture("");
        myFixture.addFileToProject(
            "templates/parent_twig4_loop_include.html.twig",
            "{% for entry in entries %}\n" +
            "    {% include 'partials/_twig4_loop_properties.html.twig' %}\n" +
            "{% endfor %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_twig4_loop_properties.html.twig",
            "{{ loop.<caret> }}",
            "index", "index0", "first", "last", "previous", "next", "changed", "cycle", "depth", "depth0"
        );
    }

    public void testFunctionIncludeInsideForLoopInheritsLoopVariable() {
        myFixture.addFileToProject(
            "templates/parent_loop_function_include.html.twig",
            "{% for entry in entries %}\n" +
            "    {{ include('partials/_function_loop.html.twig') }}\n" +
            "{% endfor %}\n"
        );

        assertPathCompletionContains(
            "templates/partials/_function_loop.html.twig",
            "{{ loop.<caret> }}",
            "index", "index0", "revindex", "revindex0", "first", "last", "length", "parent"
        );
    }

    public void testCollectorBridgeProvidesSyntheticLoopTypeProperties() {
        addTwig4LoopContextFixture("    public function getFixtureOnly(): string {}\n");

        List<String> properties = new TwigTemplateVariablesCollector(getProject()).collectPropertiesForTypes(Collections.singleton(ForLoopVariableResolver.LOOP_VARIABLE_TYPE));

        assertContainsElements(properties, "index", "previous", "next", "changed", "cycle", "depth", "depth0", "fixtureOnly");
    }

    public void testIsolatedIncludeInsideForLoopDoesNotInheritLoopVariable() {
        myFixture.addFileToProject(
            "templates/parent_loop_isolated_include.html.twig",
            "{% for entry in entries %}\n" +
            "    {% include 'partials/_isolated_loop.html.twig' only %}\n" +
            "    {{ include('partials/_isolated_function_loop.html.twig', with_context: false) }}\n" +
            "{% endfor %}\n"
        );

        assertPathCompletionNotContains(
            "templates/partials/_isolated_loop.html.twig",
            "{{ <caret> }}",
            "loop"
        );

        assertPathCompletionNotContains(
            "templates/partials/_isolated_function_loop.html.twig",
            "{{ <caret> }}",
            "loop"
        );
    }

    private void assertPathCompletionContains(String path, String content, String... expected) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.completeBasic();
        assertContainsElements(myFixture.getLookupElementStrings(), expected);
    }

    private void assertPathCompletionNotContains(String path, String content, String... unexpected) {
        PsiFile file = myFixture.addFileToProject(path, content);
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.completeBasic();
        List<String> lookupElementStrings = myFixture.getLookupElementStrings();
        if (lookupElementStrings == null) {
            return;
        }

        for (String item: unexpected) {
            assertFalse("Completion should not contain: " + item, lookupElementStrings.contains(item));
        }
    }

    private void addTwig4LoopContextFixture(String extraMethods) {
        myFixture.addFileToProject(
            "vendor/twig/twig/src/Runtime/LoopContext.php",
            "<?php\n" +
            "\n" +
            "namespace Twig\\Runtime;\n" +
            "\n" +
            "final class LoopContext\n" +
            "{\n" +
            "    public function getParent(): mixed {}\n" +
            "    public function getRevindex0(): int {}\n" +
            "    public function getRevindex(): int {}\n" +
            "    public function getIndex0(): int {}\n" +
            "    public function getIndex(): int {}\n" +
            "    public function getLength(): int {}\n" +
            "    public function isFirst(): bool {}\n" +
            "    public function isLast(): bool {}\n" +
            "    public function hasChanged(mixed $value): bool {}\n" +
            "    public function getPrevious(): mixed {}\n" +
            "    public function getNext(): mixed {}\n" +
            "    public function cycle($value, ...$values): mixed {}\n" +
            "    public function getDepth0(): int {}\n" +
            "    public function getDepth(): int {}\n" +
            extraMethods +
            "}\n"
        );
    }
}
