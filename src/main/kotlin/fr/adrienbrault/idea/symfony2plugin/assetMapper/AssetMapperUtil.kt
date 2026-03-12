package fr.adrienbrault.idea.symfony2plugin.assetMapper

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.PhpReturn
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.AssetMapperModule
import fr.adrienbrault.idea.symfony2plugin.assetMapper.dict.MappingFileEnum
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object AssetMapperUtil {

    private val MAPPING_CACHE: Key<CachedValue<List<AssetMapperModule>>> = Key.create("SYMFONY_ASSET_MAPPER_MAPPING_CACHE")

    @JvmStatic
    fun getMappingFiles(project: Project): List<AssetMapperModule> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            MAPPING_CACHE,
            {
                CachedValueProvider.Result.create(
                    getMappingFilesInner(project),
                    PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
                )
            },
            false
        )
    }

    private fun getMappingFilesInner(project: Project): List<AssetMapperModule> {
        val modules = mutableListOf<AssetMapperModule>()
        val files = LinkedHashSet<VirtualFile>()

        VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "importmap.php")?.let { files.add(it) }
        VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "assets", "vendor", "installed.php")?.let { files.add(it) }

        files.addAll(FilenameIndex.getVirtualFilesByName("importmap.php", GlobalSearchScope.allScope(project)))
        for (file in FilenameIndex.getVirtualFilesByName("installed.php", GlobalSearchScope.allScope(project))) {
            // composer
            val parent = file.parent
            if (parent != null && "composer" == parent.name) {
                continue
            }
            files.add(file)
        }

        for (file in files) {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: continue

            for (phpReturn in PsiTreeUtil.collectElementsOfType(psiFile, PhpReturn::class.java)) {
                val argument = phpReturn.argument
                if (argument is ArrayCreationExpression) {
                    for ((key, value) in PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(argument)) {
                        var path: String? = null
                        var url: String? = null
                        var version: String? = null
                        var entrypoint: Boolean? = false
                        var type: String? = null

                        if (value is ArrayCreationExpression) {
                            path = PhpElementsUtil.getArrayValueString(value, "path")
                            url = PhpElementsUtil.getArrayValueString(value, "url")
                            version = PhpElementsUtil.getArrayValueString(value, "version")
                            entrypoint = PhpElementsUtil.getArrayValueBool(value, "entrypoint")
                            type = PhpElementsUtil.getArrayValueString(value, "type")
                        }

                        modules.add(AssetMapperModule(MappingFileEnum.fromString(file.name), file, key, path, url, version, entrypoint, type))
                    }
                }
            }
        }

        return modules
    }

    private fun getModuleReferences(module: String, modules: List<AssetMapperModule>): Collection<VirtualFile> {
        val files = HashSet<VirtualFile>()

        for (mappingFile in modules) {
            if (mappingFile.key != module) {
                continue
            }

            if (mappingFile.sourceType == MappingFileEnum.IMPORTMAP) {
                // default project structure: "assets/vendor/*"
                val parent = mappingFile.sourceFile.parent
                if (parent != null) {
                    if (mappingFile.path != null) {
                        // simple path normalize: "./app.js"
                        val split = StringUtils.split(mappingFile.path, "/")
                            .filter { it != "." }
                            .toTypedArray()

                        val relativeFile = VfsUtil.findRelativeFile(parent, *split)
                        if (relativeFile != null) {
                            files.add(relativeFile)
                            break
                        }
                    } else {
                        if ("css" == mappingFile.type) {
                            val split = StringUtils.split("assets/vendor/" + mappingFile.key, "/")
                            val relativeFile = VfsUtil.findRelativeFile(parent, *split)
                            if (relativeFile != null) {
                                files.add(relativeFile)
                                break
                            }
                        } else if (mappingFile.key.contains("/")) {
                            val path: String = if (mappingFile.key.startsWith("@") && mappingFile.key.split("/").size == 2) {
                                mappingFile.key + "/" + mappingFile.key.split("/")[1] + ".index.js"
                            } else {
                                mappingFile.key + ".js"
                            }

                            val split = StringUtils.split("assets/vendor/$path", "/")
                            val relativeFile = VfsUtil.findRelativeFile(parent, *split)
                            if (relativeFile != null) {
                                files.add(relativeFile)
                                break
                            }
                        } else {
                            val relativeFile = VfsUtil.findRelativeFile(parent, "assets", "vendor", mappingFile.key, mappingFile.key + ".index.js")
                            if (relativeFile != null) {
                                files.add(relativeFile)
                                break
                            }
                        }
                    }
                }
            } else if (mappingFile.sourceType == MappingFileEnum.INSTALLED) {
                // fallback without project structure: every folder like "vendor/installed.php" => "vendor/bootstrap"
                val parent = mappingFile.sourceFile.parent
                if (parent != null) {
                    if (mappingFile.key.endsWith("css")) {
                        val split = StringUtils.split(mappingFile.key, "/")
                        val relativeFile = VfsUtil.findRelativeFile(parent, *split)
                        if (relativeFile != null) {
                            files.add(relativeFile)
                            break
                        }
                    } else if (mappingFile.key.contains("/")) {
                        val path: String = if (mappingFile.key.startsWith("@") && mappingFile.key.split("/").size == 2) {
                            mappingFile.key + "/" + mappingFile.key.split("/")[1] + ".index.js"
                        } else {
                            mappingFile.key + ".js"
                        }

                        val split = StringUtils.split(path, "/")
                        val relativeFile = VfsUtil.findRelativeFile(parent, *split)
                        if (relativeFile != null) {
                            files.add(relativeFile)
                            break
                        }
                    } else {
                        val relativeFile = VfsUtil.findRelativeFile(parent, mappingFile.key, mappingFile.key + ".index.js")
                        if (relativeFile != null) {
                            files.add(relativeFile)
                            break
                        }
                    }
                }
            }
        }

        return files
    }

    @JvmStatic
    fun getEntrypointModuleReferences(project: Project, module: String): Collection<VirtualFile> {
        val collect = getEntrypointMappings(project).filter { it.key == module }

        // mapping targets
        val files = collect.map { it.sourceFile }.toHashSet<VirtualFile>()

        // mapping reference tags
        files.addAll(getModuleReferences(module, collect))

        return files
    }

    private fun getEntrypointMappings(project: Project): List<AssetMapperModule> {
        return getMappingFiles(project).filter { it.entrypoint != null && it.entrypoint!! }
    }

    @JvmStatic
    fun getLookupElements(project: Project): Collection<LookupElement> {
        return getLookupElements(getMappingFiles(project))
    }

    @JvmStatic
    fun getEntrypointLookupElements(project: Project): Collection<LookupElement> {
        return getLookupElements(getEntrypointMappings(project))
    }

    @JvmStatic
    fun getLookupElements(modules: List<AssetMapperModule>): Collection<LookupElement> {
        val lookupElements = mutableListOf<LookupElement>()
        val visited = HashSet<String>()

        for (module in modules) {
            if (visited.contains(module.key)) {
                continue
            }

            visited.add(module.key)

            var elementBuilder = LookupElementBuilder.create(module.key).withIcon(Symfony2Icons.SYMFONY)
            var typeText = ""

            if (module.url != null) {
                typeText = module.url
            } else if (module.path != null) {
                typeText = module.path
            }

            if (module.version != null) {
                typeText = if (typeText.isNotBlank()) {
                    module.version + " " + typeText
                } else {
                    module.version
                }
            }

            if (typeText.isNotBlank()) {
                elementBuilder = elementBuilder.withTypeText(typeText)
            }

            lookupElements.add(elementBuilder)
        }

        return lookupElements
    }
}
