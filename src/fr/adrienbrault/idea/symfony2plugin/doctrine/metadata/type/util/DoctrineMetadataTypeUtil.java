package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataTypeUtil {

    public static final String DBAL_TYPE = "Doctrine\\DBAL\\Types\\Type";
    public static final String COUCHDB_TYPE = "Doctrine\\ODM\\CouchDB\\Types\\Type";
    public static final String MONGODB_TYPE = "Doctrine\\ODM\\MongoDB\\Types\\Type";

    @NotNull
    public static Collection<String> getTypeClassesByScopeWithAllFallback(@NotNull PsiElement psiElement) {
        DoctrineManagerEnum manager = DoctrineMetadataUtil.findManagerByScope(psiElement);
        if(manager != null) {
            return DoctrineMetadataTypeUtil.getTypeClassByManager(manager);
        }

        return getAllTypes();
    }

    @NotNull
    private static Collection<String> getTypeClassByManager(@NotNull DoctrineManagerEnum managerEnum) {
        if(managerEnum == DoctrineManagerEnum.ORM) {
            return Collections.singleton(DBAL_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.COUCHDB) {
            return Collections.singleton(COUCHDB_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.MONGODB) {
            return Collections.singleton(MONGODB_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.ODM || managerEnum == DoctrineManagerEnum.DOCUMENT) {
            return Arrays.asList(MONGODB_TYPE, COUCHDB_TYPE);
        }

        return Collections.emptyList();
    }

    @NotNull
    private static Collection<String> getAllTypes() {
        return Arrays.asList(
            DBAL_TYPE,
            COUCHDB_TYPE,
            MONGODB_TYPE
        );
    }

    public static void visitType(@NotNull Project project, @NotNull Collection<String> typeInterfaces, @NotNull Processor<Pair<PhpClass, String>> processor) {
        for (String typeInterface : typeInterfaces) {
            for (PhpClass phpClass : PhpIndex.getInstance(project).getAllSubclasses(typeInterface)) {
                String name = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");

                // non string value;
                // Doctrine resolves also on class name
                if(name == null) {
                    String className = phpClass.getName();
                    if(className.endsWith("Type")) {
                        name = className.substring(0, className.length() - 4).toLowerCase();
                    }
                }

                if(name == null) {
                    continue;
                }

                processor.process(Pair.create(phpClass, name));
            }
        }
    }
}
