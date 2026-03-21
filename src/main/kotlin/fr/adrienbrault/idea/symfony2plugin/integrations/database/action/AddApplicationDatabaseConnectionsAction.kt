package fr.adrienbrault.idea.symfony2plugin.integrations.database.action

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.ui.Messages
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig
import fr.adrienbrault.idea.symfony2plugin.integrations.database.provider.DotEnvConnectionProvider
import fr.adrienbrault.idea.symfony2plugin.integrations.database.ui.AddFoundDatabaseConnectionDialog

/**
 * Action that reads Symfony database configuration and adds data sources to
 * IntelliJ's Database tool window.
 *
 * Registered in the "Add" menu of the Database tool window
 * (DatabaseView.AddDataSourceGroup).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class AddApplicationDatabaseConnectionsAction : AnAction(
    "Add Symfony Application Database...",
    "Add database connections from Symfony configuration (.env, doctrine.yaml)",
    Symfony2Icons.SYMFONY
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && Symfony2ProjectComponent.isEnabled(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val connections = DotEnvConnectionProvider().getConnectionConfigs(project)

        if (connections.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No database connections found in Symfony configuration.",
                "No Connections Found"
            )
            return
        }

        val dialog = AddFoundDatabaseConnectionDialog(project, connections)
        if (dialog.showAndGet()) {
            val selected = dialog.getSelectedConnections()
            if (selected.isNotEmpty()) {
                createDataSources(project, selected)
            }
        }
    }

    private fun createDataSources(project: com.intellij.openapi.project.Project, connections: List<DatabaseConnectionConfig>) {
        val dataSourceManager = LocalDataSourceManager.getInstance(project)

        for (config in connections) {
            val driverId = getDriverId(config.driver)
            val driver = DatabaseDriverManager.getInstance().getDriver(driverId)
            if (driver == null) {
                Messages.showErrorDialog(
                    project,
                    "Could not find database driver '$driverId' for connection '${config.name}'.\n" +
                        "Make sure the required database driver is installed.",
                    "Driver Not Found"
                )
                continue
            }

            val jdbcUrl = buildJdbcUrl(config)
            val dataSource = LocalDataSource.fromDriver(driver, jdbcUrl, false)
            dataSource.name = "Symfony: ${config.name}"
            dataSource.username = config.username ?: ""

            if (!config.password.isNullOrEmpty()) {
                DatabaseCredentials.getInstance().storePassword(dataSource, OneTimeString(config.password))
            }

            dataSourceManager.addDataSource(dataSource)
        }
    }

    private fun buildJdbcUrl(config: DatabaseConnectionConfig): String {
        val driver = config.driver.lowercase()
        val host = config.host ?: "localhost"
        val database = config.database ?: ""

        return when (driver) {
            "mysql" -> {
                val port = config.port ?: 3306
                "jdbc:mysql://$host:$port/$database"
            }
            "postgresql" -> {
                val port = config.port ?: 5432
                "jdbc:postgresql://$host:$port/$database"
            }
            "sqlite" -> "jdbc:sqlite:$database"
            else -> {
                val port = config.port ?: 0
                "jdbc:$driver://$host:$port/$database"
            }
        }
    }

    private fun getDriverId(driver: String): String = when (driver.lowercase()) {
        "mysql" -> "mysql.8"
        "postgresql" -> "postgresql"
        "sqlite" -> "sqlite.xerial"
        else -> driver
    }
}
