package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlText;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.PhpResolveResult;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ConstantXmlReference extends PsiPolyVariantReferenceBase<XmlText> {

    private String contents;

    ConstantXmlReference(@NotNull XmlText element) {
        super(element);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        contents = getElement().getValue();

        // FOO
        if (!contents.contains("::")) {
            return PhpResolveResult.createResults(
                PhpIndex.getInstance(getElement().getProject()).getConstantsByName(contents)
            );
        }

        // Foo\FooBar::FOO
        String[] parts = contents.split("::");
        if(parts.length != 2) {
            return new ResolveResult[0];
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(getElement().getProject(), parts[0]);
        if(phpClass == null) {
            return new ResolveResult[0];
        }

        Field field = phpClass.findFieldByName(parts[1], true);
        if(field == null) {
            return new ResolveResult[0];
        }

        return PhpResolveResult.createResults(field);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}