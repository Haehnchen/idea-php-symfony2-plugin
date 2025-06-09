package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.insertHandler.ClassConstantInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class ClassConstantLookupElementAbstract extends LookupElement implements ClassConstantInsertHandler.ClassConstantLookupElementInterface {
    @NotNull
    protected final PhpClass phpClass;

    @NotNull
    String phpClassName;

    public ClassConstantLookupElementAbstract(@NotNull PhpClass phpClass) {
        this.phpClass = phpClass;
        this.phpClassName = phpClass.getName();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(phpClass.getName());
        presentation.setTypeText(phpClass.getPresentableFQN());
        presentation.setTypeGrayed(true);
        super.renderElement(presentation);
    }

    @NotNull
    @Override
    public String getLookupString() {
        return this.phpClassName;
    }

    @NotNull
    public Object getObject() {
        return this.phpClass;
    }

    @NotNull
    @Override
    public PhpClass getPhpClass() {
        return this.phpClass;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        ClassConstantInsertHandler.getInstance().handleInsert(context, this);
    }
}
