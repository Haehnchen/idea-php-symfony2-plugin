package fr.adrienbrault.idea.symfony2plugin.webDeployment

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class WebDeploymentPostStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!WebDeploymentUtil.isEnabled(project)) {
            return
        }

        project.getService(WebDeploymentProjectComponent.ProjectService::class.java).start()
    }
}
