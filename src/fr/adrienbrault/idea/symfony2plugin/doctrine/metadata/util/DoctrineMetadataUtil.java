package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataUtil {

    public static Collection<LookupElement> getObjectRepositoryLookupElements(@NotNull Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("\\Doctrine\\Common\\Persistence\\ObjectRepository")) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN == null) {
                continue;
            }

            lookupElements.add(
                LookupElementBuilder.create(phpClass.getName()).withTypeText(phpClass.getPresentableFQN(), true).withIcon(phpClass.getIcon())
            );
        }

        return lookupElements;
    }

}
