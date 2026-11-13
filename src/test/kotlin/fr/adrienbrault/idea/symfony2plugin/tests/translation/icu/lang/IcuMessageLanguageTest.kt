package fr.adrienbrault.idea.symfony2plugin.tests.translation.icu.lang

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.Language
import com.intellij.lang.LanguageBraceMatching
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.psi.TokenType
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageLanguage
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageSyntaxHighlighter
import fr.adrienbrault.idea.symfony2plugin.translation.icu.lang.IcuMessageTokenTypes

class IcuMessageLanguageTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testMessagesOfIntlIcuYamlFilesAreInjected() {
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.yaml", "foo: '{su<caret>m}'"))
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.yaml", "foo: Hello {na<caret>me}"))
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.yaml", "home:\n    foo: \"a {b<caret>}\""))
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.yaml", "foo: |\n    Hello {na<caret>me}"))
    }

    fun testYamlKeysAndOtherTranslationFilesAreNotInjected() {
        assertNull(injectedLanguageAt("messages+intl-icu.en.yaml", "fo<caret>o: bar"))
        assertNull(injectedLanguageAt("messages.en.yaml", "foo: '{su<caret>m}'"))
    }

    fun testValuesOfIntlIcuPhpTranslationFilesAreInjected() {
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.php", "<?php return ['k' => 'Hello {na<caret>me}'];"))
        assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.php", "<?php return ['home' => ['k' => 'Hello {na<caret>me}']];"))
        assertNull(injectedLanguageAt("messages+intl-icu.en.php", "<?php return ['k<caret>ey' => 'Hello'];"))
        assertNull(injectedLanguageAt("messages+intl-icu.en.php", "<?php \$a = ['k' => '{na<caret>me}'];"))
        assertNull(injectedLanguageAt("messages.en.php", "<?php return ['k' => 'Hello {na<caret>me}'];"))
    }

    fun testSourceAndTargetOfIntlIcuXliffFilesAreInjected() {
        val xliff = """
            <?xml version="1.0"?>
            <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2"><file source-language="en" datatype="plaintext" original="file.ext"><body>
            <trans-unit id="a"><source>SOURCE</source><target>TARGET</target><note>NOTE</note></trans-unit>
            </body></file></xliff>
        """.trimIndent()

        withXliffAsXml {
            assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.en.xlf", xliff.replace("SOURCE", "{na<caret>me}")))
            assertEquals(IcuMessageLanguage, injectedLanguageAt("messages+intl-icu.fr.xlf", xliff.replace("TARGET", "Hello {na<caret>me}")))
            assertNull(injectedLanguageAt("messages+intl-icu.de.xlf", xliff.replace("NOTE", "{na<caret>me}")))
        }
    }

    fun testValidMessagesHaveNoErrors() {
        assertEquals(
            emptyList<String>(),
            errors(
                """
                a: '{sum, number} contributions on {label}: {reviews, number} reviews'
                b: '{n, plural, offset:1 =0 {nobody} one {# item by {author}} other {# items}}'
                c: '{g, select, female {She} other {They}} {d, date, short} {p, number, ::currency/EUR} {c, choice, 0#none|1#one}'
                d: "It''s '{literal}' and <0>{hours}h</0>"
                e: '{place, selectordinal, one {#st} other {#th}}'
                """.trimIndent(),
            ),
        )
    }

    fun testSyntaxErrorsAreReported() {
        // an error at the end of the message is shown on the closing quote of the YAML string
        assertEquals(listOf("'}' expected (''')"), errors("a: '{sum'"))
        assertEquals(listOf("Unmatched '}' ('}')"), errors("a: 'closed}'"))
        assertEquals(listOf("Argument name expected ('}')"), errors("a: '{}'"))
        assertEquals(listOf("Invalid argument name ('.')"), errors("a: '{user.name}'"))
        assertEquals(listOf("',' or '}' expected ('plural')"), errors("a: '{n plural}'"))
        assertEquals(listOf("Argument type expected ('}')"), errors("a: '{n,}'"))
        assertEquals(listOf("Option expected (' ')"), errors("a: '{n, plural, }'"))
        assertEquals(listOf("'{' expected ('}')"), errors("a: '{n, plural, other}'"))
        assertEquals(listOf("Unclosed quote in argument style (''abc}')"), errors("a: \"{n, number, 'abc}\""))
    }

    fun testSemanticErrorsAreReported() {
        assertEquals(listOf("Missing 'other' option in plural ('plural')"), errors("a: '{n, plural, one {x}}'"))
        assertEquals(listOf("Missing 'other' option in select ('select')"), errors("a: '{g, select, male {x}}'"))
        assertEquals(listOf("Unknown argument type 'nombre' ('nombre')"), errors("a: '{x, nombre}'"))
    }

    fun testPhpMessagesAreCheckedWithoutTheirQuotes() {
        assertEquals(listOf("Unknown argument type 'nombre' ('nombre')"), errors("<?php return ['a' => '{x, nombre}'];", "messages+intl-icu.en.php"))
    }

    fun testOtherTranslationFilesAreNotChecked() {
        assertEquals(emptyList<String>(), errors("a: '{sum'", "messages.en.yaml"))
    }

    fun testArgumentTypeCompletion() {
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{sum, <caret>}'", "number", "date", "time", "plural", "select", "selectordinal", "spellout", "ordinal", "duration")
        assertCompletionContains("messages+intl-icu.en.php", "<?php return ['a' => '{sum, <caret>}'];", "number", "plural")
    }

    fun testOptionSelectorCompletion() {
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{n, plural, <caret>}'", "offset:", "zero", "one", "two", "few", "many", "other")
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{n, plural, one {x} <caret>}'", "other")
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{g, select, female {x} <caret>}'", "other")
        assertCompletionNotContains("messages+intl-icu.en.yaml", "a: '{g, select, female {x} <caret>}'", "one", "offset:")
    }

    fun testArgumentStyleCompletion() {
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{p, number, <caret>}'", "integer", "currency", "percent")
        assertCompletionContains("messages+intl-icu.en.yaml", "a: '{d, date, <caret>}'", "short", "medium", "long", "full")
        assertCompletionNotContains("messages+intl-icu.en.yaml", "a: '{d, time, <caret>}'", "integer")
    }

    fun testBracesAreMatched() {
        val pairs = LanguageBraceMatching.INSTANCE.forLanguage(IcuMessageLanguage)!!.pairs
        assertTrue(pairs.any { it.leftBraceType == IcuMessageTokenTypes.LBRACE && it.rightBraceType == IcuMessageTokenTypes.RBRACE && it.isStructural })
    }

    fun testEverySyntaxTokenIsHighlighted() {
        val highlighter = IcuMessageSyntaxHighlighter()
        val syntaxTokens = with(IcuMessageTokenTypes) {
            listOf(QUOTED_TEXT, ESCAPED_QUOTE, HASH, LBRACE, RBRACE, COMMA, ARGUMENT_NAME, ARGUMENT_TYPE, ARGUMENT_STYLE, UNCLOSED_QUOTE, OFFSET, NUMBER, SELECTOR)
        }

        for (tokenType in syntaxTokens + TokenType.BAD_CHARACTER) {
            assertTrue(tokenType.toString(), highlighter.getTokenHighlights(tokenType).isNotEmpty())
        }
    }

    // with the caret in an injection, the fixture file is the injected one
    private fun injectedLanguageAt(fileName: String, content: String): Language? {
        myFixture.configureByText(fileName, content)
        val manager = InjectedLanguageManager.getInstance(project)
        return manager.findInjectedElementAt(manager.getTopLevelFile(myFixture.file), content.indexOf("<caret>"))?.containingFile?.language
    }

    private fun withXliffAsXml(block: () -> Unit) {
        WriteAction.runAndWait<RuntimeException> { FileTypeManagerEx.getInstanceEx().associateExtension(XmlFileType.INSTANCE, "xlf") }
        try {
            block()
        } finally {
            WriteAction.runAndWait<RuntimeException> { FileTypeManager.getInstance().removeAssociatedExtension(XmlFileType.INSTANCE, "xlf") }
        }
    }

    private fun errors(content: String, fileName: String = "messages+intl-icu.en.yaml"): List<String> {
        myFixture.configureByText(fileName, content)
        return myFixture.doHighlighting(HighlightSeverity.ERROR).map { if (it.text.isEmpty()) it.description else "${it.description} ('${it.text}')" }
    }
}
