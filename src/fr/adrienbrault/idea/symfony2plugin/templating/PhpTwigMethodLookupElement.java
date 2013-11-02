package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TwigTypeInsertHandler;
import org.jetbrains.annotations.NotNull;


public class PhpTwigMethodLookupElement extends PhpLookupElement {

    @Override
    public void handleInsert(InsertionContext context) {
        TwigTypeInsertHandler.getInstance().handleInsert(context, this);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        super.renderElement(presentation);

        PhpNamedElement phpNamedElement = this.getNamedElement();

        // reset method to show full name again, which was stripped inside getLookupString
        if(phpNamedElement instanceof Method && phpNamedElement.getName().startsWith("get") && phpNamedElement.getName().length() > 3) {
            presentation.setItemText(phpNamedElement.getName());
        }

    }

    public PhpTwigMethodLookupElement(@NotNull PhpNamedElement namedElement) {
        super(namedElement);
    }

    @NotNull
    public String getLookupString() {
        String lookupString = super.getLookupString();

        // remove getter and set lcfirst
        if(lookupString.startsWith("get") && lookupString.length() > 3) {
            lookupString = lookupString.substring(3);
            lookupString = Character.toLowerCase(lookupString.charAt(0)) + lookupString.substring(1);
        }

        return lookupString;
    }

}
