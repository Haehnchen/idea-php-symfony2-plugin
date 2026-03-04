package fr.adrienbrault.idea.symfony2plugin.integrations.database;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a database connection configuration parsed from Symfony project files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DatabaseConnectionConfig {
    @NotNull private final String name;
    @NotNull private final String driver;
    @Nullable private final String host;
    @Nullable private final Integer port;
    @Nullable private final String database;
    @Nullable private final String username;
    @Nullable private final String password;

    public DatabaseConnectionConfig(@NotNull String name, @NotNull String driver,
                                    @Nullable String host, @Nullable Integer port,
                                    @Nullable String database, @Nullable String username,
                                    @Nullable String password) {
        this.name = name;
        this.driver = driver;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @NotNull
    public String getName() { return name; }

    @NotNull
    public String getDriver() { return driver; }

    @Nullable
    public String getHost() { return host; }

    @Nullable
    public Integer getPort() { return port; }

    @Nullable
    public String getDatabase() { return database; }

    @Nullable
    public String getUsername() { return username; }

    @Nullable
    public String getPassword() { return password; }
}
