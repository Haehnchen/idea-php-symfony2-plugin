package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable.resolver;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormVarsResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormVarsResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testResolveAttachesFormVarsForFormViewDataHolder() {
        configureFormViewVarsFixture();

        ArrayList<TwigTypeContainer> targets = new ArrayList<>();
        new FormVarsResolver().resolve(
            getProject(),
            targets,
            null,
            "vars",
            Collections.singletonList(Collections.singletonList(createRootFormViewContainer(true))),
            null
        );

        assertContainsElements(
            targets.stream().map(TwigTypeContainer::getStringElement).toList(),
            "compound",
            "form_attr"
        );
    }

    public void testResolveDoesNotUseFormViewClassWithoutFormViewDataHolder() {
        configureFormViewVarsFixture();

        ArrayList<TwigTypeContainer> targets = new ArrayList<>();
        new FormVarsResolver().resolve(
            getProject(),
            targets,
            null,
            "vars",
            Collections.singletonList(Collections.singletonList(createRootFormViewContainer(false))),
            null
        );

        assertEmpty(targets);
    }

    private void configureFormViewVarsFixture() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form {\n" +
            "  interface FormTypeInterface { public function getName(); }\n" +
            "  class FormView { public $vars = []; }\n" +
            "}\n" +
            "namespace Symfony\\Component\\Form\\Extension\\Core\\Type {\n" +
            "  class FormType implements \\Symfony\\Component\\Form\\FormTypeInterface {\n" +
            "    public function getName() { return 'form'; }\n" +
            "    public function buildView(\\Symfony\\Component\\Form\\FormView $view, $form, array $options) {\n" +
            "      $view->vars['form_attr'] = true;\n" +
            "      $view->vars = array_replace($view->vars, ['compound' => true]);\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        );
    }

    private TwigTypeContainer createRootFormViewContainer(boolean withFormViewDataHolder) {
        PhpClass phpClass = PhpElementsUtil.getClass(getProject(), "\\Symfony\\Component\\Form\\FormView");
        assertNotNull(phpClass);

        return new TwigTypeContainer(
            phpClass,
            withFormViewDataHolder ? new FormViewDataHolder(Collections.singleton("\\App\\Form\\ProductType")) : null
        );
    }
}
