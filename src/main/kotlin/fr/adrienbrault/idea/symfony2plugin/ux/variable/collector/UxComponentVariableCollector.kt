package fr.adrienbrault.idea.symfony2plugin.ux.variable.collector

import com.jetbrains.php.PhpIndex
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil

/**
 * Support variables piped from "AsTwigComponent, AsLiveComponent" to Twig templates
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class UxComponentVariableCollector : TwigFileVariableCollector {
    override fun collectPsiVariables(parameter: TwigFileVariableCollectorParameter, variables: MutableMap<String, PsiVariable>) {
        val project = parameter.project
        val componentClassesForTemplateFile = UxUtil.getComponentClassesForTemplateFile(project, parameter.element.containingFile)

        if (componentClassesForTemplateFile.isNotEmpty()) {
            variables["attributes"] = PsiVariable("\\Symfony\\UX\\TwigComponent\\ComponentAttributes")
        }

        for (phpClass in componentClassesForTemplateFile) {
            variables["this"] = PsiVariable(phpClass.fqn)

            for (field in phpClass.fields) {
                if (field.modifier.isPublic) {
                    variables[field.name] = PsiVariable(
                        PhpIndex.getInstance(project).completeType(project, field.type, HashSet()).types
                    )
                }
            }
        }
    }
}
