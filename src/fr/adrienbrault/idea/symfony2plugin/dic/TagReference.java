package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TagReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private String tagName;

    public TagReference(@NotNull StringLiteralExpression element) {
        super(element);
        tagName = element.getContents();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        // @TODO we can provide tagged classes here
        return new ResolveResult[]{};
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<LookupElement>();

        XmlTagParser xmlTagParser = ServiceXmlParserFactory.getInstance(this.getElement().getProject(), XmlTagParser.class);
        for(String tag : xmlTagParser.get()) {
            results.add(new ParameterLookupElement(tag));
        }

        return results.toArray();
    }
}
