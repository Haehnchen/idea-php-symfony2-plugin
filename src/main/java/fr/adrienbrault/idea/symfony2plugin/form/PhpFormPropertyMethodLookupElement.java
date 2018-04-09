package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpFormPropertyMethodLookupElement extends PhpLookupElement {

    @NotNull
    private final String methodOverwrite;

    PhpFormPropertyMethodLookupElement(@NotNull PhpNamedElement namedElement, @NotNull String methodOverwrite) {
        super(namedElement);
        this.methodOverwrite = methodOverwrite;
    }

    @NotNull
    public String getLookupString() {
        return this.methodOverwrite;
    }
}
