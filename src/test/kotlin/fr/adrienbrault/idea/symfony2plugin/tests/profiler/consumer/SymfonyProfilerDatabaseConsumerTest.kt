package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerDatabaseQueryGroup
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SymfonyProfilerDatabaseConsumerTest {
    @Test
    fun `reads the existing raw and GZIP profiler fixtures`() {
        val raw = readExistingFixture("748f72-gzip-profiler-raw")
        val gzip = readExistingFixture("748f72-gzip-profiler")

        val rawDatabase = SymfonyProfilerDatabaseConsumer.read(SymfonyProfilerProfile.read(raw))
        val gzipDatabase = SymfonyProfilerDatabaseConsumer.read(SymfonyProfilerProfile.read(gzip))

        assertEquals(rawDatabase, gzipDatabase)
        assertEquals(0, rawDatabase.queryCount)
        assertTrue(rawDatabase.queryGroups.isEmpty())
    }

    @Test
    fun `collects grouped Doctrine query data without serializing parameters`() {
        val profile = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-db.gz"))
        val actual = SymfonyProfilerDatabaseConsumer.read(profile)

        assertEquals(3, actual.queryCount)
        assertEquals(12.34, actual.totalTimeMs)
        assertEquals(listOf("default", "analytics"), actual.connections)
        assertEquals(1, actual.duplicateQueryCount)
        assertEquals(
            listOf(
                SymfonyProfilerDatabaseQueryGroup(
                    sql = "SELECT * FROM users WHERE id = ?",
                    count = 2,
                    totalTimeMs = 10.0,
                    averageTimeMs = 5.0,
                ),
                SymfonyProfilerDatabaseQueryGroup(
                    sql = "UPDATE users SET last_seen = ? WHERE id = ?",
                    count = 1,
                    totalTimeMs = 2.34,
                    averageTimeMs = 2.34,
                ),
            ),
            actual.queryGroups.map { it.copy(stackTraces = emptyList()) },
        )

        assertEquals(actual.queryCount, actual.queryGroups.sumOf { it.stackTraces.size })
        val selectTraces = actual.queryGroups[0].stackTraces
        assertEquals(2, selectTraces.size)
        assertEquals(selectTraces[0], selectTraces[1])
        assertEquals(12, selectTraces[0].size)
        assertEquals("/app/vendor/doctrine/orm/src/Query/Exec/FinalizedSelectExecutor.php", selectTraces[0][0].file)
        assertEquals(30, selectTraces[0][0].line)
        assertEquals("Doctrine\\DBAL\\Connection", selectTraces[0][0].className)
        assertEquals("executeQuery", selectTraces[0][0].function)
        assertEquals("->", selectTraces[0][0].callType)
        assertEquals("/app/src/Service/ExampleService.php", selectTraces[0][4].file)
        assertEquals("App\\Repository\\ExampleRepository", selectTraces[0][4].className)
        assertEquals("findOne", selectTraces[0][4].function)
        assertEquals("Symfony\\Component\\HttpKernel\\HttpKernel", selectTraces[0][7].className)
        assertEquals("require_once", selectTraces[0].last().function)
        assertTrue(selectTraces[0].mapNotNull { it.className }.all { "\\" in it })

        val updateTrace = actual.queryGroups[1].stackTraces.single()
        assertEquals(12, updateTrace.size)
        assertEquals("Doctrine\\ORM\\Persisters\\Entity\\BasicEntityPersister", updateTrace[1].className)
        assertEquals("updateOne", updateTrace[4].function)
        assertEquals("update", updateTrace[6].function)
        assertFalse("2026-08-13" in actual.queryGroups.toString())
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }

    private fun readExistingFixture(name: String): ByteArray = Files.readAllBytes(
        Path.of("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/profiler/fixtures", name),
    )
}
