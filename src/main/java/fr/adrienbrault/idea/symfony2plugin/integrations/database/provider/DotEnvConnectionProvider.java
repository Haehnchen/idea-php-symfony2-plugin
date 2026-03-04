package fr.adrienbrault.idea.symfony2plugin.integrations.database.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class DotEnvConnectionProvider {

    // Pattern: driver://user:pass@host:port/database
    private static final Pattern DB_URL_PATTERN = Pattern.compile(
        "^(mysql|mariadb|postgresql|pgsql|postgres|sqlite)://(?:([^:@]*):?([^@]*)@)?([^:/]+)(?::(\\d+))?/(.+)$"
    );

    // SQLite pattern: sqlite:///path or sqlite:///:memory:
    private static final Pattern SQLITE_URL_PATTERN = Pattern.compile(
        "^sqlite:///(.+)$"
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%[^%]+%");

    @NotNull
    public List<DatabaseConnectionConfig> getConnectionConfigs(@NotNull Project project) {
        List<DatabaseConnectionConfig> configs = new ArrayList<>();

        Map<String, String> envVars = DotEnvUtil.getEnvironmentVariablesWithValues(project);

        // Primary DATABASE_URL
        String databaseUrl = envVars.get("DATABASE_URL");
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            configs.addAll(parseUrl(databaseUrl, "default", project, envVars));
        }

        // Secondary connections: DATABASE_URL_FOO or FOO_DATABASE_URL
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String key = entry.getKey();
            if (key.equals("DATABASE_URL")) {
                continue;
            }

            String connectionName = null;
            if (key.startsWith("DATABASE_URL_")) {
                connectionName = key.substring("DATABASE_URL_".length()).toLowerCase();
            } else if (key.endsWith("_DATABASE_URL")) {
                connectionName = key.substring(0, key.length() - "_DATABASE_URL".length()).toLowerCase();
            }

            if (connectionName != null && !entry.getValue().isBlank()) {
                configs.addAll(parseUrl(entry.getValue(), connectionName, project, envVars));
            }
        }

        // Unrelated to DATABASE_URL: scan var/*.db files in the project root
        configs.addAll(scanSqliteDirectory(project));

        return uniqueByPath(configs);
    }

    @NotNull
    private List<DatabaseConnectionConfig> parseUrl(@NotNull String url, @NotNull String name, @NotNull Project project, @NotNull Map<String, String> envVars) {
        // Try standard URL pattern first (MySQL, PostgreSQL, etc.)
        Matcher matcher = DB_URL_PATTERN.matcher(url);
        if (matcher.matches()) {
            String driver = normalizeDriver(matcher.group(1));
            String username = emptyToNull(matcher.group(2));
            String password = emptyToNull(matcher.group(3));
            String host = matcher.group(4);
            Integer port = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : getDefaultPort(driver);
            String database = matcher.group(6).replaceAll("\\?.*$", "");

            return Collections.singletonList(new DatabaseConnectionConfig(name, driver, host, port, database, username, password));
        }

        // Try SQLite pattern
        Matcher sqliteMatcher = SQLITE_URL_PATTERN.matcher(url);
        if (sqliteMatcher.matches()) {
            return resolveSqlitePlaceholders(sqliteMatcher.group(1), name, project);
        }

        return Collections.emptyList();
    }

    /**
     * Resolve Symfony placeholders in a SQLite path.
     * Replaces %kernel.project_dir% with the project root, then tries
     * %kernel.environment% with prod/dev/test/stage. Skips any result
     * that still contains unresolved %placeholder%.
     */
    @NotNull
    private List<DatabaseConnectionConfig> resolveSqlitePlaceholders(@NotNull String rawPath, @NotNull String name, @NotNull Project project) {
        VirtualFile projectDir = getProjectVirtualDir(project);
        String projectDirPath = projectDir != null ? projectDir.getPath() : null;

        String path = rawPath;
        if (projectDirPath != null) {
            path = path.replace("%kernel.project_dir%", projectDirPath);
        }

        // No placeholders left — fully resolved
        if (!PLACEHOLDER_PATTERN.matcher(path).find()) {
            return Collections.singletonList(new DatabaseConnectionConfig(name, "sqlite", null, null, path, null, null));
        }

        List<DatabaseConnectionConfig> results = new ArrayList<>();
        for (String env : new String[]{"prod", "dev", "test", "stage"}) {
            String resolved = path.replace("%kernel.environment%", env);
            if (!PLACEHOLDER_PATTERN.matcher(resolved).find()) {
                String connName = results.isEmpty() ? name : name + " (" + env + ")";
                results.add(new DatabaseConnectionConfig(connName, "sqlite", null, null, resolved, null, null));
            }
        }
        return results;
    }

    /**
     * Scan the project's var/ directory for any .db files.
     * This is independent of DATABASE_URL parsing — it discovers SQLite databases
     * that exist on disk regardless of how they are referenced in configuration.
     */
    @NotNull
    private List<DatabaseConnectionConfig> scanSqliteDirectory(@NotNull Project project) {
        VirtualFile projectDir = getProjectVirtualDir(project);
        if (projectDir == null) {
            return Collections.emptyList();
        }

        VirtualFile varDir = projectDir.findChild("var");
        if (varDir == null || !varDir.isDirectory()) {
            return Collections.emptyList();
        }

        List<DatabaseConnectionConfig> results = new ArrayList<>();
        for (VirtualFile child : varDir.getChildren()) {
            if (!child.isDirectory() && child.getName().endsWith(".db") && child.isValid()) {
                String connName = child.getName().replaceAll("\\.db$", "");
                results.add(new DatabaseConnectionConfig(connName, "sqlite", null, null, child.getPath(), null, null));
            }
        }
        return results;
    }

    @Nullable
    private VirtualFile getProjectVirtualDir(@NotNull Project project) {
        @SuppressWarnings("deprecation")
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) return baseDir;

        String basePath = project.getBasePath();
        if (basePath == null) return null;

        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    /**
     * Deduplicate configs by database path, preserving order. */
    @NotNull
    private List<DatabaseConnectionConfig> uniqueByPath(@NotNull List<DatabaseConnectionConfig> configs) {
        Set<String> seen = new LinkedHashSet<>();
        List<DatabaseConnectionConfig> unique = new ArrayList<>();
        for (DatabaseConnectionConfig config : configs) {
            if (seen.add(config.getDatabase() != null ? config.getDatabase() : "")) {
                unique.add(config);
            }
        }
        return unique;
    }

    @NotNull
    private String normalizeDriver(@NotNull String driver) {
        return switch (driver.toLowerCase()) {
            case "mysql", "mariadb" -> "mysql";
            case "postgresql", "pgsql", "postgres" -> "postgresql";
            case "sqlite" -> "sqlite";
            default -> driver;
        };
    }

    private int getDefaultPort(@NotNull String driver) {
        return switch (driver.toLowerCase()) {
            case "mysql" -> 3306;
            case "postgresql" -> 5432;
            default -> 0;
        };
    }

    @Nullable
    private String emptyToNull(@Nullable String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
