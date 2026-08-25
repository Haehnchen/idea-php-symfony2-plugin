package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestDetailsCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.LocalProfilerRequest
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerTextRenderer

class SymfonyProfilerRequestDetailsCollectorTest : McpCollectorTestCase() {
    fun testLatestLoadsNewestKnownProfilerRequest() {
        val profilerIndex = TestProfilerIndex(
            resourceFixture("symfony-profiler-db.gz"),
            listOf(
                profilerRequest("abcdef", 1_723_557_600),
                profilerRequest("fedcba", 1_723_557_500),
            ),
        )

        SymfonyProfilerRequestDetailsCollector(profilerIndex).collect(" latest ")

        assertEquals("abcdef", profilerIndex.rawProfileHash)
    }

    fun testLatestFailsWithoutKnownProfilerRequests() {
        try {
            SymfonyProfilerRequestDetailsCollector(TestProfilerIndex(null)).collect("latest")
            fail("Expected latest to fail without known profiler requests")
        } catch (exception: Throwable) {
            assertTrue("No profiler requests are available to resolve 'latest'" in exception.message.orEmpty())
        }
    }

    fun testRejectsUnknownHashAlias() {
        try {
            SymfonyProfilerRequestDetailsCollector(TestProfilerIndex(null)).collect("newest")
            fail("Expected unsupported hash alias to fail validation")
        } catch (exception: Throwable) {
            assertTrue("hash must be 'latest' or a 6-64 character hexadecimal profiler token" in exception.message.orEmpty())
        }
    }

    fun testHashOnlyReturnsCompactOverview() {
        val text = fixtureCollector("symfony-profiler-db.gz").collect("abcdef")
        val profiledAt = ProfilerTextRenderer.formatTimestamp(1_723_557_600)

        assertTrue(
            text.startsWith(
                "Symfony Profiler Request - profile01 ($profiledAt) http://example.test/orders/42 200 GET\n\n",
            ),
        )
        assertFalse(text.lineSequence().any { it.startsWith("- Token:") })
        assertFalse(text.lineSequence().any { it.startsWith("- Method:") })
        assertFalse(text.lineSequence().any { it.startsWith("- URL:") })
        assertFalse(text.lineSequence().any { it.startsWith("- Status:") })
        assertTrue("## Collector: db" in text)
        assertTrue("### Top 3 query groups" in text)
        assertTrue("- Specialized: db" in text)
    }

    fun testCollectorCanBeSelected() {
        specializedFixtures().forEach { (fixture, hash, collector) ->
            val text = fixtureCollector(fixture).collect(hash, collector, page = 99)
            assertTrue("Missing selected collector '$collector'", "Collector: $collector" in text)
        }
    }

    fun testTimeOverviewIncludesMemoryPeak() {
        val text = fixtureCollector("symfony-profiler-time.gz").collect("fedcba")

        assertTrue("## Collector: time" in text)
        assertTrue("### Memory" in text)
        assertTrue("- Memory peak: 6.00 MiB" in text)
        assertTrue("- PHP memory limit: 128.00 MiB" in text)
        assertTrue("- Threshold: 1.00 ms" in text)
    }

    fun testTranslationOverviewIncludesMessageStatesWithoutCsvRows() {
        val text = fixtureCollector("symfony-profiler-translation.gz").collect("7a1a7e")

        assertTrue("## Collector: translation" in text)
        assertTrue("- Defined messages: 2" in text)
        assertTrue("- Missing messages: 2" in text)
        assertTrue("- Fallback messages: 1" in text)
        assertFalse("state,locale,fallback_locale,domain,count,id,translation" in text)
        assertFalse("checkout.missing_title" in text)
    }

    fun testTranslationDetailsRenderCsvWithoutParameters() {
        val text = fixtureCollector("symfony-profiler-translation.gz")
            .collect("7a1a7e", "translation")

        assertTrue("### Messages (CSV)" in text)
        assertTrue("state,locale,fallback_locale,domain,count,id,translation" in text)
        assertTrue("missing,en,,checkout,2,checkout.missing_title,checkout.missing_title" in text)
        assertTrue("fallback,en,fr,messages,1,account.title,Compte" in text)
        assertFalse("%name%" in text)
        assertFalse("Example" in text)
    }

    fun testOverviewSeparatesCollectorSectionsWithBlankLines() {
        val text = SymfonyProfilerRequestDetailsCollector(
            TestProfilerIndex(syntheticRequestAndTimeProfile()),
        ).collect("abc123")

        assertTrue("- Content type: application/json\n\n## Collector: time" in text)
        assertTrue("No timing events recorded.\n\n## Available collectors" in text)
    }

    fun testTimeDetailsApplySymfonyThreshold() {
        val text = fixtureCollector("symfony-profiler-time.gz").collect("fedcba", "time")

        assertTrue("`at.threshold`" in text)
        assertFalse("`below.threshold`" in text)
    }

    fun testRawCollectorNamesAreNotRepeatedAsAdditionalCollectors() {
        specializedFixtures().forEach { (fixture, hash, collector) ->
            val text = fixtureCollector(fixture).collect(hash)

            assertTrue("Missing specialized collector '$collector'", "- Specialized: $collector" in text)
            assertFalse(
                "Specialized collector '$collector' was repeated as additional",
                text.lineSequence()
                    .filter { it.startsWith("- Additional collectors: ") }
                    .flatMap { it.removePrefix("- Additional collectors: ").split(", ").asSequence() }
                    .any { it == collector },
            )
        }
    }

    fun testSpecializedCollectorNamesAreCaseSensitive() {
        try {
            fixtureCollector("symfony-profiler-db.gz").collect("abcdef", "DB")
            fail("Expected case-mismatched specialized collector to fail")
        } catch (exception: Throwable) {
            assertTrue("does not contain the 'DB' collector" in exception.message.orEmpty())
        }
    }

    fun testReportsCollectorMissingFromProfile() {
        try {
            fixtureCollector("symfony-profiler-db.gz").collect("abcdef", "mailer")
            fail("Expected missing collector to fail")
        } catch (exception: Throwable) {
            assertTrue("does not contain the 'mailer' collector" in exception.message.orEmpty())
            assertTrue("Available collectors: db" in exception.message.orEmpty())
        }
    }

    fun testParseFailureListsNewestTenProfilerHashesWithDates() {
        val requests = (1..12).map { index ->
            profilerRequest(index.toString(16).padStart(6, '0'), 1_723_557_600L - index)
        }
        val profilerIndex = TestProfilerIndex("invalid serialized profile".toByteArray(), requests)

        try {
            SymfonyProfilerRequestDetailsCollector(profilerIndex).collect("abcdef")
            fail("Expected invalid profiler data to fail")
        } catch (exception: Throwable) {
            val message = exception.message.orEmpty()
            val expectedHashes = requests.take(10).joinToString(", ") { request ->
                "${request.hash} (${ProfilerTextRenderer.formatTimestamp(requireNotNull(request.time))})"
            }
            assertTrue("Newest profiler hashes: $expectedHashes" in message)
            assertFalse(requests[10].hash in message)
            assertFalse(requests[11].hash in message)
        }
    }

    fun testCustomCollectorUsesSanitizedRawFallback() {
        val sensitiveNames = listOf(
            "auth",
            "key",
            "private",
            "pwd",
            "session",
            "accessKey",
            "api_key",
            "authorization",
            "authentication",
            "bearer",
            "cookie",
            "credential",
            "csrf",
            "dsn",
            "encryptionKey",
            "oauth",
            "otp",
            "passphrase",
            "passwd",
            "password",
            "privateKey",
            "secret",
            "session_attributes",
            "sessionId",
            "signingKey",
            "token",
            "xsrf",
        )
        val entries = sensitiveNames.mapIndexed { index, name -> name to phpString("sensitive-$index") } + listOf(
            "nested" to phpArray(listOf("clientSecret" to phpString("nested-sensitive"))),
            "database_url" to phpString("mysql://user:credential@example.test/app"),
            "safe_field" to phpString("visible-value"),
            "monkey" to phpString("banana"),
            "primary_key" to phpString("entity-id"),
            "public_key" to phpString("public-value"),
        )
        val collectorName = "App\\Profiler\\CustomCollector"

        val text = syntheticCollector(collectorName, entries).collect("abc123", collectorName)

        assertTrue("Collector: App\\Profiler\\CustomCollector" in text)
        assertFalse("Format:" in text)
        assertTrue("\n## Data\n\n" in text)
        sensitiveNames.forEach { name -> assertTrue("$name: ***REDACTED***" in text) }
        assertTrue("clientSecret: ***REDACTED***" in text)
        assertTrue("database_url: ***REDACTED***" in text)
        assertTrue("safe_field: visible-value" in text)
        assertTrue("monkey: banana" in text)
        assertTrue("primary_key: entity-id" in text)
        assertTrue("public_key: public-value" in text)
        assertFalse("nested-sensitive" in text)
        assertFalse("credential@example.test" in text)
        sensitiveNames.indices.forEach { index -> assertFalse("sensitive-$index" in text) }
    }

    fun testOverviewOnlyListsAdditionalCollectorWithoutRenderingItsData() {
        val collectorName = "App\\Profiler\\CustomCollector"

        val text = syntheticCollector(
            collectorName,
            listOf("safe_field" to phpString("visible-value")),
        ).collect("abc123")

        assertTrue("## Available collectors" in text)
        assertTrue("- Additional collectors: App\\Profiler\\CustomCollector" in text)
        assertFalse("visible-value" in text)
        assertFalse("Format: sanitized raw fallback" in text)
    }

    fun testCustomCollectorNameKeepsExactCase() {
        val collectorName = "App\\Profiler\\CustomCollector"
        val collector = syntheticCollector(collectorName, emptyList())

        assertTrue("Collector: $collectorName" in collector.collect("abc123", collectorName))
        try {
            collector.collect("abc123", collectorName.lowercase())
            fail("Expected case-mismatched custom collector to fail")
        } catch (exception: Throwable) {
            assertTrue("does not contain" in exception.message.orEmpty())
        }
    }

    fun testCustomCollectorWithoutDataReturnsEmptyFallback() {
        val collectorName = "App\\Profiler\\EmptyCollector"

        val text = syntheticCollectorWithoutData(collectorName).collect("abc123", collectorName)

        assertFalse("Format:" in text)
        assertTrue("\n## Data\n\n" in text)
        assertTrue("\n(empty)" in text)
    }

    fun testRawFallbackPaginatesGenericDataAroundTokenTarget() {
        val collectorName = "App\\Profiler\\LargeCollector"
        val entries = (1..205).map { index -> "field_$index" to "i:$index;" }
        val collector = syntheticCollector(collectorName, entries)

        val compactDetails = collector.collect("abc123", collectorName, page = 1)
        assertFalse("Page:" in compactDetails)
        assertTrue("field_205: 205" in compactDetails)

        val largeCollector = syntheticCollector(
            collectorName,
            (1..16).map { index -> "field_$index" to phpString("x".repeat(1_100)) },
        )

        val firstPage = largeCollector.collect("abc123", collectorName, page = 1)
        assertTrue("Page: 1 of 2" in firstPage)
        assertTrue("field_15:" in firstPage)
        assertFalse("field_16:" in firstPage)

        val secondPage = largeCollector.collect("abc123", collectorName, page = 2)
        assertTrue("Page: 2 of 2" in secondPage)
        assertTrue("field_16:" in secondPage)
        assertFalse("field_15:" in secondPage)
    }

    fun testReportsUnavailableRawProfile() {
        try {
            SymfonyProfilerRequestDetailsCollector(TestProfilerIndex(null)).collect("abcdef")
            fail("Expected unavailable raw profile to fail")
        } catch (exception: Throwable) {
            assertTrue("currently require a local Symfony profiler file" in exception.message.orEmpty())
        }
    }

    private fun fixtureCollector(fixture: String) = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(resourceFixture(fixture)),
    )

    private fun specializedFixtures() = listOf(
        Triple("symfony-profiler-request.gz", "abc123", "request"),
        Triple("symfony-profiler-time.gz", "fedcba", "time"),
        Triple("symfony-profiler-twig.gz", "c0ffee", "twig"),
        Triple("symfony-profiler-logger.gz", "10ca11", "logger"),
        Triple("symfony-profiler-translation.gz", "7a1a7e", "translation"),
        Triple("symfony-profiler-events-symfony-6.3.gz", "e71e17", "events"),
        Triple("symfony-profiler-db.gz", "abcdef", "db"),
    )

    private fun syntheticCollector(
        collectorName: String,
        dataEntries: List<Pair<String, String>>,
    ) = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(syntheticProfile(collectorName, dataEntries)),
    )

    private fun syntheticCollectorWithoutData(collectorName: String) = SymfonyProfilerRequestDetailsCollector(
        TestProfilerIndex(syntheticProfile(collectorName, null)),
    )

    private fun syntheticProfile(collectorName: String, dataEntries: List<Pair<String, String>>?): ByteArray {
        val protectedDataProperty = "\u0000*\u0000data"
        val collector = phpObject(
            collectorName,
            dataEntries?.let { listOf(protectedDataProperty to phpArray(it)) }.orEmpty(),
        )
        return phpArray(
            listOf(
                "token" to phpString("abc123"),
                "method" to phpString("GET"),
                "url" to phpString("https://example.test/profile"),
                "status_code" to "i:200;",
                "data" to phpArray(listOf(collectorName to collector)),
            ),
        ).toByteArray(Charsets.UTF_8)
    }

    private fun syntheticRequestAndTimeProfile(): ByteArray {
        val protectedDataProperty = "\u0000*\u0000data"
        val requestCollector = phpObject(
            "Symfony\\Component\\HttpKernel\\DataCollector\\RequestDataCollector",
            listOf(
                protectedDataProperty to phpArray(
                    listOf(
                        "method" to phpString("GET"),
                        "path_info" to phpString("/profile"),
                        "route" to phpString("app_profile"),
                        "status_code" to "i:200;",
                        "content_type" to phpString("application/json"),
                    ),
                ),
            ),
        )
        val timeCollector = phpObject(
            "Symfony\\Component\\HttpKernel\\DataCollector\\TimeDataCollector",
            listOf(
                protectedDataProperty to phpArray(
                    listOf(
                        "start_time" to "i:0;",
                        "events" to phpArray(emptyList()),
                        "stopwatch_installed" to "b:0;",
                    ),
                ),
            ),
        )

        return phpArray(
            listOf(
                "token" to phpString("abc123"),
                "method" to phpString("GET"),
                "url" to phpString("https://example.test/profile"),
                "status_code" to "i:200;",
                "data" to phpArray(
                    listOf(
                        "request" to requestCollector,
                        "time" to timeCollector,
                    ),
                ),
            ),
        ).toByteArray(Charsets.UTF_8)
    }

    private fun phpObject(className: String, properties: List<Pair<String, String>>): String = buildString {
        append("O:${className.toByteArray(Charsets.UTF_8).size}:\"")
        append(className)
        append("\":${properties.size}:{")
        properties.forEach { (name, value) ->
            append(phpString(name))
            append(value)
        }
        append('}')
    }

    private fun phpArray(entries: List<Pair<String, String>>): String = buildString {
        append("a:${entries.size}:{")
        entries.forEach { (key, value) ->
            append(phpString(key))
            append(value)
        }
        append('}')
    }

    private fun phpString(value: String): String = buildString {
        append("s:${value.toByteArray(Charsets.UTF_8).size}:\"")
        append(value)
        append("\";")
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }

    private fun profilerRequest(hash: String, time: Long): ProfilerRequestInterface = LocalProfilerRequest(
        "$hash,127.0.0.1,GET,http://example.test/profile,$time,parent,200".split(',').toTypedArray(),
    )

    private class TestProfilerIndex(
        private val rawProfile: ByteArray?,
        private val requests: List<ProfilerRequestInterface> = emptyList(),
    ) : ProfilerIndexInterface {
        var rawProfileHash: String? = null
            private set

        override fun getRequests(): List<ProfilerRequestInterface> = requests

        override fun getUrlForRequest(request: ProfilerRequestInterface): String? = null

        override fun getRawProfile(hash: String): ByteArray? {
            rawProfileHash = hash
            return rawProfile
        }
    }
}
