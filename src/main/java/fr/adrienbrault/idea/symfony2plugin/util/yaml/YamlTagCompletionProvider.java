package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.ElementType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlTagCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final String TAG_PHP_CONST = "!php/const";
    private static final String TAG_PHP_OBJECT = "!php/object";
    private static final String TAG_TAGGED = "!tagged";
    private static final String[] yamlTags = {TAG_PHP_CONST, TAG_PHP_OBJECT, TAG_TAGGED, "!!str", "!!float", "!!binary"};

    @NotNull
    private static final InsertQuotesInsertHandler insertQuotesInsertHandler = new InsertQuotesInsertHandler();

    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        String elementText = psiElement.getText();
        //System.out.println("text: " + elementText);

        if (psiElement instanceof LeafPsiElement) {
            // Don't complete for multiple tags
            // key: !my_tag !<caret>
            if (((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.TAG) {
                if (psiElement.getParent() instanceof PsiErrorElementImpl) {
                    // Error "Multiple tags are not permitted"
                    if (((PsiErrorElementImpl) psiElement.getParent()).getElementType() == ElementType.ERROR_ELEMENT) {
                        return;
                    }
                }
            }

            // Don't complete again after tag (at end of line)
            // key: !my_tag <caret>\n
            if (YamlHelper.isElementAfterYamlTag(psiElement)) {
                return;
            }

            // Don't complete again after tag (after second "!")
            // key: !my_tag !!<caret>
            if (((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.TEXT) {
                PsiElement prevElement = PsiTreeUtil.getPrevSiblingOfType(psiElement.getPrevSibling(), LeafPsiElement.class);
                if (prevElement != null) {
                    if (prevElement.getText().startsWith("!")) {
                        return;
                    }
                }
            }

            // Workaround: "!!" is composite element (first "!" is tag and second "!" inside text)
            if (psiElement.getParent() != null) {
                if (psiElement.getParent().getParent() != null) {
                    String text = psiElement.getParent().getParent().getText();
                    if (text.startsWith("!!")) {
                        elementText = "!" + elementText;
                    }
                }
            }
            if (elementText.startsWith("!") || elementText.startsWith(".")) {
                result = result.withPrefixMatcher(elementText);
            }
        }
        boolean allTags = parameters.getInvocationCount() > 2;
        boolean isServices = YamlHelper.isInsideServiceDefinition(psiElement);
        boolean isConfig = YamlHelper.isConfigFile(psiElement.getContainingFile());

        for (String tag : yamlTags) {
            // Only in DIC services.yml
            // arguments: [ !tagged ... ]
            if (tag.equals(TAG_TAGGED) && !isServices) {
                continue;
            }

            // in DIC YamlFileLoader (services.yml and config.yml) we know for sure we have !php/const
            //  ...yamlParser->parse(..., Yaml::PARSE_CONSTANT | Yaml::PARSE_CUSTOM_TAGS ...
            if (tag.equals(TAG_PHP_CONST) && !allTags && !isServices && !isConfig) {
                continue;
            }

            LookupElementBuilder lookupElement = LookupElementBuilder.create(tag + " ")
                    .withPresentableText(tag);

            // Show !php/object only when invoking completion 2 times and we're not inside services
            // (see parse flags in comment above)
            if (tag.equals(TAG_PHP_OBJECT))
                if (!allTags || isServices) {
                    continue;
                } else {
                    // key: !php/object '<caret>'
                    lookupElement = lookupElement.withInsertHandler(insertQuotesInsertHandler);
                }

            result.addElement(lookupElement);
        }
    }

    static class InsertQuotesInsertHandler implements InsertHandler<LookupElement> {

        @Override
        public void handleInsert(InsertionContext context, LookupElement lookupElement) {
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), "''", false, 1);
            //context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
        }
    }
}
