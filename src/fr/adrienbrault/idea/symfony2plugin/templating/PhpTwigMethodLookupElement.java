package fr.adrienbrault.idea.symfony2plugin.templating;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;


public class PhpTwigMethodLookupElement extends PhpLookupElement {

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
