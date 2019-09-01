package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlKeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {
    private final static Map<String, String> YAML_KEYWORDS = new HashMap<String, String>() {{
        put("~", "null");
        put("null", "null");
        put("true", "bool");
        put("false", "bool");
        put(".inf", "double");
    }};

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        PsiElement psiElement = parameters.getPosition();

        if (psiElement instanceof LeafPsiElement) {
            // Don't complete after tag (at end of line)
            // key: !my_tag <caret>\n
            if (YamlHelper.isElementAfterYamlTag(psiElement)) {
                return;
            }

            // Don't complete after End Of Line:
            // key: foo\n
            // <caret>
            if (YamlHelper.isElementAfterEol(psiElement)) {
                return;
            }

            String prefix = psiElement.getText();

            if (prefix.contains(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED)) {
                prefix = prefix.substring(0, prefix.indexOf(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED));
            }

            result = result.withPrefixMatcher(prefix);
        }

        for (Map.Entry<String, String> entry : YAML_KEYWORDS.entrySet()) {
            String yamlKeyword = entry.getKey();
            String yamlType = entry.getValue();

            LookupElementBuilder lookupElement = LookupElementBuilder.create(yamlKeyword)
                    .withTypeText(yamlType);

            result.addElement(lookupElement);
        }
    }
}
