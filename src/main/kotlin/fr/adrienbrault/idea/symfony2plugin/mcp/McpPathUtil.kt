package fr.adrienbrault.idea.symfony2plugin.mcp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil

object McpPathUtil {
    fun getRelativeProjectPath(project: Project, virtualFile: VirtualFile): String {
        val projectDir = ProjectUtil.getProjectDir(project) ?: return ""

        return VfsUtil.getRelativePath(virtualFile, projectDir, '/')
            ?: FileUtil.getRelativePath(projectDir.path, virtualFile.path, '/')
            ?: ""
    }
}
