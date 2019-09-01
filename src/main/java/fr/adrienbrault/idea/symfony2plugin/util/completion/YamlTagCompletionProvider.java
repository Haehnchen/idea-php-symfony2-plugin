package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.impl.source.tree.PsiErrorElementImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;

import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlTagCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final String TAG_PHP_CONST = "!php/const";
    private static final String TAG_PHP_OBJECT = "!php/object";
    private static final String TAG_TAGGED = "!tagged";
    private static final String TAG_TAGGED_ITERATOR = "!tagged_iterator";
    private static final String TAG_TAGGED_LOCATOR = "!tagged_locator";
    private static final String TAG_ITERATOR = "!iterator";
    private static final String TAG_SERVICE = "!service";
    private static final String TAG_SERVICE_LOCATOR = "!service_locator";

    private static final String[] yamlTags = {
        TAG_PHP_CONST,
        TAG_PHP_OBJECT,
        TAG_TAGGED,
        TAG_TAGGED_ITERATOR,
        TAG_TAGGED_LOCATOR,
        TAG_ITERATOR,
        TAG_SERVICE,
        TAG_SERVICE_LOCATOR,
        "!!str",
        "!!float",
        "!!binary"
    };

    private static final String[] yamlServiceTags = {
        TAG_TAGGED,
        TAG_TAGGED_ITERATOR,
        TAG_TAGGED_LOCATOR,
        TAG_ITERATOR,
        TAG_SERVICE,
        TAG_SERVICE_LOCATOR,
    };

    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        String elementText = psiElement.getText();

        if (psiElement instanceof LeafPsiElement) {
            // Don't complete for multiple tags
            // key: !my_tag !<caret>
            if (((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.TAG) {
                if (psiElement.getParent() instanceof PsiErrorElementImpl) {
                    // Error "Multiple tags are not permitted"
                    if (((PsiErrorElementImpl) psiElement.getParent()).getElementType() == TokenType.ERROR_ELEMENT) {
                        return;
                    }
                }
            }

            // Don't complete again after tag (at end of line)
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
        boolean isConfig = YamlHelper.isConfigFile(psiElement.getContainingFile());
        boolean isServices = YamlHelper.isServicesFile(psiElement.getContainingFile());
        boolean isServicesArgument = YamlHelper.isInsideServiceArgumentDefinition(psiElement);

        List<String> serviceTags = Arrays.asList(yamlServiceTags);
        Project project = psiElement.getProject();

        for (String tag : yamlTags) {
            // in DI YamlFileLoader (services.yml and config.yml) we know for sure we have !php/const
            //  ...yamlParser->parse(..., Yaml::PARSE_CONSTANT | Yaml::PARSE_CUSTOM_TAGS ...
            if (tag.equals(TAG_PHP_CONST) && !allTags && !isServices && !isConfig) {
                continue;
            }

            // DI YamlFileLoader specific tags only in services.yml etc
            // arguments: [ !tagged ... ]
            if (serviceTags.contains(tag) && !isServicesArgument) {
                continue;
            }

            // !tagged since Symfony 3.4
            // Since Symfony 4.4 !tagged_iterator should be used instead of !tagged
            if (tag.equals(TAG_TAGGED) && (SymfonyUtil.isVersionGreaterThenEquals(project, "4.4") || SymfonyUtil.isVersionLessThen(project, "3.4"))) {
                continue;
            }

            // !tagged_iterator since Symfony 4.4
            if (tag.equals(TAG_TAGGED_ITERATOR) && !SymfonyUtil.isVersionGreaterThenEquals(project, "4.4")) {
                continue;
            }

            // !tagged_locator since Symfony 4.3
            if (tag.equals(TAG_TAGGED_LOCATOR) && SymfonyUtil.isVersionLessThen(project, "4.3")) {
                continue;
            }

            // !iterator and !service since Symfony 3.3
            if ((tag.equals(TAG_ITERATOR) || tag.equals(TAG_SERVICE)) && SymfonyUtil.isVersionLessThen(project, "3.3")) {
                continue;
            }

            // !service_locator since Symfony 4.2
            if (tag.equals(TAG_SERVICE_LOCATOR) && SymfonyUtil.isVersionLessThen(project, "4.2")) {
                continue;
            }

            LookupElementBuilder lookupElement = LookupElementBuilder.create(tag)
                .withInsertHandler(InsertSpaceInsertHandler.getInstance());

            // Show !php/object only when invoking completion 2 times and we're not inside services
            // (see parse flags in comment above)
            if (tag.equals(TAG_PHP_OBJECT)) {
                if (!allTags || isServices) {
                    continue;
                }

                lookupElement = lookupElement.withInsertHandler(InsertQuotesInsertHandler.getInstance());
            }

            result.addElement(lookupElement);
        }
    }

    static class InsertSpaceInsertHandler implements InsertHandler<LookupElement> {
        private static final InsertSpaceInsertHandler instance = new InsertSpaceInsertHandler();

        @Override
        public void handleInsert(InsertionContext context, @NotNull LookupElement lookupElement) {
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), " ", false, 1);
        }

        public static InsertSpaceInsertHandler getInstance() {
            return instance;
        }
    }

    static class InsertQuotesInsertHandler implements InsertHandler<LookupElement> {
        private static final InsertQuotesInsertHandler instance = new InsertQuotesInsertHandler();

        @Override
        public void handleInsert(InsertionContext context, @NotNull LookupElement lookupElement) {
            EditorModificationUtil.insertStringAtCaret(context.getEditor(), " ''", false, 1);
        }

        public static InsertQuotesInsertHandler getInstance() {
            return instance;
        }
    }
}
