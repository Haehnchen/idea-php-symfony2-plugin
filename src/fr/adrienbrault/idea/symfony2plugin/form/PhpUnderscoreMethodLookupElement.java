package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;


public class PhpUnderscoreMethodLookupElement extends PhpLookupElement {

    public PhpUnderscoreMethodLookupElement(@NotNull PhpNamedElement namedElement) {
        super(namedElement);
    }

    @NotNull
    public String getLookupString() {
        String lookupString = super.getLookupString();

        if(lookupString.startsWith("get") || lookupString.startsWith("set")) {
            lookupString = lookupString.substring(3);
        }

        return StringUtils.underscore(lookupString);
    }


}
