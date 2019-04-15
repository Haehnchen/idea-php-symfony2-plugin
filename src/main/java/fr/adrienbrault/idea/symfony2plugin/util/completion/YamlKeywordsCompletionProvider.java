package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.jsonSchema.ide.JsonSchemaService;
import com.jetbrains.jsonSchema.impl.JsonSchemaObject;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlKeywordsCompletionProvider extends CompletionProvider<CompletionParameters> {

    private static final String YAML_TYPE_NULL = "null";
    private static final String YAML_TYPE_BOOL = "bool";
    private static final String YAML_TYPE_DOUBLE = "double";

    private static final String YAML_KEYWORD_TILDE = "~";
    private static final String YAML_KEYWORD_NULL = "null";
    private static final String YAML_KEYWORD_TRUE = "true";
    private static final String YAML_KEYWORD_FALSE = "false";
    private static final String YAML_KEYWORD_INF = ".inf";

    private final static HashMap<String, String> yamlKeywords = new HashMap<String, String>() {{
        put(YAML_KEYWORD_TILDE, YAML_TYPE_NULL);
        put(YAML_KEYWORD_NULL, YAML_TYPE_NULL);
        put(YAML_KEYWORD_TRUE, YAML_TYPE_BOOL);
        put(YAML_KEYWORD_FALSE, YAML_TYPE_BOOL);
        put(YAML_KEYWORD_INF, YAML_TYPE_DOUBLE);
    }};

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {

        PsiElement psiElement = parameters.getPosition();

        Project project = psiElement.getProject();
        final JsonSchemaService jsonSchemaService = JsonSchemaService.Impl.get(project);
        JsonSchemaObject jsonRootSchema = jsonSchemaService.getSchemaObject(parameters.getOriginalFile().getVirtualFile());

        //final List<JsonSchemaType> yamlTypes = new ArrayList<>();

        if (jsonRootSchema != null) {
            // for now we should not show any keywords, when a JSON Schema is active for this YAML file
            return;

            /* For 2019.2+
             see https://github.com/JetBrains/intellij-community/commit/3fa4357b2fa419710c1819040aa561a53c886d2b

            PsiElement position = parameters.getPosition();
            JsonLikePsiWalker walker = JsonLikePsiWalker.getWalker(position, jsonRootSchema);
            if (walker != null) {
                final PsiElement checkable = walker.findElementToCheck(position);
                final ThreeState isName = walker.isName(checkable);
                final JsonPointerPosition jsonPointerPosition = walker.findPosition(checkable, isName == ThreeState.NO);
                if (jsonPointerPosition != null) {
                    final Collection<JsonSchemaObject> schemas = new JsonSchemaResolver(project, jsonRootSchema, jsonPointerPosition).resolve();
                    schemas.forEach(schema -> {
                        if (isName == ThreeState.NO) {
                            if (schema.getTypeVariants() != null) {
                                yamlTypes.addAll(schema.getTypeVariants());
                            } else if (schema.getType() != null) {
                                yamlTypes.add(schema.getType());
                            }
                        }
                    });
                }
            }
            */
        }


        if (psiElement instanceof LeafPsiElement) {
//            // Don't complete after tag (at end of line)
//            // key: !my_tag <caret>\n
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

        /*
        boolean isYamlNullable = yamlTypes.contains(JsonSchemaType._null);
        boolean isYamlNumber = yamlTypes.contains(JsonSchemaType._number) || yamlTypes.contains(JsonSchemaType._integer);
        boolean elementHasType = yamlTypes.size() > 0;
        */

        for (Map.Entry<String, String> entry : yamlKeywords.entrySet()) {
            String yamlKeyword = entry.getKey();
            String yamlType = entry.getValue();

            /*if (elementHasType) {
                if (YAML_KEYWORD_TILDE.equals(yamlKeyword) && !isYamlNullable) {
                    continue;
                }
                if (YAML_KEYWORD_NULL.equals(yamlKeyword) && isYamlNullable) {
                    continue;
                }
                if (YAML_TYPE_BOOL.equals(yamlType)) {
                    continue;
                }
                if (YAML_TYPE_DOUBLE.equals(yamlType) && !isYamlNumber) {
                    continue;
                }
            }*/

            LookupElementBuilder lookupElement = LookupElementBuilder.create(yamlKeyword)
                    .withTypeText(yamlType);
            result.addElement(lookupElement);
        }
    }
}
