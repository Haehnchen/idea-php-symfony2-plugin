package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;


public class PhpUnderscoreMethodLookupElement extends PhpLookupElement {

    private static final Pattern UNDERSCORE_PATTERN_1 = Pattern.compile("([A-Z]+)([A-Z][a-z])");
    private static final Pattern UNDERSCORE_PATTERN_2 = Pattern.compile("([a-z\\d])([A-Z])");

    public PhpUnderscoreMethodLookupElement(@NotNull PhpNamedElement namedElement) {
        super(namedElement);
    }

    @NotNull
    public String getLookupString() {
        String lookupString = super.getLookupString();

        if(lookupString.startsWith("get") || lookupString.startsWith("set")) {
            lookupString = lookupString.substring(3);
        }

        return underscore(lookupString);
    }

    private String underscore(String camelCasedWord) {

        // Regexes in Java are fucking stupid...
        String underscoredWord = UNDERSCORE_PATTERN_1.matcher(camelCasedWord).replaceAll("$1_$2");
        underscoredWord = UNDERSCORE_PATTERN_2.matcher(underscoredWord).replaceAll("$1_$2");
        underscoredWord = underscoredWord.replace('-', '_').toLowerCase();

        return underscoredWord;
    }

}
