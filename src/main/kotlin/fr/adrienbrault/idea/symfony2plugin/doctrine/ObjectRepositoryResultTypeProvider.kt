package fr.adrienbrault.idea.symfony2plugin.doctrine

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil

private val FIND_SIGNATURES = arrayOf(
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "find"),
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy"),
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findAll"),
    MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "find"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findOneBy"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findAll"),
    MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findBy"),
)

private val MANAGED_FIND_METHOD = setOf("find", "findOneBy", "findAll", "findBy")
private const val OBJECT_REPOSITORY_RESULT_TRIM_KEY = '\u0184'

/**
 * Resolve "find*" and attach the entity from the getRepository method
 *
 * "$om->getRepository('\Foo\Bar')->find('foobar')->get<caret>Id()"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ObjectRepositoryResultTypeProvider : PhpTypeProvider4 {
    override fun getKey(): Char = '\u0152'

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

        // we can get the repository name from the signature calls
        // #M#?#M#?#M#C\Foo\Bar\Controller\BarController.get?doctrine.getRepository?EntityBundle:User.find
        var repositorySignature = methodReference.signature

        val lastRepositoryName = repositorySignature.lastIndexOf(ObjectRepositoryTypeProvider.TRIM_KEY)
        if (lastRepositoryName == -1) {
            return null
        }

        repositorySignature = repositorySignature.substring(lastRepositoryName)
        val nextMethodCall = repositorySignature.indexOf('.' + methodRefName)
        if (nextMethodCall == -1) {
            return null
        }

        repositorySignature = repositorySignature.substring(1, nextMethodCall)
        return PhpType().add("#${key}$refSignature$OBJECT_REPOSITORY_RESULT_TRIM_KEY$repositorySignature")
    }

    override fun complete(s: String, project: Project): PhpType? {
        val endIndex = s.lastIndexOf(OBJECT_REPOSITORY_RESULT_TRIM_KEY)
        if (endIndex == -1) {
            return null
        }

        val originalSignature = s.substring(0, endIndex)
        val parameter = PhpTypeProviderUtil.getResolvedParameter(
            PhpIndex.getInstance(project),
            s.substring(endIndex + 1),
        ) ?: return null

        val phpClass = EntityHelper.resolveShortcutName(project, parameter) ?: return null
        val phpIndex = PhpIndex.getInstance(project)
        val typeSignature = getTypeSignatureMagic(phpIndex, originalSignature)

        // ->getRepository(SecondaryMarket::class)->findAll() => "findAll", but only if its a instance of this method;
        // so non Doctrine method are already filtered
        val resolveMethods = getObjectRepositoryCall(typeSignature).mapTo(mutableSetOf()) { it.name }
        if (resolveMethods.isEmpty()) {
            return null
        }

        val phpType = PhpType()
        resolveMethods
            .mapTo(mutableSetOf()) {
                if (it == "findAll" || it.startsWith("findBy")) phpClass.fqn + "[]" else phpClass.fqn
            }
            .forEach(phpType::add)

        return phpType
    }

    override fun getBySignature(
        expression: String,
        visited: Set<String>,
        depth: Int,
        project: Project,
    ): Collection<PhpNamedElement>? = null

    private fun getObjectRepositoryCall(phpNamedElements: Collection<PhpNamedElement>): Collection<Method> {
        return phpNamedElements.filterTo(mutableSetOf()) {
            it is Method && PhpElementsUtil.isMethodInstanceOf(it, *FIND_SIGNATURES)
        }.filterIsInstance<Method>()
    }

    /**
     * We can have multiple types inside a TypeProvider; split them on "|" so that we dont get empty types
     *
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository|
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository
     */
    private fun getTypeSignatureMagic(phpIndex: PhpIndex, signature: String): Collection<PhpNamedElement> {
        // magic method resolving; we need to have the ObjectRepository method which does not exists for magic methods, so strip it
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findByName => findBy
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findOneBy => findOne
        val elements = mutableSetOf<PhpNamedElement>()
        val signatures = signature.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
        for (signaturePart in signatures) {
            var part = signaturePart
            val separator = part.lastIndexOf('.')
            if (separator > 0) {
                // method already exists in repository use it
                for (phpNamedElement in phpIndex.getBySignature(part, null, 0)) {
                    if (phpNamedElement is Method) {
                        if (MANAGED_FIND_METHOD.contains(phpNamedElement.name)) {
                            continue
                        }

                        // we got into the repository itself so stop here; was not overwritten by repository class
                        val containingClass = phpNamedElement.containingClass
                        if (
                            PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Persistence\\ObjectRepository") ||
                            PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository")
                        ) {
                            continue
                        }

                        return emptyList()
                    }
                }

                // strip field name from the method name "findOneByName" => "findOneBy"
                val methodName = part.substring(separator + 1)
                part = when {
                    methodName.startsWith("findOneBy") -> part.substring(0, separator + 1) + "findOneBy"
                    methodName.startsWith("findBy") -> part.substring(0, separator + 1) + "findBy"
                    else -> part
                }
            }

            elements.addAll(phpIndex.getBySignature(part, null, 0))
        }

        return elements
    }
}
