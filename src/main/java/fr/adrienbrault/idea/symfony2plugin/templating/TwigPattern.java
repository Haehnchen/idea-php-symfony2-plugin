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
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPattern {
    @SuppressWarnings("unchecked")
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
        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
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
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
            )
            .withLanguage(TwigLanguage.INSTANCE);
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
                PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER))
            )
            .withLanguage(TwigLanguage.INSTANCE);
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

        return
            PlatformPatterns.or(
                // {{ foo(['fo<caret>obar']) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                        TwigTokenTypes.SINGLE_QUOTE,
                        TwigTokenTypes.DOUBLE_QUOTE
                    )).afterLeafSkipping(
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        functionPattern
                    )
                ).withLanguage(TwigLanguage.INSTANCE),

                // {{ foo(['foobar', 'foo<caret>bar']) }}
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
                            functionPattern
                        )
                    )
                ).withLanguage(TwigLanguage.INSTANCE)
            );
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

        return
            PlatformPatterns.or(
                // {{ foo(bar, ['fo<caret>obar']) }}
                PlatformPatterns
                    .psiElement(TwigTokenTypes.STRING_TEXT).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
                        TwigTokenTypes.SINGLE_QUOTE,
                        TwigTokenTypes.DOUBLE_QUOTE
                    )).afterLeafSkipping(
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        functionPattern
                    )
                ).withLanguage(TwigLanguage.INSTANCE),

                // {{ foo(bar, ['foobar', 'foo<caret>bar']) }}
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
                            functionPattern
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
        return PlatformPatterns
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
    }

    /**
     * Check for {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenWithOneIdentifierLeafPattern() {
        return PlatformPatterns
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
    }

    /**
     * Extract text {% if foo is "foo foo" %}
     */
    public static ElementPattern<PsiElement> getAfterIsTokenTextPattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.IS),
            PlatformPatterns.psiElement(TwigTokenTypes.NOT)
        );
    }

    /**
     * {% if foo <carpet> %}
     * {% if foo.bar <carpet> %}
     * {% if "foo.bar" <carpet> %}
     * {% if 'foo.bar' <carpet> %}
     */
    public static ElementPattern<PsiElement> getAfterOperatorPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .inside(PlatformPatterns.psiElement(TwigElementTypes.IF_TAG))
            .andNot(PlatformPatterns.psiElement().inside(PlatformPatterns.psiElement(TwigElementTypes.FIELD_REFERENCE)))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Twig tag pattern with some hack
     * because we have invalid psi elements after STATEMENT_BLOCK_START
     *
     * {% <caret> %}
     */
    public static ElementPattern<PsiElement> getTagTokenParserPattern() {
        return PlatformPatterns
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
    }

    /**
     * Twig tag pattern
     *
     * {% fo<caret>obar %}
     * {% fo<caret>obar 'foo' %}
     */
    public static ElementPattern<PsiElement> getTagTokenBlockPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.TAG_NAME)
            .withLanguage(TwigLanguage.INSTANCE);
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
        return getTagNameParameterPattern(TwigElementTypes.EMBED_TAG, "embed");
    }

    public static ElementPattern<PsiElement> getPrintBlockFunctionPattern() {
      return PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
          .inside(PlatformPatterns.or(
              PlatformPatterns.psiElement(TwigPsiReference.class),
              PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL),
              PlatformPatterns.psiElement(TwigElementTypes.METHOD_CALL)
          ))
          .inside(getFunctionCallScopePattern())
          .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * "{{ _self.input('password', '', 'password') }}"
     */
    public static ElementPattern<PsiElement> getSelfMacroFunctionPattern() {
        return PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).with(PhpElementsUtil.EMPTY_PREVIOUS_LEAF)
            .inside(PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL)
                .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOT).afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.RESERVED_ID)))
            )
            .inside(getFunctionCallScopePattern())
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ _self.input() }}
     */
    public static ElementPattern<PsiElement> getSelfMacroIdentifierPattern() {
        return PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).afterLeaf(
            PlatformPatterns.psiElement(TwigTokenTypes.DOT)
                .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.RESERVED_ID))
            )
            .inside(getFunctionCallScopePattern())
            .withLanguage(TwigLanguage.INSTANCE);
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
        return PlatformPatterns.or(
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
    }

    /**
     * {{ form(foo) }}, {{ foo }}, {% if foo %}
     * NOT: {{ foo.bar }}, {{ 'foo.bar' }}
     */
    public static ElementPattern<PsiElement> getCompletablePattern() {
        return  PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER)
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
    }

    /**
     * {% block 'foo' %}
     * {% block "foo" %}
     * {% block foo %}
     */
    public static ElementPattern<PsiElement> getBlockTagPattern() {
        return PlatformPatterns.or(

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
    }

    /**
     * {% filter foo %}
     */
    public static ElementPattern<PsiElement> getFilterTagPattern() {
        return
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
                    PlatformPatterns.psiElement(TwigElementTypes.FILTER_TAG)
                )
                .withLanguage(TwigLanguage.INSTANCE)
            ;
    }

    /**
     * use getStringAfterTagNamePattern @TODO
     *
     * {% trans_default_domain '<carpet>' %}
     * {% trans_default_domain <carpet> %}
     */
    public static ElementPattern<PsiElement> getTransDefaultDomainPattern() {
        return getArgumentAfterTagNamePattern("trans_default_domain");
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
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL)
                )
            )
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.LITERAL).afterLeafSkipping(
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
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getParentFunctionPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withText("parent")
            .beforeLeaf(
                PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ foo.fo<caret>o }}
     * {{ foo.fo<caret>o() }}
     */
    public static ElementPattern<PsiElement> getTypeCompletionPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeaf(PlatformPatterns.psiElement(TwigTokenTypes.DOT))
            .withLanguage(TwigLanguage.INSTANCE);
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
     * {# @see Foo.html.twig #}
     * {# @see \Class #}
     * {# \Class #}
     */
    @NotNull
    public static ElementPattern<PsiElement> getTwigDocSeePattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement(TwigTokenTypes.COMMENT_TEXT).withText(PlatformPatterns.string().matches(DOC_SEE_REGEX)).withLanguage(TwigLanguage.INSTANCE),
            PlatformPatterns.psiElement(TwigTokenTypes.COMMENT_TEXT).withText(PlatformPatterns.string().matches(TwigUtil.DOC_SEE_REGEX_WITHOUT_SEE)).withLanguage(TwigLanguage.INSTANCE)
        );
    }

    public static ElementPattern<PsiElement> getAutocompletableRoutePattern() {
        return PlatformPatterns
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
            .withLanguage(TwigLanguage.INSTANCE)
        ;
    }

    /**
     * "{{ component('<caret>'}) }}"
     */
    public static ElementPattern<PsiElement> getComponentPattern() {
        return PlatformPatterns
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
    }

    /**
     *  {{ asset('<caret>') }}
     *  {{ asset("<caret>") }}
     *  {{ absolute_url("<caret>") }}
     */
    public static ElementPattern<PsiElement> getAutocompletableAssetPattern() {
        return PlatformPatterns
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
            .withLanguage(TwigLanguage.INSTANCE)
        ;
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
        return getTemplateFileReferenceTagPattern("extends", "from", "include", "use", "import", "embed");
    }

    public static ElementPattern<PsiElement> getTemplateImportFileReferenceTagPattern() {

        // first: {% from '<xxx>' import foo, <|>  %}
        // second: {% from '<xxx>' import <|>  %}
        // and not: {% from '<xxx>' import foo as <|>  %}

        return PlatformPatterns
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
    }

    public static ElementPattern<PsiElement> getForTagVariablePattern() {
        // {% for "user"  %}

        return captureVariableOrField().beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IN)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {{ 'test'|<caret> }}
     * {% for user in users|de<caret> %}
     */
    public static ElementPattern<PsiElement> getFilterPattern() {
        return PlatformPatterns.or(
            getFilterAsIdentifierPattern(),
            getFilterAsVariableReferencePattern()
        );
    }

    /**
     * {{ 'test'|<caret> }}
     */
    private static ElementPattern<PsiElement> getFilterAsIdentifierPattern() {
        return PlatformPatterns.psiElement()
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement().withElementType(TwigTokenTypes.FILTER)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Since PhpStorm 2024.1 value can be nested inside TwigVariableReference element
     *
     * {% for user in users|de<caret> %}
     */
    private static ElementPattern<PsiElement> getFilterAsVariableReferencePattern() {
        return PlatformPatterns.psiElement()
            .withParent(PlatformPatterns.psiElement(TwigVariableReference.class).afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement().withElementType(TwigTokenTypes.FILTER)
            ))
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% apply <caret> %}foobar{% endapply %}
     */
    public static ElementPattern<PsiElement> getApplyFilterPattern() {
        return PlatformPatterns
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
    }

    /**
     * @TODO: check related features introduce by PhpStorm for Twig changes in 2020
     */
  public static PsiElementPattern.Capture<PsiElement> captureVariableOrField() {
    return PlatformPatterns.psiElement().withElementType(TokenSet.create(TwigElementTypes.VARIABLE_REFERENCE,
        TwigElementTypes.FIELD_REFERENCE));
  }

    public static ElementPattern<PsiElement> getForTagInVariableReferencePattern() {
        // {% for user in test %}
        // {% for user in test.test %}
        return captureVariableOrField().afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IN)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getForTagInVariablePattern() {
        // {% for key, user in "users" %}
        // {% for user in "users" %}
        // {% for user in "users"|slice(0, 10) %}
        return PlatformPatterns.psiElement().afterLeafSkipping(
            PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
            ),
            PlatformPatterns.psiElement(TwigTokenTypes.IN).withLanguage(TwigLanguage.INSTANCE));
    }

    public static ElementPattern<PsiElement> getIfVariablePattern() {

        // {% if "var" %}

        return captureVariableOrField().afterLeafSkipping(
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
    }

    public static ElementPattern<PsiElement> getIfConditionVariablePattern() {

        // {% if var < "var1" %}
        // {% if var == "var1" %}
        // and so on

        return captureVariableOrField().afterLeafSkipping(
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
    }

    public static ElementPattern<PsiElement> getTwigTagUseNamePattern() {

        // {% use '<foo>' %}

        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(PlatformPatterns.psiElement(
                TwigElementTypes.TAG
            ))
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

        // {% set count1 = "var" %}

        return captureVariableOrField().afterLeafSkipping(
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
    }

    /**
     * {% include 'foo.html.twig' {'foo': 'foo'} only %}
     */
    public static ElementPattern<PsiElement> getIncludeOnlyPattern() {

        // {% set count1 = "var" %}

        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER).withText("only")
            .beforeLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.STATEMENT_BLOCK_END)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * {% from _self import foo %}
     * {% from 'template_name' import foo %}
     */
    public static ElementPattern<PsiElement> getFromTemplateElementPattern() {
        return PlatformPatterns.or(
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

    }

    public static ElementPattern<PsiElement> getVariableTypePattern() {
        return PlatformPatterns.or(
            getForTagInVariablePattern(),
            getForTagInVariableReferencePattern(),
            getIfVariablePattern(),
            getIfConditionVariablePattern(),
            getSetVariablePattern()
        );
    }

    /**
     * Only a parameter is valid "('foobar',"
     */
    @NotNull
    public static PsiElementPattern getFirstFunctionParameterAsStringPattern() {
        // string wrapped elements
        ElementPattern<?>[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };

        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.LBRACE));
    }

    /**
     * Only a parameter is valid ", 'foobar' [,)]"
     */
    @NotNull
    public static PsiElementPattern getParameterAsStringPattern() {
        // string wrapped elements
        ElementPattern<?>[] elementPatterns = {
            PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
            PlatformPatterns.psiElement(PsiWhiteSpace.class),
            PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
            PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
        };

        return PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .beforeLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.COMMA), PlatformPatterns.psiElement(TwigTokenTypes.RBRACE)))
            .afterLeafSkipping(PlatformPatterns.or(elementPatterns), PlatformPatterns.psiElement(TwigTokenTypes.COMMA));
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
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .withParent(PlatformPatterns.psiElement().withText(PlatformPatterns.string().matches("\\{%\\s+form_theme.*")))
            .withLanguage(TwigLanguage.INSTANCE);
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
        return PlatformPatterns
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
    }

    /**
     * {% guard function importmap %}
     * {% guard filter upper %}
     * {% guard test even %}
     *
     * Pattern for the callable name after "guard" type keyword
     */
    public static ElementPattern<PsiElement> getGuardCallablePattern() {
        return PlatformPatterns
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
    }

    /**
     * Matches named argument identifiers in Twig filters and functions
     * For completion: {{ value|filter(param<caret>) }}
     * For navigation: {{ value|filter(param_name<caret>: 'value') }}
     */
    public static ElementPattern<PsiElement> getNamedArgumentNamePattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE)
                ),
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.COMMA)
                )
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    /**
     * Matches string values after named argument colon
     * {{ value|filter(param_name: '<caret>') }}
     */
    public static ElementPattern<PsiElement> getNamedArgumentValuePattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.COLON)
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }
}
