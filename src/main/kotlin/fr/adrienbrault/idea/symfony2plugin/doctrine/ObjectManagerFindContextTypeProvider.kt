package fr.adrienbrault.idea.symfony2plugin.doctrine

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

private const val OBJECT_MANAGER_CONTEXT_TRIM_KEY = '\u0182'

/**
 * $repository->find()->get<caret>Id();
 * $this->repository->find()->ge<caret>tId();
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ObjectManagerFindContextTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0173'

    // Index-safe only: no PhpIndex here.
    override fun getType(e: PsiElement): PhpType? {
        val methodReference = e as? MethodReference ?: return null

        val settings = Settings.getInstance(e.project)
        if (!settings.pluginEnabled || !settings.featureTypeProvider) {
            return null
        }

        val refSignature = methodReference.signature
        if (StringUtil.isEmpty(refSignature)) {
            return null
        }

        val methodRefName = methodReference.name
        if (
            methodRefName == null ||
            (methodRefName !in listOf("find", "findAll") && !methodRefName.startsWith("findOneBy") && !methodRefName.startsWith("findBy"))
        ) {
            return null
        }

        val signature = when (val firstPsiChild = methodReference.firstPsiChild) {
            // reduce supported scope here; by checking via "instanceof"
            is Variable -> firstPsiChild.signature
            is PhpReference -> firstPsiChild.signature
            else -> null
        }

        if (StringUtils.isBlank(signature)) {
            return null
        }

        return PhpType().add("#${key}$signature$OBJECT_MANAGER_CONTEXT_TRIM_KEY$methodRefName")
    }

    override fun complete(s: String, project: Project): PhpType? {
        val split = s.substring(2)
            .split(OBJECT_MANAGER_CONTEXT_TRIM_KEY)
            .dropLastWhile { it.isEmpty() }
        if (split.size < 2) {
            return null
        }

        val signature = split[0]
        val methodName = split[1]
        val repositoryClasses = mutableSetOf<PhpClass>()

        // collect the instances of the type before our call; which we extract on "getType()"
        for (phpNamedElement in PhpIndex.getInstance(project).getBySignature(signature)) {
            // #C\Foo\BarRepository
            if (phpNamedElement is PhpClass) {
                repositoryClasses.add(phpNamedElement)
                continue
            }

            // resolve the the previous type
            for (type in phpNamedElement.type.filterPrimitives().types) {
                repositoryClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(type))
            }
        }

        // only all repository class
        val repositories = repositoryClasses.filterTo(mutableSetOf()) {
            PhpElementsUtil.isInstanceOf(it, "\\Doctrine\\Common\\Persistence\\ObjectRepository") ||
                PhpElementsUtil.isInstanceOf(it, "\\Doctrine\\Persistence\\ObjectRepository")
        }

        if (repositories.isEmpty()) {
            return null
        }

        // based on repositoryClass find the Entity which as defined it via metadata
        val types = mutableSetOf<String>()
        for (phpClass in repositories) {
            val models = DoctrineMetadataUtil.findMetadataModelForRepositoryClass(project, phpClass.presentableFQN)
            for (doctrineModel in models) {
                for (entityClass in PhpElementsUtil.getClassesInterface(project, doctrineModel.className)) {
                    types.add(
                        if (methodName == "findAll" || methodName.startsWith("findBy")) {
                            entityClass.fqn + "[]"
                        } else {
                            entityClass.fqn
                        },
                    )
                }
            }
        }

        if (types.isEmpty()) {
            return null
        }

        val phpType = PhpType()
        types.forEach(phpType::add)
        return phpType
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement>? = null
}
