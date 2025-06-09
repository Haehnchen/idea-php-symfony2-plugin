package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TwigTypeInsertHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTwigMethodLookupElement extends PhpLookupElement {
    @Nullable
    private String methodName;

    PhpTwigMethodLookupElement(@NotNull PhpNamedElement namedElement) {
        super(namedElement);

        if (namedElement instanceof Method method) {
            this.methodName = method.getName();
        }
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context) {
        TwigTypeInsertHandler.getInstance().handleInsert(context, this);
    }

    @Override
    public void renderElement(@NotNull LookupElementPresentation presentation) {
        super.renderElement(presentation);

        // reset method to show full name again, which was stripped inside getLookupString
        if (methodName != null && TwigTypeResolveUtil.isPropertyShortcutMethod(methodName)) {
            presentation.setItemText(methodName);
        }
    }

    @NotNull
    public String getLookupString() {
        String lookupString = super.getLookupString();

        // remove property shortcuts eg getter / issers
        if (methodName != null && TwigTypeResolveUtil.isPropertyShortcutMethod(methodName)) {
            lookupString = TwigTypeResolveUtil.getPropertyShortcutMethodName(methodName);
        }

        return lookupString;
    }
}
