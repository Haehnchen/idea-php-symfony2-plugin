package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil

object McpPathUtil {
    fun getRelativeProjectPath(project: Project, virtualFile: VirtualFile): String {
        return VfsExUtil.getRelativeProjectPathStrict(project, virtualFile)
            ?: VfsExUtil.getRelativeProjectPath(project, virtualFile)
            ?: ""
    }
}
