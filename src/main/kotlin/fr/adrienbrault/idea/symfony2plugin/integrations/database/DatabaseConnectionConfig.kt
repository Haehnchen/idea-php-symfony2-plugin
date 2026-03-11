package fr.adrienbrault.idea.symfony2plugin.integrations.database

/**
 * Represents a database connection configuration parsed from Symfony project files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
data class DatabaseConnectionConfig(
    val name: String,
    val driver: String,
    val host: String?,
    val port: Int?,
    val database: String?,
    val username: String?,
    val password: String?,
)
