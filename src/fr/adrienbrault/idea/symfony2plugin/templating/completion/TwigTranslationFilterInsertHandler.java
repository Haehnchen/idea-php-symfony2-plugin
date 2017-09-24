package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTranslationFilterInsertHandler implements InsertHandler<TranslatorLookupElement> {
    @NotNull
    private static final TwigTranslationFilterInsertHandler instance = new TwigTranslationFilterInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull TranslatorLookupElement lookupElement) {
        PsiElement elementAt = context.getFile().findElementAt(context.getStartOffset());

        String domain = null;

        if(elementAt != null) {
            PsiElement element = TwigUtil.getElementOnTwigViewProvider(elementAt);
            TwigUtil.DomainScope domainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : elementAt);
            String transDefaultDomain = domainScope.getDefaultDomain();

            String myDomain = lookupElement.getDomain();
            if(!transDefaultDomain.equalsIgnoreCase(myDomain)) {
                domain = myDomain;
            }
        }

        // preappend
        context.getDocument().insertString(context.getStartOffset(), "{{ '");

        StringBuilder stringBuilder = new StringBuilder();

        // 'foo.bar'
        stringBuilder.append("'|trans");

        // domain
        if(domain != null) {
            stringBuilder.append(String.format("({}, '%s')", domain));
        }

        stringBuilder.append(" }}");

        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), stringBuilder.toString());
    }

    @NotNull
    public static TwigTranslationFilterInsertHandler getInstance(){
        return instance;
    }
}