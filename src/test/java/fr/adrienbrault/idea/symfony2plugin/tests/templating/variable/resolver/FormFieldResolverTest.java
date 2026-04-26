package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable.resolver;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.TwigFormField;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormFieldResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testGetFormTypeFqnsFromFormFactory() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form { interface FormInterface {} interface FormView {} interface FormTypeInterface {} }\n" +
            "namespace Symfony\\Bundle\\FrameworkBundle\\Controller { class AbstractController { public function createForm($type) {} } }\n" +
            "namespace App\\Form { class ProductType implements \\Symfony\\Component\\Form\\FormTypeInterface {} }\n" +
            "namespace App\\Controller {\n" +
            "  class ProductController extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController {\n" +
            "    public function index() {\n" +
            "      $form = $this->createForm(\\App\\Form\\ProductType::class);\n" +
            "      return ['form' => $form->createView()];\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        );

        Set<String> formTypeFqns = FormFieldResolver.getFormTypeFqnsFromFormFactory(findMethodReference("createView"));

        assertContainsElements(formTypeFqns, "\\App\\Form\\ProductType");
    }

    public void testVisitFormFieldsProvidesPrimitiveFieldMetadata() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form { interface FormTypeInterface {} interface FormBuilderInterface { public function add(); } }\n" +
            "namespace Symfony\\Component\\Form\\Extension\\Core\\Type { class TextType implements \\Symfony\\Component\\Form\\FormTypeInterface {} }\n" +
            "namespace App\\Form {\n" +
            "  class ProductType implements \\Symfony\\Component\\Form\\FormTypeInterface {\n" +
            "    public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options) {\n" +
            "      $builder->add('title', \\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType::class);\n" +
            "      $builder->add('plain');\n" +
            "    }\n" +
            "  }\n" +
            "}\n"
        );

        Collection<TwigFormField> fields = new ArrayList<>();
        FormFieldResolver.visitFormFields(getProject(), Collections.singleton("\\App\\Form\\ProductType"), fields::add);

        Set<String> fieldNames = fields.stream().map(TwigFormField::name).collect(Collectors.toSet());
        assertContainsElements(fieldNames, "title", "plain");

        TwigFormField title = fields.stream()
            .filter(field -> "title".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertEquals("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", title.fieldTypeFqn());
        assertEquals("\\App\\Form\\ProductType", title.ownerFormTypeFqn());

        TwigFormField plain = fields.stream()
            .filter(field -> "plain".equals(field.name()))
            .findFirst()
            .orElseThrow();

        assertNull(plain.fieldTypeFqn());
        assertEquals("\\App\\Form\\ProductType", plain.ownerFormTypeFqn());
    }

    @NotNull
    private MethodReference findMethodReference(@NotNull String name) {
        for (PsiElement psiElement : PsiTreeUtil.collectElementsOfType(myFixture.getFile(), MethodReference.class)) {
            MethodReference methodReference = (MethodReference) psiElement;
            if (name.equals(methodReference.getName())) {
                return methodReference;
            }
        }

        fail("Method reference not found: " + name);
        throw new IllegalStateException(name);
    }
}
