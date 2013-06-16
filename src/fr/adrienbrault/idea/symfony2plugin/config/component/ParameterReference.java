package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {

    private String parameterName;

    private boolean wrapPercent = false;

    public ParameterReference(@NotNull PsiElement element, String ParameterName) {
        super(element);
        parameterName = ParameterName;
    }

    public ParameterReference(@NotNull StringLiteralExpression element) {
        super(element);

        parameterName = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    public ParameterReference wrapVariantsWithPercent(boolean WrapPercent) {
        this.wrapPercent = WrapPercent;
        return this;
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {

        String parameterName = ServiceXmlParserFactory.getInstance(getElement().getProject(), ParameterServiceParser.class).getParameterMap().get(this.parameterName);
        if (null == parameterName) {
            return new ResolveResult[]{};
        }

        List<ResolveResult> results = new ArrayList<ResolveResult>();
        results.addAll(PhpElementsUtil.getClassInterfaceResolveResult(getElement().getProject(), parameterName));

        // self add; so variable is not marked as invalid eg in xml
        if(results.size() == 0) {
            results.add(new PsiElementResolveResult(getElement()));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] resolveResults = multiResolve(false);

        return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<LookupElement>();
        Map<String, String> it = ServiceXmlParserFactory.getInstance(getElement().getProject(), ParameterServiceParser.class).getParameterMap();

        for(Map.Entry<String, String> Entry: it.entrySet()) {
            String parameterKey = Entry.getKey();

            // wrap parameter for reuse this class in xml, php and yaml
            if(this.wrapPercent) {
                parameterKey = "%" + parameterKey + "%";
            }

            results.add(new ParameterLookupElement(parameterKey, Entry.getValue()));
        }

        return results.toArray();
    }
}
