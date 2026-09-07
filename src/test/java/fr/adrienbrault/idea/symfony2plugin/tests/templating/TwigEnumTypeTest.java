package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class TwigEnumTypeTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("Status.php", """
            <?php
            namespace Example;
            enum Status: string {
                case Active = 'active';
                case Inactive = 'inactive';
                public function label(): string { return $this->value; }
                public function metadata(): Metadata { return new Metadata(); }
            }
            class Metadata { public function description(): string { return ''; } }
            class NotAnEnum { public const Active = 'active'; }
            """);
    }

    public void testEnumFunctionCompletesCases() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('Example\\\\Status').<caret> }}", "Active", "Inactive");
        assertCompletionContains(TwigFileType.INSTANCE, "{{ render_status(enum('Example\\\\Status').<caret>, format: 'text') }}", "Active");
        assertCompletionContains(TwigFileType.INSTANCE, "{% if enum(\"\\\\Example\\\\Status\").<caret> %}{% endif %}", "Active");
    }

    public void testEnumCaseNavigationInsideFunctionArgument() {
        assertNavigationMatch(TwigFileType.INSTANCE,
            "{{ render_status(enum('Example\\\\Status').Ac<caret>tive, format: 'text') }}",
            PlatformPatterns.psiElement(PhpEnumCase.class).withName("Active"));
    }

    public void testEnumCaseRetainsItsTypeInMemberChains() {
        assertCompletionContains(TwigFileType.INSTANCE, "{{ enum('Example\\\\Status').Active.<caret> }}", "label", "metadata");
        assertNavigationMatch(TwigFileType.INSTANCE,
            "{{ enum('Example\\\\Status').Active.metadata().descr<caret>iption() }}",
            PlatformPatterns.psiElement(Method.class).withName("description"));
    }

    public void testEnumCasesAreInspected() {
        assertLocalInspectionNotContains("f.html.twig",
            "{{ render_status(enum('Example\\\\Status').Ac<caret>tive, format: 'text') }}",
            "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{{ enum('Example\\\\Status').Un<caret>known }}", "Field or method not found");
        assertLocalInspectionContains("f.html.twig", "{{ enum('Example\\\\Status').ac<caret>tive }}", "Field or method not found");
    }

    public void testInvalidOrDynamicArgumentsDoNotResolveAsEnums() {
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('Example\\\\NotAnEnum').<caret> }}", "Active");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('Example\\\\Missing').<caret> }}", "Active");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum('Example\\\\Status' ~ suffix).<caret> }}", "Active");
        assertCompletionNotContains(TwigFileType.INSTANCE, "{{ enum(className).<caret> }}", "Active");
    }
}
