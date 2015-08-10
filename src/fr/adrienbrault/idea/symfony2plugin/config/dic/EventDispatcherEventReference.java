package fr.adrienbrault.idea.symfony2plugin.config.dic;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EventDispatcherEventReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String eventName;

    public EventDispatcherEventReference(@NotNull PsiElement element, String eventName) {
        super(element);
        this.eventName = eventName;
    }

    public EventDispatcherEventReference(@NotNull StringLiteralExpression element) {
        super(element);
        this.eventName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        List<ResolveResult> resolveResults = new ArrayList<ResolveResult>();

        for(PsiElement psiElement: EventDispatcherSubscriberUtil.getEventPsiElements(getElement().getProject(), this.eventName)) {
            resolveResults.add(new PsiElementResolveResult(psiElement));
        }

        return resolveResults.toArray(new ResolveResult[resolveResults.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EventDispatcherSubscriberUtil.getEventNameLookupElements(getElement().getProject()).toArray();
    }
}
