package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationUseImporter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpConstantFieldPhpLookupElement extends PhpLookupElement {

    final private PhpClass phpClass;

    public PhpConstantFieldPhpLookupElement(@NotNull Field field) {
        super(field);
        this.phpClass = field.getContainingClass();
    }

    @NotNull
    @Override
    public Object getObject() {
        return this.phpClass;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return this.phpClass.getName() + "::" + super.getLookupString();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        AnnotationUseImporter.insertUse(context, phpClass.getFQN());
    }

}
