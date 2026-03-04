package fr.adrienbrault.idea.symfony2plugin.tests.integrations.database;

import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.provider.DotEnvConnectionProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see DotEnvConnectionProvider
 */
public class DotEnvConnectionProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testParseMysqlDatabaseUrl() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=mysql://root:secret@127.0.0.1:3306/symfony_db\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());

        DatabaseConnectionConfig config = configs.get(0);
        assertEquals("default", config.getName());
        assertEquals("mysql", config.getDriver());
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(3306, (int) config.getPort());
        assertEquals("symfony_db", config.getDatabase());
        assertEquals("root", config.getUsername());
        assertEquals("secret", config.getPassword());
    }

    public void testParsePostgresqlDatabaseUrl() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=postgresql://user:pass@localhost:5432/mydb\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());

        DatabaseConnectionConfig config = configs.get(0);
        assertEquals("default", config.getName());
        assertEquals("postgresql", config.getDriver());
        assertEquals("localhost", config.getHost());
        assertEquals(5432, (int) config.getPort());
        assertEquals("mydb", config.getDatabase());
        assertEquals("user", config.getUsername());
        assertEquals("pass", config.getPassword());
    }

    public void testParsePostgresAliasDatabaseUrl() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=postgres://user:pass@localhost/mydb\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());
        assertEquals("postgresql", configs.get(0).getDriver());
    }

    public void testParseMysqlWithoutPort() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=mysql://root:@localhost/symfony\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());

        DatabaseConnectionConfig config = configs.get(0);
        assertEquals("mysql", config.getDriver());
        assertEquals("localhost", config.getHost());
        assertEquals(3306, (int) config.getPort()); // default port
        assertEquals("symfony", config.getDatabase());
    }

    public void testSqlitePlainPath() {
        myFixture.addFileToProject(".env", "DATABASE_URL=sqlite:////var/data/app.db\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());
        assertEquals("sqlite", configs.get(0).getDriver());
        assertTrue(configs.get(0).getDatabase().endsWith("app.db"));
    }

    public void testSqliteWithKernelProjectDirAndEnvironmentPlaceholders() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=\"sqlite:///%kernel.project_dir%/var/data_%kernel.environment%.db\"\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        // All four environments (prod/dev/test/stage) are resolved since no file-existence check
        assertEquals(4, configs.size());
        for (DatabaseConnectionConfig config : configs) {
            assertEquals("sqlite", config.getDriver());
            assertNotNull(config.getDatabase());
            assertFalse("Path should have no unresolved placeholders: " + config.getDatabase(),
                config.getDatabase().contains("%"));
        }
    }

    public void testSqliteWithUnresolvablePlaceholderIsSkipped() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=sqlite:///%kernel.project_dir%/var/%unknown.param%/app.db\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        for (DatabaseConnectionConfig config : configs) {
            if (config.getDatabase() != null) {
                assertFalse("Unresolved placeholder leaked into config: " + config.getDatabase(),
                    config.getDatabase().contains("%"));
            }
        }
    }

    public void testParseQuotedDatabaseUrl() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=\"postgresql://app:!ChangeMe!@127.0.0.1:5432/app?serverVersion=16&charset=utf8\"\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(1, configs.size());

        DatabaseConnectionConfig config = configs.get(0);
        assertEquals("postgresql", config.getDriver());
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(5432, (int) config.getPort());
        assertEquals("app", config.getDatabase());
        assertEquals("app", config.getUsername());
        assertEquals("!ChangeMe!", config.getPassword());
    }

    public void testEmptyEnvFileReturnsNoConfigs() {
        myFixture.addFileToProject(".env", "APP_ENV=dev\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertTrue(configs.isEmpty());
    }

    public void testSecondaryConnectionViaPrefix() {
        myFixture.addFileToProject(".env",
            "DATABASE_URL=mysql://root:@localhost/main\n" +
            "DATABASE_URL_SECONDARY=postgresql://user:pass@db2:5432/analytics\n");

        DotEnvConnectionProvider provider = new DotEnvConnectionProvider();
        List<DatabaseConnectionConfig> configs = provider.getConnectionConfigs(getProject());

        assertEquals(2, configs.size());
        // default connection
        assertTrue(configs.stream().anyMatch(c -> "default".equals(c.getName()) && "mysql".equals(c.getDriver())));
        // secondary connection
        assertTrue(configs.stream().anyMatch(c -> "secondary".equals(c.getName()) && "postgresql".equals(c.getDriver())));
    }
}
