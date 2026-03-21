package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import java.nio.file.Paths

object McpPathUtil {
    fun getRelativeProjectPath(project: Project, virtualFile: VirtualFile): String {
        val projectDir = ProjectUtil.getProjectDir(project) ?: return ""

        return VfsUtil.getRelativePath(virtualFile, projectDir, '/')
            ?: runCatching { Paths.get(projectDir.path).relativize(Paths.get(virtualFile.path)).toString() }.getOrNull()
            ?: ""
    }
}
