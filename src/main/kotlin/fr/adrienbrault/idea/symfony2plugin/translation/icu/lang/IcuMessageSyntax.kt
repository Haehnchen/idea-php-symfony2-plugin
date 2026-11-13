package fr.adrienbrault.idea.symfony2plugin.translation.icu.lang

object IcuMessageSyntax {
    val PLURAL_TYPES = setOf("plural", "selectordinal")
    val COMPLEX_TYPES = PLURAL_TYPES + "select"
    val ARGUMENT_TYPES = listOf("number", "date", "time", "plural", "select", "selectordinal", "spellout", "ordinal", "duration", "choice")
    val PLURAL_SELECTORS = listOf("zero", "one", "two", "few", "many", "other")
    val NUMBER_STYLES = listOf("integer", "currency", "percent", "::")
    val DATE_STYLES = listOf("short", "medium", "long", "full")
}
