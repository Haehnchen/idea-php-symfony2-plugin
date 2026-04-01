package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPattern {
    private static final ElementPattern<?>[] PARAMETER_WHITE_LIST = new ElementPattern[]{
        PlatformPatterns.psiElement(PsiWhiteSpace.class),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.NUMBER),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.CONCAT),
        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
        PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
        PlatformPatterns.psiElement(TwigTokenTypes.DOT)
    };
    /**
     * ([) "FOO", 'FOO' (])
     */
    public static final ElementPattern<PsiElement> STRING_WRAP_PATTERN = PlatformPatterns.or(
        PlatformPatterns.psiElement(PsiWhiteSpace.class),
        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE)
    );

    public static final String DOC_SEE_REGEX  = "@see[\\s]+([-@\\./\\:\\w\\\\\\[\\]]+)[\\s]*";

    /**
     * Some workaround: "FUNCTION_CALL" got broken in PhpStorm 2024.x
     */
    private static final PatternCondition<PsiElement> PARENTHESIZED_FUNCTION_NAME_WORKAROUND = new PatternCondition<>("PARENTHESIZED_EXPRESSION workaround") {
        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
            PsiElement psiElement1 = null;

            String parent = psiElement.getNode().toString();
            if (parent.contains("PARENTHESIZED_EXPRESSION")) {
                // its "PARENTHESIZED_EXPRESSION" on PhpStorm 2024.x
                // function name is outside
                PsiElement firstChild = psiElement.getPrevSibling();
                if (firstChild != null && firstChild.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                    psiElement1 = firstChild;
                }
            } else if (parent.contains("FUNCTION_CALL")) {
                // rightly nested; function is included
                PsiElement firstChild = psiElement.getFirstChild();
                if (firstChild != null && firstChild.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                    psiElement1 = firstChild;
                }
            }

            if (psiElement1 == null) {
                return false;
            }

            String text = psiElement1.getText();
            return ("trans".equals(text) || "transchoice".equals(text));
        }
    };

    // -----------------------------------------------------------------------
    // Cached patterns - declared in dependency order
    // -----------------------------------------------------------------------

    /**
     * Provide a workaround for getting a FUNCTION scope as it not consistent in all Twig elements
     *
     * {% if asset('') %}
     * {{ asset('') }}
     */
    private static final ElementPattern<PsiElement> FUNCTION_CALL_SCOPE_PATTERN = PlatformPatterns.or(
        // old and inconsistently implementations of FUNCTION_CALL:
        // eg {% if asset('') %} does not provide a FUNCTION_CALL whereas a print block does
        PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
        PlatformPatterns.psiElement(TwigElementTypes.TAG),
        PlatformPatterns.psiElement(TwigElementTypes.IF_TAG),
        PlatformPatterns.psiElement(TwigElementTypes.SET_TAG),
        PlatformPatterns.psiElement(TwigElementTypes.ELSE_TAG),
        PlatformPatterns.psiElement(TwigElementTypes.ELSEIF_TAG),
        PlatformPatterns.psiElement(TwigElementTypes.FOR_TAG),

        // PhpStorm 2017.3.2: {{ asset('') }}
        PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL)
    );

    /**
     * Shared parent pattern for path() hash literal: the LITERAL node that is the second argument of path()/url()
     */
    private static final ElementPattern<PsiElement> PATH_HASH_LITERAL_PATTERN = PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
        PlatformPatterns.or(
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
        ),
        PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).withParent(
                PlatformPatterns.psiElement().withText(PlatformPatterns.string().contains("path"))
            )
        )
    );

    /** {{ 'test'|<caret> }} */
    private static final ElementPattern<PsiElement> FILTER_AS_IDENTIFIER_PATTERN = PlatformPatterns.psiElement()
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement().withElementType(TwigTokenTypes.FILTER)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /**
     * Since PhpStorm 2024.1 value can be nested inside TwigVariableReference element
     *
     * {% for user in users|de<caret> %}
     */
    private static final ElementPattern<PsiElement> FILTER_AS_VARIABLE_REFERENCE_PATTERN = PlatformPatterns.psiElement()
        .withParent(PlatformPatterns.psiElement(TwigVariableReference.class).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement().withElementType(TwigTokenTypes.FILTER)
        ))
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_INCLUDE_TAG_ARRAY_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.INCLUDE_TAG)
        )
        .afterLeafSkipping(
            STRING_WRAP_PATTERN,
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ)
            )
        )
        .beforeLeafSkipping(
            STRING_WRAP_PATTERN,
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ)
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /** Check for {{ include('|')  }}, {% include('|') %} */
    private static final ElementPattern<PsiElement> CACHED_PRINT_BLOCK_OR_TAG_FUNCTION_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .withParent(FUNCTION_CALL_SCOPE_PATTERN)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_FUNCTION_STRING_PARAMETER_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER))
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_AFTER_IS_TOKEN_PATTERN = PlatformPatterns
        .psiElement()
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.IS),
                PlatformPatterns.psiElement(TwigTokenTypes.NOT)
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_AFTER_IS_TOKEN_WITH_ONE_IDENTIFIER_LEAF_PATTERN = PlatformPatterns
        .psiElement()
        .withParent(PlatformPatterns
            .psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).afterLeafSkipping(PlatformPatterns.psiElement(PsiWhiteSpace.class), PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.IS),
                    PlatformPatterns.psiElement(TwigTokenTypes.NOT)
                ))
            ))
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_AFTER_IS_TOKEN_TEXT_PATTERN = PlatformPatterns.or(
        PlatformPatterns.psiElement(TwigTokenTypes.IS),
        PlatformPatterns.psiElement(TwigTokenTypes.NOT)
    );

    private static final ElementPattern<PsiElement> CACHED_AFTER_OPERATOR_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .inside(PlatformPatterns.psiElement(TwigElementTypes.IF_TAG))
        .andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(TwigElementTypes.FIELD_REFERENCE)))
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TAG_TOKEN_PARSER_PATTERN = PlatformPatterns
        .psiElement()
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_START),
                PlatformPatterns.psiElement(PsiErrorElement.class)
            )
        )
        .beforeLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_END)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TAG_TOKEN_BLOCK_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.TAG_NAME)
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_EMBED_PATTERN = getTagNameParameterPattern(TwigElementTypes.EMBED_TAG, "embed");

    private static final ElementPattern<PsiElement> CACHED_PRINT_BLOCK_FUNCTION_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
        .inside(PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigPsiReference.class),
            PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL),
            PlatformPatterns.psiElement(TwigElementTypes.METHOD_CALL)
        ))
        .inside(FUNCTION_CALL_SCOPE_PATTERN)
        .withLanguage(TwigLanguage.INSTANCE);

    /** "{{ _self.input('password', '', 'password') }}" */
    private static final ElementPattern<PsiElement> CACHED_SELF_MACRO_FUNCTION_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).with(PhpElementsUtil.EMPTY_PREVIOUS_LEAF)
        .inside(PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL)
            .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOT).afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.RESERVED_ID)))
        )
        .inside(FUNCTION_CALL_SCOPE_PATTERN)
        .withLanguage(TwigLanguage.INSTANCE);

    /** {{ _self.input() }} */
    private static final ElementPattern<PsiElement> CACHED_SELF_MACRO_IDENTIFIER_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).afterLeaf(
        PlatformPatterns.psiElement(TwigTokenTypes.DOT)
            .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.RESERVED_ID))
        )
        .inside(FUNCTION_CALL_SCOPE_PATTERN)
        .withLanguage(TwigLanguage.INSTANCE);

    /** {{ form(foo) }}, {{ foo }}, {% if foo %} — NOT: {{ foo.bar }}, {{ 'foo.bar' }} */
    private static final ElementPattern<PsiElement> CACHED_COMPLETABLE_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
        .inside(PlatformPatterns.psiElement(TwigElementTypes.VARIABLE_REFERENCE))
        .andNot(
            PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(TwigElementTypes.FIELD_REFERENCE))
        ).inside(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
                PlatformPatterns.psiElement(TwigElementTypes.SET_TAG),
                PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_BLOCK_TAG_PATTERN = PlatformPatterns.or(
        // {% block "foo" %}
        PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
        )
        .withParent(
            PlatformPatterns.psiElement(TwigBlockTag.class)
        )
        .withLanguage(TwigLanguage.INSTANCE),

        // {% block foo %}
        PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
            )
            .withParent(
                PlatformPatterns.psiElement(TwigBlockTag.class)
            )
            .withLanguage(TwigLanguage.INSTANCE)
    );

    private static final ElementPattern<PsiElement> CACHED_FILTER_TAG_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.FILTER_TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TRANS_DEFAULT_DOMAIN_PATTERN = getArgumentAfterTagNamePattern("trans_default_domain");

    private static final ElementPattern<PsiElement> CACHED_PATH_AFTER_LEAF_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL)
            )
        )
        .withParent(PATH_HASH_LITERAL_PATTERN)
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_PATH_AFTER_LEAF_FOR_IDENTIFIER_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL)
            )
        )
        .withParent(PATH_HASH_LITERAL_PATTERN)
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_PARENT_FUNCTION_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .withText("parent")
        .beforeLeaf(
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TYPE_COMPLETION_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOT))
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_AUTOCOMPLETABLE_ROUTE_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("path"),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("url")
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /**
     * app.request.attributes.get('_route') == '<caret>'
     * app.request.attributes.get('_route') != '<caret>'
     */
    private static final ElementPattern<PsiElement> CACHED_ROUTE_COMPARE_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.EQ_EQ),
                PlatformPatterns.psiElement(TwigTokenTypes.NOT_EQ)
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /**
     * app.request.attributes.get('_route') starts with '<caret>'
     * app.request.attributes.get('_route') starts with('<caret>')
     */
    private static final ElementPattern<PsiElement> CACHED_ROUTE_STARTS_WITH_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("with")
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /**
     * app.request.attributes.get('_route') is same as('<caret>')
     */
    private static final ElementPattern<PsiElement> CACHED_ROUTE_SAME_AS_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("as")
        )
        .withLanguage(TwigLanguage.INSTANCE);

    /**
     * app.request.attributes.get('_route') in ['<caret>', 'route_b']
     * app.request.attributes.get('_route') not in ['<caret>', 'route_b']
     */
    private static final ElementPattern<PsiElement> CACHED_ROUTE_IN_ARRAY_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.ARRAY_VALUE)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.ARRAY_LITERAL)
                )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_COMPONENT_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("component")
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_AUTOCOMPLETABLE_ASSET_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("asset"),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("absolute_url")
            )
        )
        .beforeLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TEMPLATE_FILE_REFERENCE_TAG_PATTERN = getTemplateFileReferenceTagPattern("extends", "from", "include", "use", "import", "embed");

    private static final ElementPattern<PsiElement> CACHED_TEMPLATE_IMPORT_FILE_REFERENCE_TAG_PATTERN = PlatformPatterns
        .psiElement()
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.VARIABLE_REFERENCE).andNot(PlatformPatterns.psiElement().afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.AS_KEYWORD)
            )).withParent(
                PlatformPatterns.psiElement(TwigElementTypes.IMPORT_TAG)
            )
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_APPLY_FILTER_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).with(new PatternCondition<>("aa") {
                @Override
                public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
                    return "apply".equalsIgnoreCase(psiElement.getText());
                }
            })
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_FOR_TAG_IN_VARIABLE_PATTERN = PlatformPatterns.psiElement().afterLeafSkipping(
        PlatformPatterns.or(
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
        ),
        PlatformPatterns.psiElement(TwigTokenTypes.IN).withLanguage(TwigLanguage.INSTANCE));

    private static final ElementPattern<PsiElement> CACHED_FOR_TAG_IN_VARIABLE_REFERENCE_PATTERN = PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
            TwigElementTypes.FIELD_REFERENCE)).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IN)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_FOR_TAG_VARIABLE_PATTERN = PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
            TwigElementTypes.FIELD_REFERENCE)).beforeLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IN)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_IF_VARIABLE_PATTERN = PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
            TwigElementTypes.FIELD_REFERENCE)).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(
                PlatformPatterns.string().oneOfIgnoreCase("if")
            )
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_IF_CONDITION_VARIABLE_PATTERN = PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
            TwigElementTypes.FIELD_REFERENCE)).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigTokenTypes.LE),
                PlatformPatterns.psiElement(TwigTokenTypes.LT),
                PlatformPatterns.psiElement(TwigTokenTypes.GE),
                PlatformPatterns.psiElement(TwigTokenTypes.GT),
                PlatformPatterns.psiElement(TwigTokenTypes.EQ_EQ),
                PlatformPatterns.psiElement(TwigTokenTypes.NOT_EQ)
            )
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.IF_TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TWI_TAG_USE_NAME_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .withParent(PlatformPatterns.psiElement(TwigElementTypes.TAG))
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("use")
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_SET_VARIABLE_PATTERN = PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
            TwigElementTypes.FIELD_REFERENCE)).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.EQ)
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.SET_TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_INCLUDE_ONLY_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER).withText("only")
        .beforeLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_END)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_FROM_TEMPLATE_ELEMENT_PATTERN = PlatformPatterns.or(
        PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("from"))
            )
            .withLanguage(TwigLanguage.INSTANCE),
        PlatformPatterns
            .psiElement(TwigTokenTypes.RESERVED_ID)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("from"))
            )
            .withLanguage(TwigLanguage.INSTANCE)
    );

    private static final PsiElementPattern CACHED_FIRST_FUNCTION_PARAMETER_AS_STRING_PATTERN;
    static {
        ElementPattern<?>[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };
        CACHED_FIRST_FUNCTION_PARAMETER_AS_STRING_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.LBRACE));
    }

    private static final PsiElementPattern CACHED_PARAMETER_AS_STRING_PATTERN;
    static {
        ElementPattern<?>[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };
        CACHED_PARAMETER_AS_STRING_PATTERN = PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.COMMA), PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA));
    }

    private static final PsiElementPattern.Capture<PsiElement> CACHED_FORM_THEME_FILE_TAG_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.STRING_TEXT)
        .withParent(PlatformPatterns.psiElement().withText(PlatformPatterns.string().matches("\\{%\\s+form_theme.*")))
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_GUARD_TYPE_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("guard")
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_GUARD_CALLABLE_PATTERN = PlatformPatterns
        .psiElement(TwigTokenTypes.IDENTIFIER)
        .afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                PlatformPatterns.string().oneOf("function", "filter", "test")
            )
        )
        .withParent(
            PlatformPatterns.psiElement(TwigElementTypes.TAG)
        )
        .withLanguage(TwigLanguage.INSTANCE);

    private static final ElementPattern<PsiElement> CACHED_TWI_DOC_SEE_PATTERN = PlatformPatterns.or(
        PlatformPatterns.psiElement(TwigTokenTypes.COMMENT_TEXT).withText(PlatformPatterns.string().matches(DOC_SEE_REGEX)).withLanguage(TwigLanguage.INSTANCE),
        PlatformPatterns.psiElement(TwigTokenTypes.COMMENT_TEXT).withText(PlatformPatterns.string().matches(TwigUtil.DOC_SEE_REGEX_WITHOUT_SEE)).withLanguage(TwigLanguage.INSTANCE)
    );

    // Depends on CACHED_FOR_TAG_IN_VARIABLE_PATTERN etc.
    private static final ElementPattern<PsiElement> CACHED_VARIABLE_TYPE_PATTERN = PlatformPatterns.or(
        CACHED_FOR_TAG_IN_VARIABLE_PATTERN,
        CACHED_FOR_TAG_IN_VARIABLE_REFERENCE_PATTERN,
        CACHED_IF_VARIABLE_PATTERN,
        CACHED_IF_CONDITION_VARIABLE_PATTERN,
        CACHED_SET_VARIABLE_PATTERN
    );

    // Depends on FILTER_AS_* patterns
    private static final ElementPattern<PsiElement> CACHED_FILTER_PATTERN = PlatformPatterns.or(
        FILTER_AS_IDENTIFIER_PATTERN,
        FILTER_AS_VARIABLE_REFERENCE_PATTERN
    );

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * {% trans with {'%name%': 'Fabien'} from "app" %}
     * {% transchoice count with {'%name%': 'Fabien'} from "app" %}
     */
    public static ElementPattern<PsiElement> getTranslationTokenTagFromPattern() {

        // we need to use withText check, because twig tags dont have childrenAllowToVisit to search for tag name
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG).withText(
                        PlatformPatterns.string().matches("\\{%\\s+(trans|transchoice).*")
                    )
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("from")
                ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG).withText(
                        PlatformPatterns.string().matches("\\{%\\s+(trans|transchoice).*")
                    )
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("from")
                ).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * {% include ['', ~ '', ''] %}
     */
    public static ElementPattern<PsiElement> getIncludeTagArrayPattern() {
        return CACHED_INCLUDE_TAG_ARRAY_PATTERN;
    }

    /**
     * {% include foo ? '' : '' %}
     * {% extends foo ? '' : '' %}
     */
    public static ElementPattern<PsiElement> getTagTernaryPattern(@NotNull IElementType type) {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(type)
                )
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.QUESTION)
                )
                .withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(type)
                )
                .afterLeafSkipping(
                    STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
                .withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * Check for {{ include('|')  }}, {% include('|') %}
     *
     * @param functionName twig function name
     */
    public static ElementPattern<PsiElement> getPrintBlockOrTagFunctionPattern(String... functionName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                getFunctionCallScopePattern()
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Check for {{ include('|')  }}, {% include('|') %}
     */
    public static ElementPattern<PsiElement> getPrintBlockOrTagFunctionPattern() {
        return CACHED_PRINT_BLOCK_OR_TAG_FUNCTION_PATTERN;
    }

    /**
     * Check for {{ foo(bar, '|')  }}, {% foo(bar, '|') %}
     *
     * @param functionName twig function name
     */
    public static ElementPattern<PsiElement> getPrintBlockOrTagFunctionSecondParameterPattern(String... functionName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                getFunctionCallScopePattern()
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.or(PARAMETER_WHITE_LIST),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                    )
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ foo('<caret>') }}
     * {{ 'test'|foo('<caret>') }}
     * {% apply date('<caret>') %}foobar{% endapply %}
     */
    public static ElementPattern<PsiElement> getFunctionStringParameterPattern() {
        return CACHED_FUNCTION_STRING_PARAMETER_PATTERN;
    }

    /**
     * Literal are fine in lexer so just extract the parameter
     *
     * {{ foo({'foobar', 'foo<caret>bar'}) }}
     * {{ foo({'fo<caret>obar'}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsLiteralPattern(@NotNull String... functionName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Literal as second parameter
     *
     * {{ foo(bar, {'foobar', 'foo<caret>bar'}) }}
     * {{ foo(bar, {'fo<caret>obar'}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithSecondParameterAsLiteralPattern(@NotNull String... functionName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(PARAMETER_WHITE_LIST),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                            PlatformPatterns.or(
                                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                            ),
                            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                        )
                    )
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ foo({'foo<caret>bar': 'foo'}}) }}
     * {{ foo({'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsKeyLiteralPattern(@NotNull String... functionName) {
        return PlatformPatterns.or(
            PlatformPatterns
                // ",'foo'", {'foo'"
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL).withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                    )
                )
            ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns
                // ",'foo'", {'foo'"
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.psiElement().with(new MyBeforeColonAndInsideLiteralPatternCondition()),
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
            )
        );
    }

    /**
     * {{ foo(12, {'foo<caret>bar': 'foo'}}) }}
     * {{ foo(12, {'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithSecondParameterAsKeyLiteralPattern(@NotNull String... functionName) {
        PsiElementPattern.Capture<PsiElement> parameterPattern = PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                PlatformPatterns.or(PARAMETER_WHITE_LIST),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.NUMBER)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            )
        );

        return
            PlatformPatterns.or(
                // {{ foo({'foobar': 'foo', 'foo<caret>bar': 'foo'}}) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).withParent(parameterPattern)
                ).withLanguage(TwigLanguage.INSTANCE),
                // {{ foo(12, {'foo<caret>bar': 'foo'}}) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL).withParent(parameterPattern)
                )
                    .withLanguage(TwigLanguage.INSTANCE)
            );
    }

    /**
     * Array values are not detected by lexer, lets do the magic on our own
     *
     * {{ foo(['foobar', 'foo<caret>bar']) }}
     * {{ foo(['fo<caret>obar']) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithFirstParameterAsArrayPattern(@NotNull String... functionName) {

        // "foo(<caret>"
        PsiElementPattern.Capture<PsiElement> functionPattern = PlatformPatterns
            .psiElement(TwigTokenTypes.LBRACE_SQ)
            .afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                )
            );

        return getStringInArrayPattern(functionPattern);
    }

    /**
     * Array values as second parameter
     *
     * {{ foo(bar, ['foobar', 'foo<caret>bar']) }}
     * {{ foo(bar, ['fo<caret>obar']) }}
     */
    public static ElementPattern<PsiElement> getFunctionWithSecondParameterAsArrayPattern(@NotNull String... functionName) {

        // "foo(param, [<caret>"
        PsiElementPattern.Capture<PsiElement> functionPattern = PlatformPatterns
            .psiElement(TwigTokenTypes.LBRACE_SQ)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                    PlatformPatterns.or(PARAMETER_WHITE_LIST),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf(functionName))
                    )
                )
            );

        return getStringInArrayPattern(functionPattern);
    }

    /**
     * Matches a STRING_TEXT token inside an array literal that follows the given {@code arrayOpenPattern}.
     * Handles both the first element and subsequent elements (after a comma).
     */
    private static ElementPattern<PsiElement> getStringInArrayPattern(@NotNull PsiElementPattern.Capture<PsiElement> arrayOpenPattern) {
        return PlatformPatterns.or(
            // first item: ['fo<caret>obar']
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                    TwigTokenTypes.SINGLE_QUOTE,
                    TwigTokenTypes.DOUBLE_QUOTE
                )).afterLeafSkipping(
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    arrayOpenPattern
                )
            ).withLanguage(TwigLanguage.INSTANCE),

            // subsequent item: ['foobar', 'foo<caret>bar']
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                    TwigTokenTypes.SINGLE_QUOTE,
                    TwigTokenTypes.DOUBLE_QUOTE
                )).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT),
                            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE),
                            PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                        ),
                        arrayOpenPattern
                    )
                )
            ).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * {% render "foo"
     *
     * @param tagName twig tag name
     */
    public static ElementPattern<PsiElement> getStringAfterTagNamePattern(@NotNull String tagName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.TAG)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * "{% tagName foo"
     * "{% tagName 'foo'"
     */
    public static ElementPattern<PsiElement> getArgumentAfterTagNamePattern(@NotNull String tagName) {
        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(TwigTokenTypes.IDENTIFIER)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG)
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
                ).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .withParent(
                    PlatformPatterns.psiElement(TwigElementTypes.TAG)
                )
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
                ).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    /**
     * Check for {% if foo is "foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenPattern() {
        return CACHED_AFTER_IS_TOKEN_PATTERN;
    }

    /**
     * Check for {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenWithOneIdentifierLeafPattern() {
        return CACHED_AFTER_IS_TOKEN_WITH_ONE_IDENTIFIER_LEAF_PATTERN;
    }

    /**
     * Extract text {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenTextPattern() {
        return CACHED_AFTER_IS_TOKEN_TEXT_PATTERN;
    }

    /**
     * {% if foo <carpet> %}
     * {% if foo.bar <carpet> %}
     * {% if "foo.bar" <carpet> %}
     * {% if 'foo.bar' <carpet> %}
     */
    public static ElementPattern<PsiElement> getAfterOperatorPattern() {
        return CACHED_AFTER_OPERATOR_PATTERN;
    }

    /**
     * Twig tag pattern with some hack
     * because we have invalid psi elements after STATEMENT_BLOCK_START
     *
     * {% <caret> %}
     */
    public static ElementPattern<PsiElement> getTagTokenParserPattern() {
        return CACHED_TAG_TOKEN_PARSER_PATTERN;
    }

    /**
     * Twig tag pattern
     *
     * {% fo<caret>obar %}
     * {% fo<caret>obar 'foo' %}
     */
    public static ElementPattern<PsiElement> getTagTokenBlockPattern() {
        return CACHED_TAG_TOKEN_BLOCK_PATTERN;
    }

    /**
     * {% FOOBAR "WANTED.html.twig" %}
     */
    public static ElementPattern<PsiElement> getTagNameParameterPattern(@NotNull IElementType elementType, @NotNull String tagName) {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(
                PlatformPatterns.psiElement(elementType)
            )
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(tagName)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% embed "vertical_boxes_skeleton.twig" %}
     */
    public static ElementPattern<PsiElement> getEmbedPattern() {
        return CACHED_EMBED_PATTERN;
    }

    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern() {
        return CACHED_PRINT_BLOCK_FUNCTION_PATTERN;
    }

    /**
     * "{{ _self.input('password', '', 'password') }}"
     */
    public static ElementPattern<PsiElement> getSelfMacroFunctionPattern() {
        return CACHED_SELF_MACRO_FUNCTION_PATTERN;
    }

    /**
     * {{ _self.input() }}
     */
    public static ElementPattern<PsiElement> getSelfMacroIdentifierPattern() {
        return CACHED_SELF_MACRO_IDENTIFIER_PATTERN;
    }

    public static ElementPattern<PsiElement> getLeafFunctionPattern(@NotNull String ...functionName) {
        return PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL))
            .withText(PlatformPatterns.string().with(new PatternCondition<>("Twig: Leaf function call") {
                @Override
                public boolean accepts(@NotNull String function, ProcessingContext processingContext) {
                    String funcWithoutSpace = function.replaceAll(" +", "");

                    return Arrays.stream(functionName).anyMatch(wantFunction ->
                        funcWithoutSpace.startsWith(wantFunction + "(") || funcWithoutSpace.equals(wantFunction)
                    );
                }
            }));
    }

    /**
     * Provide a workaround for getting a FUNCTION scope as it not consistent in all Twig elements
     *
     * {% if asset('') %}
     * {{ asset('') }}
     */
    @NotNull
    private static ElementPattern<PsiElement> getFunctionCallScopePattern() {
        return FUNCTION_CALL_SCOPE_PATTERN;
    }

    /**
     * {{ form(foo) }}, {{ foo }}, {% if foo %}
     * NOT: {{ foo.bar }}, {{ 'foo.bar' }}
     */
    public static ElementPattern<PsiElement> getCompletablePattern() {
        return CACHED_COMPLETABLE_PATTERN;
    }

    /**
     * {% block 'foo' %}
     * {% block "foo" %}
     * {% block foo %}
     */
    public static ElementPattern<PsiElement> getBlockTagPattern() {
        return CACHED_BLOCK_TAG_PATTERN;
    }

    /**
     * {% filter foo %}
     */
    public static ElementPattern<PsiElement> getFilterTagPattern() {
        return CACHED_FILTER_TAG_PATTERN;
    }

    /**
     * use getStringAfterTagNamePattern @TODO
     *
     * {% trans_default_domain '<carpet>' %}
     * {% trans_default_domain <carpet> %}
     */
    public static ElementPattern<PsiElement> getTransDefaultDomainPattern() {
        return CACHED_TRANS_DEFAULT_DOMAIN_PATTERN;
    }

    /**
     * trans({}, 'bar')
     * trans(null, 'bar')
     * transchoice(2, null, 'bar')
     */
    public static ElementPattern<PsiElement> getTransDomainPattern() {
        ElementPattern<?>[] whitespace = {
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
        };

        ElementPattern<?>[] placeholder = {
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER),
            PlatformPatterns.psiElement(TwigTokenTypes.DOT),
            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ),
            PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ)
        };

        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.or(
                    // trans({}, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(whitespace),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_CURL).withParent(
                            PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                    PlatformPatterns.or(whitespace),
                                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans"))
                                )
                            )
                        )
                    ),
                    // trans(null, 'bar')
                    // trans(, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(placeholder),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                            PlatformPatterns.or(whitespace),
                            PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans"))
                        )
                    ),
                    // transchoice(2, {}, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(whitespace),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_CURL).withParent(
                            PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                                    PlatformPatterns.or(PARAMETER_WHITE_LIST),
                                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                        PlatformPatterns.or(whitespace),
                                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("transchoice"))
                                    )
                                )
                            )
                        )
                    ),
                    // transchoice(2, null, 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                        PlatformPatterns.or(placeholder),
                        PlatformPatterns.psiElement(TwigTokenTypes.COMMA).afterLeafSkipping(
                            PlatformPatterns.or(PARAMETER_WHITE_LIST),
                            PlatformPatterns.psiElement(TwigTokenTypes.LBRACE).afterLeafSkipping(
                                PlatformPatterns.or(whitespace),
                                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("transchoice"))
                            )
                        )
                    ),
                    // transchoice(2, null, domain='bar')
                    // trans(2, domain='bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.EQ).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("domain"))
                            .withParent(PlatformPatterns.psiElement(TwigVariableReference.class)
                                .withParent(PlatformPatterns.psiElement().with(PARENTHESIZED_FUNCTION_NAME_WORKAROUND)))
                    ),
                    // transchoice(2, null, domain: 'bar')
                    // trans(domain: 'bar')
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON).afterLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("domain"))
                            .withParent(PlatformPatterns.psiElement(TwigVariableReference.class)
                                .withParent(PlatformPatterns.psiElement().with(PARENTHESIZED_FUNCTION_NAME_WORKAROUND)))
                    )
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ path('_profiler_info', {'<caret>'}) }}
     * {{ path('_profiler_info', {'foobar': 'foobar', '<caret>'}) }}
     */
    public static ElementPattern<PsiElement> getPathAfterLeafPattern() {
        return CACHED_PATH_AFTER_LEAF_PATTERN;
    }

    /**
     * {{ path('_profiler_info', {<caret>foo}) }}
     * {{ path('_profiler_info', {<caret>foo: 'bar'}) }}
     * {{ path('_profiler_info', {'foobar': 'foobar', <caret>foo}) }}
     * {{ path('_profiler_info', {'foobar': 'foobar', <caret>foo: 'bar'}) }}
     */
    public static ElementPattern<PsiElement> getPathAfterLeafForIdentifierPattern() {
        return CACHED_PATH_AFTER_LEAF_FOR_IDENTIFIER_PATTERN;
    }

    /**
     * Shared parent pattern for path() hash literal: the LITERAL node that is the second argument of path()/url()
     */
    private static ElementPattern<PsiElement> getPathHashLiteralPattern() {
        return PATH_HASH_LITERAL_PATTERN;
    }

    public static ElementPattern<PsiElement> getParentFunctionPattern() {
        return CACHED_PARENT_FUNCTION_PATTERN;
    }

    /**
     * {{ foo.fo<caret>o }}
     * {{ foo.fo<caret>o() }}
     */
    public static ElementPattern<PsiElement> getTypeCompletionPattern() {
        return CACHED_TYPE_COMPLETION_PATTERN;
    }

    public static ElementPattern<PsiComment> getTwigTypeDocBlockPattern() {
        Collection<PsiElementPattern.Capture<PsiElement>> patterns = new ArrayList<>();

        for (String s : TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE) {
            patterns.add(PlatformPatterns.psiElement(TwigTokenTypes.COMMENT_TEXT).withText(PlatformPatterns.string().matches(s)).withLanguage(TwigLanguage.INSTANCE));
        }

        @SuppressWarnings("unchecked")
        final ElementPattern<PsiComment>[] array = patterns.toArray(new ElementPattern[0]);
        return PlatformPatterns.or(array);
    }

    /**
     * {% types { foo: '<caret>' } %}
     * {% types { foo?: '<caret>' } %}
     */
    @NotNull
    public static ElementPattern<PsiElement> getTypesTagTypeStringPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .with(new PatternCondition<>("types tag") {
                @Override
                public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
                    // Find the TAG element at any parent level
                    for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
                        if (parent instanceof TwigCompositeElement && parent.getNode().getElementType() == TwigElementTypes.TAG) {
                            PsiElement firstChild = parent.getFirstChild();
                            if (firstChild != null) {
                                PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
                                if (tagName != null && "types".equals(tagName.getText())) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                }
            })
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {# @see Foo.html.twig #}
     * {# @see \Class #}
     * {# \Class #}
     */
    @NotNull
    public static ElementPattern<PsiElement> getTwigDocSeePattern() {
        return CACHED_TWI_DOC_SEE_PATTERN;
    }

    public static ElementPattern<PsiElement> getAutocompletableRoutePattern() {
        return CACHED_AUTOCOMPLETABLE_ROUTE_PATTERN;
    }

    /**
     * app.request.attributes.get('_route') == '<caret>'
     * app.request.attributes.get('_route') != '<caret>'
     */
    public static ElementPattern<PsiElement> getTwigRouteComparePattern() {
        return CACHED_ROUTE_COMPARE_PATTERN;
    }

    /**
     * app.request.attributes.get('_route') starts with '<caret>'
     * app.request.attributes.get('_route') starts with('<caret>')
     */
    public static ElementPattern<PsiElement> getTwigRouteStartsWithPattern() {
        return CACHED_ROUTE_STARTS_WITH_PATTERN;
    }

    /**
     * app.request.attributes.get('_route') is same as('<caret>')
     */
    public static ElementPattern<PsiElement> getTwigRouteSameAsPattern() {
        return CACHED_ROUTE_SAME_AS_PATTERN;
    }

    /**
     * app.request.attributes.get('_route') in ['<caret>', 'route_b']
     * app.request.attributes.get('_route') not in ['<caret>', 'route_b']
     */
    public static ElementPattern<PsiElement> getTwigRouteInArrayPattern() {
        return CACHED_ROUTE_IN_ARRAY_PATTERN;
    }

    /**
     * Returns true when the element is inside a route comparison context,
     * i.e. there is a preceding '_route' string literal in the same template block.
     *
     * app.request.attributes.get('_route') == 'my_route'
     */
    public static boolean isRouteCompareContext(@NotNull PsiElement element) {
        PsiElement prev = PsiTreeUtil.prevLeaf(element);
        while (prev != null) {
            IElementType type = prev.getNode().getElementType();
            if (type == TwigTokenTypes.STATEMENT_BLOCK_START || type == TwigTokenTypes.PRINT_BLOCK_START) {
                break;
            }
            if (type == TwigTokenTypes.STRING_TEXT && "_route".equals(prev.getText())) {
                return true;
            }
            prev = PsiTreeUtil.prevLeaf(prev);
        }
        return false;
    }

    /**
     * "{{ component('<caret>'}) }}"
     */
    public static ElementPattern<PsiElement> getComponentPattern() {
        return CACHED_COMPONENT_PATTERN;
    }

    /**
     *  {{ asset('<caret>') }}
     *  {{ asset("<caret>") }}
     *  {{ absolute_url("<caret>") }}
     */
    public static ElementPattern<PsiElement> getAutocompletableAssetPattern() {
        return CACHED_AUTOCOMPLETABLE_ASSET_PATTERN;
    }

    /**
     * Just to support "idea-php-drupal-symfony2-bridge"
     */
    @Deprecated
    public static ElementPattern<PsiElement> getTranslationPattern(@NotNull String... type) {
        return getTranslationKeyPattern(type);
    }

    /**
     * Pattern to match given string before "filter" with given function name
     *
     * {{ 'foo<caret>bar'|trans }}
     * {{ 'foo<caret>bar'|trans() }}
     * {{ 'foo<caret>bar'|transchoice() }}
     */
    @NotNull
    public static ElementPattern<PsiElement> getTranslationKeyPattern(@NotNull String... type) {
        return
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .beforeLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.FILTER).beforeLeafSkipping(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                            PlatformPatterns.psiElement(PsiWhiteSpace.class)
                        ),
                        PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(
                            PlatformPatterns.string().oneOf(type)
                        )
                    )
                )
                .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getAutocompletableAssetTag(String tagName) {
        // @TODO: withChild is not working so we are filtering on text

        // pattern to match '..foo.css' but not match eg ='...'
        //
        // {% stylesheets filter='cssrewrite'
        //  'assets/css/foo.css'
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class)
                )
            .withParent(PlatformPatterns
                    .psiElement(TwigCompositeElement.class)
                    .withText(PlatformPatterns.string().startsWith("{% " + tagName))
            );
    }

    public static ElementPattern<PsiElement> getTemplateFileReferenceTagPattern(String... tagNames) {

        // {% include '<xxx>' with {'foo' : bar, 'bar' : 'foo'} %}

        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf(tagNames))
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getTemplateFileReferenceTagPattern() {
        return CACHED_TEMPLATE_FILE_REFERENCE_TAG_PATTERN;
    }

    public static ElementPattern<PsiElement> getTemplateImportFileReferenceTagPattern() {
        return CACHED_TEMPLATE_IMPORT_FILE_REFERENCE_TAG_PATTERN;
    }

    public static ElementPattern<PsiElement> getForTagVariablePattern() {
        return CACHED_FOR_TAG_VARIABLE_PATTERN;
    }

    /**
     * {{ 'test'|<caret> }}
     * {% for user in users|de<caret> %}
     */
    public static ElementPattern<PsiElement> getFilterPattern() {
        return CACHED_FILTER_PATTERN;
    }

    /**
     * {% apply <caret> %}foobar{% endapply %}
     */
    public static ElementPattern<PsiElement> getApplyFilterPattern() {
        return CACHED_APPLY_FILTER_PATTERN;
    }

    /**
     * @TODO: check related features introduce by PhpStorm for Twig changes in 2020
     */
  public static PsiElementPattern.Capture<PsiElement> captureVariableOrField() {
    return PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
        TwigElementTypes.FIELD_REFERENCE));
  }

    public static ElementPattern<PsiElement> getForTagInVariableReferencePattern() {
        return CACHED_FOR_TAG_IN_VARIABLE_REFERENCE_PATTERN;
    }

    public static ElementPattern<PsiElement> getForTagInVariablePattern() {
        // {% for key, user in "users" %}
        // {% for user in "users" %}
        // {% for user in "users"|slice(0, 10) %}
        return CACHED_FOR_TAG_IN_VARIABLE_PATTERN;
    }

    public static ElementPattern<PsiElement> getTwigTagUseNamePattern() {
        return CACHED_TWI_TAG_USE_NAME_PATTERN;
    }

    public static ElementPattern<PsiElement> getTwigMacroNameKnownPattern(String macroName) {

        // {% macro <foo>(user) %}

        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText(macroName)
            .withParent(PlatformPatterns.psiElement(
                TwigElementTypes.MACRO_TAG
            ))
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText("macro")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getSetVariablePattern() {
        return CACHED_SET_VARIABLE_PATTERN;
    }

    /**
     * {% include 'foo.html.twig' {'foo': 'foo'} only %}
     */
    public static ElementPattern<PsiElement> getIncludeOnlyPattern() {
        return CACHED_INCLUDE_ONLY_PATTERN;
    }

    /**
     * {% from _self import foo %}
     * {% from 'template_name' import foo %}
     */
    public static ElementPattern<PsiElement> getFromTemplateElementPattern() {
        return CACHED_FROM_TEMPLATE_ELEMENT_PATTERN;
    }

    public static ElementPattern<PsiElement> getVariableTypePattern() {
        return CACHED_VARIABLE_TYPE_PATTERN;
    }

    /**
     * Only a parameter is valid "('foobar',"
     */
    @NotNull
    public static PsiElementPattern getFirstFunctionParameterAsStringPattern() {
        return CACHED_FIRST_FUNCTION_PARAMETER_AS_STRING_PATTERN;
    }

    /**
     * Only a parameter is valid ", 'foobar' [,)]"
     */
    @NotNull
    public static PsiElementPattern getParameterAsStringPattern() {
        return CACHED_PARAMETER_AS_STRING_PATTERN;
    }

    public static PsiElementPattern.Capture<PsiComment> getTwigDocBlockMatchPattern(@NotNull String pattern) {
        return PlatformPatterns
            .psiComment().withText(PlatformPatterns.string().matches(pattern))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% form_theme form 'foobar.html.twig' %}
     */
    public static PsiElementPattern.Capture<PsiElement> getFormThemeFileTagPattern() {
        return CACHED_FORM_THEME_FILE_TAG_PATTERN;
    }

    /**
     * trans({
     *  %some%': "button.reserve"|trans,
     *  %vars%': "button.reserve"|trans({}, '<caret>')
     * })
     */
    private static class MyBeforeColonAndInsideLiteralPatternCondition extends PatternCondition<PsiElement> {
        MyBeforeColonAndInsideLiteralPatternCondition() {
            super("BeforeColonAndInsideLiteralPattern");
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            IElementType elementType = psiElement.getNode().getElementType();
            return
                elementType != TwigTokenTypes.LBRACE_CURL &&
                elementType != TwigTokenTypes.RBRACE_CURL &&
                elementType != TwigTokenTypes.COLON;
        }
    }

    /**
     * {% guard function importmap %}
     * {% guard filter upper %}
     * {% guard test even %}
     *
     * Pattern for the type keyword after "guard" tag name
     */
    public static ElementPattern<PsiElement> getGuardTypePattern() {
        return CACHED_GUARD_TYPE_PATTERN;
    }

    /**
     * {% guard function importmap %}
     * {% guard filter upper %}
     * {% guard test even %}
     *
     * Pattern for the callable name after "guard" type keyword
     */
    public static ElementPattern<PsiElement> getGuardCallablePattern() {
        return CACHED_GUARD_CALLABLE_PATTERN;
    }
}
