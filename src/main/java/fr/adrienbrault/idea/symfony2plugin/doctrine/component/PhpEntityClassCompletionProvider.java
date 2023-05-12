package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.util.PhpContractUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import gnu.trove.THashSet;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpEntityClassCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Map<String, String> entityNamespaces = ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap();

        // copied from PhpCompletionUtil::addClassesInNamespace looks the official way to find classes in namespaces
        // its a really performance nightmare

        Collection<String> names = phpIndex.getAllClassNames(new CamelHumpMatcher(resultSet.getPrefixMatcher().getPrefix()));
        for (String name : names) {
            Collection<PhpClass> classes = phpIndex.getClassesByName(name);

            for(Map.Entry<String, String> entry: entityNamespaces.entrySet()) {
                String namespaceFqn = PhpLangUtil.toFQN(entry.getValue());
                Collection<PhpClass> filtered = filterByNamespace(classes, namespaceFqn);
                for (PhpClass phpClass : filtered) {
                    resultSet.addElement(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
                }
            }
        }
    }

    /**
     * Replacment for "PhpCompletionUtil.filterByNamespace(classes, namespaceFqn)"
     */
    public static Collection<PhpClass> filterByNamespace(@NotNull Collection<PhpClass> classes, @NotNull String namespaceFQN) {
        PhpContractUtil.assertFqn(namespaceFQN);
        return StreamEx.of(classes).filter((aClass) -> PhpLangUtil.equalsClassNames(namespaceFQN, StringUtil.trimEnd(aClass.getNamespaceName(), "\\"))).toCollection(THashSet::new);
    }
}
