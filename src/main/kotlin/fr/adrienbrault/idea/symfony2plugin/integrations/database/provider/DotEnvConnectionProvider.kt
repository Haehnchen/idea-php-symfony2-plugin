package fr.adrienbrault.idea.symfony2plugin.integrations.database.provider

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig
import java.util.regex.Pattern

/**
 * Provides database connection configurations by parsing DATABASE_URL from .env files.
 *
 * Supports Symfony's standard DATABASE_URL format:
 *   mysql://user:pass@host:port/database
 *   postgresql://user:pass@host:port/database
 *   sqlite:///path/to/database.db
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class DotEnvConnectionProvider {

    fun getConnectionConfigs(project: Project): List<DatabaseConnectionConfig> {
        val configs = mutableListOf<DatabaseConnectionConfig>()
        val envVars = DotEnvUtil.getEnvironmentVariablesWithValues(project)

        val databaseUrl = envVars["DATABASE_URL"]
        if (!databaseUrl.isNullOrBlank()) {
            configs.addAll(parseUrl(databaseUrl, "default", project, envVars))
        }

        for ((key, value) in envVars) {
            if (key == "DATABASE_URL") continue

            val connectionName = when {
                key.startsWith("DATABASE_URL_") -> key.removePrefix("DATABASE_URL_").lowercase()
                key.endsWith("_DATABASE_URL") -> key.removeSuffix("_DATABASE_URL").lowercase()
                else -> null
            }

            if (connectionName != null && value.isNotBlank()) {
                configs.addAll(parseUrl(value, connectionName, project, envVars))
            }
        }

        configs.addAll(scanSqliteDirectory(project))

        return uniqueByPath(configs)
    }

    private fun parseUrl(url: String, name: String, project: Project, envVars: Map<String, String>): List<DatabaseConnectionConfig> {
        val matcher = DB_URL_PATTERN.matcher(url)
        if (matcher.matches()) {
            val driver = normalizeDriver(matcher.group(1))
            val username = emptyToNull(matcher.group(2))
            val password = emptyToNull(matcher.group(3))
            val host = matcher.group(4)
            val port = matcher.group(5)?.toInt() ?: getDefaultPort(driver)
            val database = matcher.group(6).replace(Regex("\\?.*$"), "")

            return listOf(DatabaseConnectionConfig(name, driver, host, port, database, username, password))
        }

        val sqliteMatcher = SQLITE_URL_PATTERN.matcher(url)
        if (sqliteMatcher.matches()) {
            return resolveSqlitePlaceholders(sqliteMatcher.group(1), name, project)
        }

        return emptyList()
    }

    /**
     * Resolve Symfony placeholders in a SQLite path.
     * Replaces %kernel.project_dir% with the project root, then tries
     * %kernel.environment% with prod/dev/test/stage. Skips any result
     * that still contains unresolved %placeholder%.
     */
    private fun resolveSqlitePlaceholders(rawPath: String, name: String, project: Project): List<DatabaseConnectionConfig> {
        val projectDirPath = getProjectVirtualDir(project)?.path

        var path = rawPath
        if (projectDirPath != null) {
            path = path.replace("%kernel.project_dir%", projectDirPath)
        }

        if (!PLACEHOLDER_PATTERN.matcher(path).find()) {
            return listOf(DatabaseConnectionConfig(name, "sqlite", null, null, path, null, null))
        }

        val results = mutableListOf<DatabaseConnectionConfig>()
        for (env in arrayOf("prod", "dev", "test", "stage")) {
            val resolved = path.replace("%kernel.environment%", env)
            if (!PLACEHOLDER_PATTERN.matcher(resolved).find()) {
                val connName = if (results.isEmpty()) name else "$name ($env)"
                results.add(DatabaseConnectionConfig(connName, "sqlite", null, null, resolved, null, null))
            }
        }
        return results
    }

    /**
     * Scan the project's var/ directory for any .db files.
     * This is independent of DATABASE_URL parsing — it discovers SQLite databases
     * that exist on disk regardless of how they are referenced in configuration.
     */
    private fun scanSqliteDirectory(project: Project): List<DatabaseConnectionConfig> {
        val projectDir = getProjectVirtualDir(project) ?: return emptyList()
        val varDir = projectDir.findChild("var")?.takeIf { it.isDirectory } ?: return emptyList()

        return varDir.children
            .filter { !it.isDirectory && it.name.endsWith(".db") && it.isValid }
            .map { DatabaseConnectionConfig(it.name.removeSuffix(".db"), "sqlite", null, null, it.path, null, null) }
    }

    @Suppress("DEPRECATION")
    private fun getProjectVirtualDir(project: Project): VirtualFile? {
        project.baseDir?.let { return it }
        val basePath = project.basePath ?: return null
        return LocalFileSystem.getInstance().findFileByPath(basePath)
    }

    private fun uniqueByPath(configs: List<DatabaseConnectionConfig>): List<DatabaseConnectionConfig> {
        val seen = linkedSetOf<String>()
        return configs.filter { seen.add(it.database ?: "") }
    }

    private fun normalizeDriver(driver: String): String = when (driver.lowercase()) {
        "mysql", "mariadb" -> "mysql"
        "postgresql", "pgsql", "postgres" -> "postgresql"
        "sqlite" -> "sqlite"
        else -> driver
    }

    private fun getDefaultPort(driver: String): Int = when (driver.lowercase()) {
        "mysql" -> 3306
        "postgresql" -> 5432
        else -> 0
    }

    private fun emptyToNull(value: String?): String? = if (value.isNullOrEmpty()) null else value

    companion object {
        // Pattern: driver://user:pass@host:port/database
        private val DB_URL_PATTERN: Pattern = Pattern.compile(
            "^(mysql|mariadb|postgresql|pgsql|postgres|sqlite)://(?:([^:@]*):?([^@]*)@)?([^:/]+)(?::(\\d+))?/(.+)$"
        )

        // SQLite pattern: sqlite:///path or sqlite:///:memory:
        private val SQLITE_URL_PATTERN: Pattern = Pattern.compile(
            "^sqlite:///(.+)$"
        )

        private val PLACEHOLDER_PATTERN: Pattern = Pattern.compile("%[^%]+%")
    }
}
