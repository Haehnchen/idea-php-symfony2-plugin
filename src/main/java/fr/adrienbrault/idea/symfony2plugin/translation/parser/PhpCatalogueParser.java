package fr.adrienbrault.idea.symfony2plugin.translation.parser;

import com.jetbrains.php.lang.lexer.PhpLexer;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal recursive descent parser for Symfony-generated translation catalogue files.
 *
 * Uses PhpLexer for proper PHP tokenization (no project, no read action, no PSI overhead),
 * then builds a small AST (strings, arrays, new-expressions) sufficient to extract
 * translation domains and keys from compiled catalogue files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpCatalogueParser {

    // --- AST nodes ---

    sealed interface Node permits StringNode, ArrayNode, NewNode, OtherNode {}

    record StringNode(@NotNull String value) implements Node {}

    record ArrayEntry(@NotNull String key, @NotNull Node value) {}

    record ArrayNode(@NotNull List<ArrayEntry> entries) implements Node {}

    record NewNode(@NotNull String className, @NotNull List<Node> args) implements Node {}

    /** Placeholder for anything we don't care about (numbers, variables, etc.) */
    record OtherNode() implements Node {}

    private static final OtherNode OTHER = new OtherNode();

    // --- Public API ---

    @NotNull
    static List<NewNode> findNewExpressions(@NotNull String content) {
        var tokens = tokenize(content);
        return new Parser(tokens).parseAll();
    }

    // --- Token abstraction ---

    private enum T {
        STRING, ARRAY_KW, OPEN_PAREN, CLOSE_PAREN, OPEN_BRACKET, CLOSE_BRACKET,
        COMMA, FAT_ARROW, NEW_KW, IDENT, EOF
    }

    private record Token(@NotNull T type, @NotNull String value) {}

    /**
     * Tokenizes PHP source using PhpLexer and maps the relevant token types to our minimal T enum.
     * PhpLexer handles comments, whitespace, escape sequences, and all PHP edge cases — everything
     * we omit here is simply skipped.
     */
    @NotNull
    private static List<Token> tokenize(@NotNull String src) {
        PhpLexer lexer = new PhpLexer(false);
        lexer.start(src);

        List<Token> out = new ArrayList<>();

        while (lexer.getTokenType() != null) {
            var type = lexer.getTokenType();
            String text = lexer.getTokenText();

            if (type == PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE) {
                // PhpLexer emits the string content as a separate token (without surrounding quotes).
                // Only \' and \\ need unescaping inside single-quoted PHP strings.
                String content = text.replace("\\'", "'").replace("\\\\", "\\");
                out.add(new Token(T.STRING, content));
            } else if (type == PhpTokenTypes.STRING_LITERAL) {
                // Double-quoted content token — no interpolation in generated catalogue files.
                out.add(new Token(T.STRING, text));
            } else if (type == PhpTokenTypes.kwNEW) {
                out.add(new Token(T.NEW_KW, text));
            } else if (type == PhpTokenTypes.IDENTIFIER || type == PhpTokenTypes.NAMESPACE_RESOLUTION) {
                // Both plain names and namespace separators (\) are collected as IDENT so the
                // parser can join them into a fully-qualified class name (e.g. \Symfony\...\MessageCatalogue)
                out.add(new Token(T.IDENT, text));
            } else if (type == PhpTokenTypes.opCOMMA) {
                out.add(new Token(T.COMMA, text));
            } else if (type == PhpTokenTypes.opHASH_ARRAY) {
                out.add(new Token(T.FAT_ARROW, text));
            } else if (type == PhpTokenTypes.kwARRAY) {
                out.add(new Token(T.ARRAY_KW, text));
            } else if (type == PhpTokenTypes.chLPAREN) {
                out.add(new Token(T.OPEN_PAREN, text));
            } else if (type == PhpTokenTypes.chRPAREN) {
                out.add(new Token(T.CLOSE_PAREN, text));
            } else if (type == PhpTokenTypes.chLBRACKET) {
                out.add(new Token(T.OPEN_BRACKET, text));
            } else if (type == PhpTokenTypes.chRBRACKET) {
                out.add(new Token(T.CLOSE_BRACKET, text));
            }
            // Whitespace, comments, operators, variables, etc. are intentionally skipped

            lexer.advance();
        }

        out.add(new Token(T.EOF, ""));
        return out;
    }

    // --- Recursive descent parser ---

    private static class Parser {
        private final List<Token> tokens;
        private int pos;

        Parser(@NotNull List<Token> tokens) {
            this.tokens = tokens;
        }

        @NotNull
        List<NewNode> parseAll() {
            List<NewNode> result = new ArrayList<>();
            while (!at(T.EOF)) {
                if (at(T.NEW_KW)) {
                    NewNode node = parseNew();
                    if (node != null) {
                        result.add(node);
                    }
                } else {
                    advance();
                }
            }
            return result;
        }

        @Nullable
        private NewNode parseNew() {
            consume(T.NEW_KW);

            // Collect the full class name: may be simple ("MessageCatalogue") or FQN
            // ("\Symfony\Component\Translation\MessageCatalogue") — consume IDENT tokens
            // (including namespace separators, which are also mapped to IDENT) until "("
            StringBuilder className = new StringBuilder();
            while (at(T.IDENT)) {
                className.append(advance().value());
            }

            if (className.isEmpty() || !at(T.OPEN_PAREN)) {
                return null;
            }

            consume(T.OPEN_PAREN);
            List<Node> args = parseArgList(T.CLOSE_PAREN);
            consume(T.CLOSE_PAREN);
            return new NewNode(className.toString(), args);
        }

        @NotNull
        private List<Node> parseArgList(@NotNull T closeToken) {
            List<Node> args = new ArrayList<>();
            while (!at(closeToken) && !at(T.EOF)) {
                args.add(parseValue());
                if (at(T.COMMA)) {
                    consume(T.COMMA);
                }
            }
            return args;
        }

        @NotNull
        private Node parseValue() {
            if (at(T.STRING)) {
                return new StringNode(advance().value());
            }
            if (at(T.ARRAY_KW)) {
                consume(T.ARRAY_KW);
                consume(T.OPEN_PAREN);
                ArrayNode arr = parseArrayEntries(T.CLOSE_PAREN);
                consume(T.CLOSE_PAREN);
                return arr;
            }
            if (at(T.OPEN_BRACKET)) {
                consume(T.OPEN_BRACKET);
                ArrayNode arr = parseArrayEntries(T.CLOSE_BRACKET);
                consume(T.CLOSE_BRACKET);
                return arr;
            }
            if (at(T.NEW_KW)) {
                NewNode node = parseNew();
                return node != null ? node : OTHER;
            }
            advance();
            return OTHER;
        }

        @NotNull
        private ArrayNode parseArrayEntries(@NotNull T closeToken) {
            List<ArrayEntry> entries = new ArrayList<>();
            while (!at(closeToken) && !at(T.EOF)) {
                Node keyNode = parseValue();
                if (at(T.FAT_ARROW)) {
                    consume(T.FAT_ARROW);
                    Node value = parseValue();
                    if (keyNode instanceof StringNode sk) {
                        entries.add(new ArrayEntry(sk.value(), value));
                    }
                }
                if (at(T.COMMA)) {
                    consume(T.COMMA);
                }
            }
            return new ArrayNode(entries);
        }

        private boolean at(@NotNull T type) { return tokens.get(pos).type() == type; }

        @NotNull
        private Token advance() {
            Token t = tokens.get(pos);
            if (t.type() != T.EOF) pos++;
            return t;
        }

        private void consume(@NotNull T type) { if (at(type)) advance(); }
    }
}
