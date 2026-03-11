package fr.adrienbrault.idea.symfony2plugin.tests.integrations.database

import fr.adrienbrault.idea.symfony2plugin.integrations.database.provider.DotEnvConnectionProvider
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DotEnvConnectionProvider
 */
class DotEnvConnectionProviderTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testParseMysqlDatabaseUrl() {
        myFixture.addFileToProject(".env", "DATABASE_URL=mysql://root:secret@127.0.0.1:3306/symfony_db\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)

        val config = configs[0]
        assertEquals("default", config.name)
        assertEquals("mysql", config.driver)
        assertEquals("127.0.0.1", config.host)
        assertEquals(3306, config.port)
        assertEquals("symfony_db", config.database)
        assertEquals("root", config.username)
        assertEquals("secret", config.password)
    }

    fun testParsePostgresqlDatabaseUrl() {
        myFixture.addFileToProject(".env", "DATABASE_URL=postgresql://user:pass@localhost:5432/mydb\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)

        val config = configs[0]
        assertEquals("default", config.name)
        assertEquals("postgresql", config.driver)
        assertEquals("localhost", config.host)
        assertEquals(5432, config.port)
        assertEquals("mydb", config.database)
        assertEquals("user", config.username)
        assertEquals("pass", config.password)
    }

    fun testParsePostgresAliasDatabaseUrl() {
        myFixture.addFileToProject(".env", "DATABASE_URL=postgres://user:pass@localhost/mydb\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)
        assertEquals("postgresql", configs[0].driver)
    }

    fun testParseMysqlWithoutPort() {
        myFixture.addFileToProject(".env", "DATABASE_URL=mysql://root:@localhost/symfony\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)

        val config = configs[0]
        assertEquals("mysql", config.driver)
        assertEquals("localhost", config.host)
        assertEquals(3306, config.port) // default port
        assertEquals("symfony", config.database)
    }

    fun testSqlitePlainPath() {
        myFixture.addFileToProject(".env", "DATABASE_URL=sqlite:////var/data/app.db\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)
        assertEquals("sqlite", configs[0].driver)
        assertTrue(configs[0].database!!.endsWith("app.db"))
    }

    fun testSqliteWithKernelProjectDirAndEnvironmentPlaceholders() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=\"sqlite:///%kernel.project_dir%/var/data_%kernel.environment%.db\"\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        // All four environments (prod/dev/test/stage) are resolved since no file-existence check
        assertEquals(4, configs.size)
        for (config in configs) {
            assertEquals("sqlite", config.driver)
            assertNotNull(config.database)
            assertFalse("Path should have no unresolved placeholders: ${config.database}",
                config.database!!.contains("%"))
        }
    }

    fun testSqliteWithUnresolvablePlaceholderIsSkipped() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=sqlite:///%kernel.project_dir%/var/%unknown.param%/app.db\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        for (config in configs) {
            if (config.database != null) {
                assertFalse("Unresolved placeholder leaked into config: ${config.database}",
                    config.database.contains("%"))
            }
        }
    }

    fun testParseQuotedDatabaseUrl() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=\"postgresql://app:!ChangeMe!@127.0.0.1:5432/app?serverVersion=16&charset=utf8\"\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(1, configs.size)

        val config = configs[0]
        assertEquals("postgresql", config.driver)
        assertEquals("127.0.0.1", config.host)
        assertEquals(5432, config.port)
        assertEquals("app", config.database)
        assertEquals("app", config.username)
        assertEquals("!ChangeMe!", config.password)
    }

    fun testEmptyEnvFileReturnsNoConfigs() {
        myFixture.addFileToProject(".env", "APP_ENV=dev\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertTrue(configs.isEmpty())
    }

    fun testSecondaryConnectionViaPrefix() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=mysql://root:@localhost/main\n" +
            "DATABASE_URL_SECONDARY=postgresql://user:pass@db2:5432/analytics\n")

        val configs = DotEnvConnectionProvider().getConnectionConfigs(project)

        assertEquals(2, configs.size)
        assertTrue(configs.any { it.name == "default" && it.driver == "mysql" })
        assertTrue(configs.any { it.name == "secondary" && it.driver == "postgresql" })
    }
}
