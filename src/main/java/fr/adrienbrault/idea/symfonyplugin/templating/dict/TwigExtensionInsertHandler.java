package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.FunctionInsertHandler;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionInsertHandler {
    private static final TwigExtensionInsertHandler INSTANCE = new TwigExtensionInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement, @NotNull TwigExtension twigExtension) {
        // {{ form_javasc|() }}
        // {{ form_javasc| }}
        if(PhpInsertHandlerUtil.isStringAtCaret(context.getEditor(), "(")) {
            return;
        }

        FunctionInsertHandler.getInstance().handleInsert(context, lookupElement);

        // if first parameter is a string type; add quotes
        for (PsiElement psiElement : PhpElementsUtil.getPsiElementsBySignature(context.getProject(), twigExtension.getSignature())) {
            if(!(psiElement instanceof Function)) {
                continue;
            }

            Parameter[] parameters = ((Function) psiElement).getParameters();

            // skip Twig parameter, we need first function parameter
            int parameter = 0;
            if(twigExtension.getOption("needs_context") != null) {
                parameter++;
            }

            if(twigExtension.getOption("needs_environment") != null) {
                parameter++;
            }

            if(parameters.length <= parameter) {
                continue;
            }

            if(!isString(parameters[parameter].getType())) {
                continue;
            }

            // wrap caret with '' so we have foobar('<caret>')
            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "''");
            context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

            return;
        }
    }

    private boolean isString(@NotNull PhpType type) {
        for (String s : type.getTypes()) {
            if(StringUtils.stripStart(s, "\\").equalsIgnoreCase("string")) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    public static TwigExtensionInsertHandler getInstance(){
        return INSTANCE;
    }
}