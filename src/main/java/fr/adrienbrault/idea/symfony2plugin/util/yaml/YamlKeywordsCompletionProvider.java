package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class YamlKeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {

    public final static HashMap<String, String> yamlKeywords = new HashMap<String, String>() {{
        put("~", "null");
        put("null", "null");
        put("true", "bool");
        put("false", "bool");
        put(".inf", "double");
    }};

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if (psiElement == null) {
            return;
        }

        String elementText = psiElement.getText();
        //System.out.println("text: " + elementText);

        // TODO: Don't complete inside key
//        if (psiElement.getParent() instanceof YAMLMapping && PsiTreeUtil.getPrevSiblingOfType(psiElement, YAMLKeyValue.class) == null) {
//            return;
//        }

        if (psiElement instanceof LeafPsiElement) {
//            // Don't complete after tag (at end of line)
//            // key: !my_tag <caret>\n
            if (YamlHelper.isElementAfterYamlTag(psiElement)) {
                return;
            }

            result = result.withPrefixMatcher(elementText);
        }

        for (Map.Entry<String, String> entry : yamlKeywords.entrySet()) {
            LookupElementBuilder lookupElement = LookupElementBuilder.create(entry.getKey())
                    .withTypeText(entry.getValue());
            result.addElement(lookupElement);
        }
    }
}
