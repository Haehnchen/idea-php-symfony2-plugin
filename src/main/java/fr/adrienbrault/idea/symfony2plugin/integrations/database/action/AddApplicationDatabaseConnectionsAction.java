package fr.adrienbrault.idea.symfony2plugin.integrations.database.action;

import com.intellij.credentialStore.OneTimeString;
import com.intellij.database.access.DatabaseCredentials;
import com.intellij.database.dataSource.DatabaseDriver;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.provider.DotEnvConnectionProvider;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.ui.AddFoundDatabaseConnectionDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action that reads Symfony database configuration and adds data sources to
 * IntelliJ's Database tool window.
 *
 * Registered in the "Add" menu of the Database tool window
 * (DatabaseView.AddDataSourceGroup).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AddApplicationDatabaseConnectionsAction extends AnAction {

    public AddApplicationDatabaseConnectionsAction() {
        super("Add Symfony Application Database...",
              "Add database connections from Symfony configuration (.env, doctrine.yaml)",
              Symfony2Icons.SYMFONY);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        List<DatabaseConnectionConfig> connections = new DotEnvConnectionProvider().getConnectionConfigs(project);

        if (connections.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No database connections found in Symfony configuration.",
                "No Connections Found"
            );
            return;
        }

        AddFoundDatabaseConnectionDialog dialog = new AddFoundDatabaseConnectionDialog(project, connections);
        if (dialog.showAndGet()) {
            List<DatabaseConnectionConfig> selected = dialog.getSelectedConnections();
            if (!selected.isEmpty()) {
                createDataSources(project, selected);
            }
        }
    }

    private void createDataSources(@NotNull Project project, @NotNull List<DatabaseConnectionConfig> connections) {
        LocalDataSourceManager dataSourceManager = LocalDataSourceManager.getInstance(project);

        for (DatabaseConnectionConfig config : connections) {
            String driverId = getDriverId(config.getDriver());
            DatabaseDriver driver = DatabaseDriverManager.getInstance().getDriver(driverId);
            if (driver == null) {
                Messages.showErrorDialog(project,
                    "Could not find database driver '" + driverId + "' for connection '" + config.getName() + "'.\n" +
                    "Make sure the required database driver is installed.",
                    "Driver Not Found");
                continue;
            }

            String jdbcUrl = buildJdbcUrl(config);
            LocalDataSource dataSource = LocalDataSource.fromDriver(driver, jdbcUrl, false);
            dataSource.setName("Symfony: " + config.getName());

            String username = config.getUsername() != null ? config.getUsername() : "";
            dataSource.setUsername(username);

            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                DatabaseCredentials.getInstance().storePassword(dataSource, new OneTimeString(config.getPassword()));
            }

            dataSourceManager.addDataSource(dataSource);
        }
    }

    @NotNull
    private String buildJdbcUrl(@NotNull DatabaseConnectionConfig config) {
        String driver = config.getDriver().toLowerCase();
        String host = config.getHost() != null ? config.getHost() : "localhost";
        String database = config.getDatabase() != null ? config.getDatabase() : "";

        switch (driver) {
            case "mysql":
                int mysqlPort = config.getPort() != null ? config.getPort() : 3306;
                return String.format("jdbc:mysql://%s:%d/%s", host, mysqlPort, database);
            case "postgresql":
                int pgPort = config.getPort() != null ? config.getPort() : 5432;
                return String.format("jdbc:postgresql://%s:%d/%s", host, pgPort, database);
            case "sqlite":
                // SQLite path is stored in database field
                return String.format("jdbc:sqlite:%s", database);
            default:
                int port = config.getPort() != null ? config.getPort() : 0;
                return String.format("jdbc:%s://%s:%d/%s", driver, host, port, database);
        }
    }

    @NotNull
    private String getDriverId(@NotNull String driver) {
        switch (driver.toLowerCase()) {
            case "mysql":
                return "mysql.8";
            case "postgresql":
                return "postgresql";
            case "sqlite":
                return "sqlite.xerial";
            default:
                return driver;
        }
    }
}
