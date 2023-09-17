package fr.adrienbrault.idea.symfony2plugin.ux.variable.collector;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Support variables piped from "AsTwigComponent, AsLiveComponent" to Twig templates
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxComponentVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collectPsiVariables(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, PsiVariable> variables) {
        Project project = parameter.getProject();
        Collection<PhpClass> componentClassesForTemplateFile = UxUtil.getComponentClassesForTemplateFile(project, parameter.getElement().getContainingFile());

        if (!componentClassesForTemplateFile.isEmpty()) {
            variables.put("attributes", new PsiVariable("\\Symfony\\UX\\TwigComponent\\ComponentAttributes"));
        }

        for (PhpClass phpClass : componentClassesForTemplateFile) {
            variables.put("this", new PsiVariable(phpClass.getFQN()));

            for (Field field : phpClass.getFields()) {
                if (field.getModifier().isPublic()) {
                    variables.put(field.getName(), new PsiVariable(
                        PhpIndex.getInstance(project).completeType(project, field.getType(), new HashSet<>()).getTypes()
                    ));
                }
            }
        }
    }
}
