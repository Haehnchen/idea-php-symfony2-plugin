package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUnderscoreMethodReference extends PsiPolyVariantReferenceBase<StringLiteralExpression> {
    @NotNull
    private final PhpClass phpClass;

    public FormUnderscoreMethodReference(@NotNull StringLiteralExpression element, @NotNull PhpClass phpClass) {
        super(element);
        this.phpClass = phpClass;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        String contents = getElement().getContents();
        if(org.apache.commons.lang3.StringUtils.isBlank(contents)) {
            return new ResolveResult[0];
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        Set<String> methods = getCamelizeAndUnderscoreString(contents);

        // provide setter fallback for non model class or or unknown methods
        for (String value : methods) {
            Method method = phpClass.findMethodByName("set" + value);
            if (method != null) {
                psiElements.add(method);
            }
        }

        // property path
        psiElements.addAll(this.phpClass.getFields().stream()
            .filter(field -> !field.isConstant() && field.getModifier().isPublic() && methods.contains(field.getName()))
            .toList()
        );

        return PsiElementResolveResult.createResults(psiElements);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        visitPropertyPath(
            this.phpClass,
            pair -> lookupElements.add(new PhpFormPropertyMethodLookupElement(pair.getSecond(), pair.getFirst()))
        );

        return lookupElements.toArray();
    }

    public static void visitPropertyPath(@NotNull PhpClass phpClass, @NotNull Consumer<Pair<String, PhpNamedElement>> consumer) {
        // provide setter fallback for non-model class or unknown methods
        for (Method method: phpClass.getMethods()) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")) {
                consumer.accept(new Pair<>(StringUtils.lcfirst(name.substring(3)), method));
            }
        }

        // Symfony\Component\PropertyAccess\PropertyAccessor::getWriteAccessInfo
        // property: public $foobar
        for (Field field: phpClass.getFields()) {
            if (!field.isConstant() && field.getModifier().isPublic()) {
                consumer.accept(new Pair<>(field.getName(), field));
            }
        }
    }

    @NotNull
    private Set<String> getCamelizeAndUnderscoreString(@NotNull String string) {
        TreeSet<String> strings = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        string = StringUtils.lcfirst(string);

        strings.addAll(Arrays.asList(
            StringUtils.underscore(string),
            StringUtils.camelize(string),
            string
        ));

        return strings;
    }
}
