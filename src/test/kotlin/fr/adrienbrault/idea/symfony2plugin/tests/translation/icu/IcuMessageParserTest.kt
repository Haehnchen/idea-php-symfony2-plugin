package fr.adrienbrault.idea.symfony2plugin.tests.translation.icu

import fr.adrienbrault.idea.symfony2plugin.translation.icu.IcuMessageParser
import fr.adrienbrault.idea.symfony2plugin.translation.icu.TranslationParameter
import junit.framework.TestCase

class IcuMessageParserTest : TestCase() {
    fun testSimpleArgumentsAreTypedAfterTheirFormat() {
        assertEquals(
            listOf(
                TranslationParameter("sum", "number"),
                TranslationParameter("label", "string"),
                TranslationParameter("reviews", "number"),
                TranslationParameter("prs", "number"),
            ),
            parse("{sum, number} contributions on {label}: {reviews, number} reviews, {prs, number} pull requests"),
        )
    }

    fun testNumberAndDateFormatsWithStyles() {
        assertEquals(
            listOf(
                TranslationParameter("count", "number"),
                TranslationParameter("rank", "number"),
                TranslationParameter("price", "number"),
                TranslationParameter("day", "date"),
                TranslationParameter("at", "date"),
                TranslationParameter("length", "number"),
            ),
            parse("{count, spellout} {rank, ordinal} {price, number, ::currency/EUR} {day, date, short} {at, time, ::jmm} {length, duration}"),
        )
    }

    fun testHtmlLikeTagsAreText() {
        assertEquals(
            listOf(TranslationParameter("hours", "string"), TranslationParameter("years", "string")),
            parse("Contributing every <0>{hours}h</0>, for {years} years."),
        )
    }

    fun testPluralIsANumberAndItsSubMessagesAreScanned() {
        assertEquals(
            listOf(TranslationParameter("count", "number"), TranslationParameter("author", "string")),
            parse("{count, plural, offset:1 =0 {nobody} one {# item by {author}} other {# items}}"),
        )
    }

    fun testSelectordinalIsANumber() {
        assertEquals(
            listOf(TranslationParameter("place", "number")),
            parse("{place, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"),
        )
    }

    fun testSelectListsItsValues() {
        assertEquals(
            listOf(
                TranslationParameter("gender", "string", listOf("female", "male", "other")),
                TranslationParameter("name", "string"),
                TranslationParameter("guest", "string"),
            ),
            parse("{gender, select, female {She, {name},} male {He} other {They}} invited {guest}"),
        )
    }

    fun testFirstOccurrenceOfAnArgumentWins() {
        assertEquals(
            listOf(TranslationParameter("count", "number")),
            parse("{count, plural, other {{count} items}}"),
        )
    }

    fun testChoiceIsANumber() {
        assertEquals(listOf(TranslationParameter("n", "number")), parse("{n, choice, 0#none|1#one|1<many}"))
    }

    fun testApostrophesQuoteSyntaxCharactersOnly() {
        assertEquals(listOf(TranslationParameter("name", "string")), parse("I''m {name}"))
        assertEquals(listOf(TranslationParameter("name", "string")), parse("It's {name}"))
        assertEquals(listOf(TranslationParameter("real", "string")), parse("'{literal}' {real}"))
        assertEquals(listOf(TranslationParameter("n", "number")), parse("{n, plural, other {'#' is # and '{x}'}}"))
    }

    fun testNumberedArguments() {
        assertEquals(listOf(TranslationParameter("0", "string")), parse("Hello {0}"))
    }

    fun testMessageWithoutArguments() {
        assertEquals(emptyList<TranslationParameter>(), parse("No arguments here"))
    }

    fun testInvalidMessagesAreRejected() {
        for (message in listOf("{unclosed", "closed}", "{}", "{a, plural, one {x}", "{a, select}", "{a b}", "{user.name}", "{a, number, 'style}")) {
            assertNull(message, IcuMessageParser.getParameters(message))
        }
    }

    private fun parse(message: String): List<TranslationParameter> = IcuMessageParser.getParameters(message)!!.values.toList()
}
