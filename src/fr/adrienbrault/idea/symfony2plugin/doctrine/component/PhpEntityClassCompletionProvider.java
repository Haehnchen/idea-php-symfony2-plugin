package fr.adrienbrault.idea.symfony2plugin.doctrine.component;


import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.completion.PhpCompletionUtil;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public class PhpEntityClassCompletionProvider extends CompletionProvider<CompletionParameters> {

    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {


        PhpIndex phpIndex = PhpIndex.getInstance(parameters.getOriginalFile().getProject());

        PhpClass repositoryInterface = PhpElementsUtil.getInterface(phpIndex, DoctrineTypes.REPOSITORY_INTERFACE);
        if(null == repositoryInterface) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = parameters.getOriginalFile().getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, String> entityNamespaces = symfony2ProjectComponent.getEntityNamespacesMap();


        // copied from PhpCompletionUtil::addClassesInNamespace looks the official way to find classes in namespaces
        // its a really performance nightmare

        Collection<String> names = phpIndex.getAllClassNames(new CamelHumpMatcher(resultSet.getPrefixMatcher().getPrefix()));
        for (String name : names) {
            Collection<PhpClass> classes = phpIndex.getClassesByName(name);

            for(Map.Entry<String, String> entry: entityNamespaces.entrySet()) {
                String namespaceFqn = PhpLangUtil.toFQN(entry.getValue());
                Collection<PhpClass> filtered = PhpCompletionUtil.filterByNamespace(classes, namespaceFqn);
                for (PhpClass phpClass : filtered) {
                    if(EntityReference.isEntity(phpClass, repositoryInterface)) {
                        resultSet.addElement(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
                    }
                }
            }

        }

    }

}
