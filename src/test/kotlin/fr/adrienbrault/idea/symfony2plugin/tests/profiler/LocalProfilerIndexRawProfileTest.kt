package fr.adrienbrault.idea.symfony2plugin.tests.profiler

import fr.adrienbrault.idea.symfony2plugin.profiler.LocalProfilerIndex
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalProfilerIndexRawProfileTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `loads raw profile from Symfony hash directory`() {
        val profilerDirectory = temporaryDirectory.resolve("profiler")
        val indexFile = profilerDirectory.resolve("index.csv")
        val profileFile = profilerDirectory.resolve("cc/bb/aabbcc")
        val expected = byteArrayOf(0x1f, 0x8b.toByte(), 8, 1, 2, 3)
        Files.createDirectories(profileFile.parent)
        Files.write(indexFile, byteArrayOf())
        Files.write(profileFile, expected)

        val actual = LocalProfilerIndex(indexFile.toFile()).getRawProfile("aabbcc")

        assertArrayEquals(expected, actual)
    }

    @Test
    fun `rejects non profiler hash instead of resolving arbitrary paths`() {
        val profilerDirectory = temporaryDirectory.resolve("profiler")
        val indexFile = profilerDirectory.resolve("index.csv")
        Files.createDirectories(profilerDirectory)
        Files.write(indexFile, byteArrayOf())

        assertNull(LocalProfilerIndex(indexFile.toFile()).getRawProfile("../../etc/passwd"))
    }

    @Test
    fun `rejects raw profiles larger than parser input limit`() {
        val profilerDirectory = temporaryDirectory.resolve("profiler")
        val indexFile = profilerDirectory.resolve("index.csv")
        val profileFile = profilerDirectory.resolve("ff/ee/ddeeff")
        Files.createDirectories(profileFile.parent)
        Files.write(indexFile, byteArrayOf())
        Files.write(profileFile, ByteArray(5 * 1024 * 1024 + 1))

        assertNull(LocalProfilerIndex(indexFile.toFile()).getRawProfile("ddeeff"))
    }
}
