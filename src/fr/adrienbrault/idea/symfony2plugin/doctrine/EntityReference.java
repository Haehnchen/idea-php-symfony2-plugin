package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private String entityName;
    private boolean useClassNameAsLookupString = false;
    private List<DoctrineTypes.Manager> doctrineManagers;

    public EntityReference(@NotNull StringLiteralExpression element, boolean useClassNameAsLookupString) {
        this(element);
        this.useClassNameAsLookupString = useClassNameAsLookupString;
    }

    public EntityReference(@NotNull StringLiteralExpression element) {
        super(element);
        entityName = element.getContents();

        this.doctrineManagers = new ArrayList<DoctrineTypes.Manager>();
        this.doctrineManagers.add(DoctrineTypes.Manager.ORM);
        this.doctrineManagers.add(DoctrineTypes.Manager.MONGO_DB);
    }

    public EntityReference(@NotNull StringLiteralExpression element, DoctrineTypes.Manager... managers) {
        super(element);
        entityName = element.getContents();
        this.doctrineManagers = Arrays.asList(managers);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult.createResults(EntityHelper.getModelPsiTargets(getElement().getProject(), this.entityName));
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        List<LookupElement> results = new ArrayList<LookupElement>();
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        // find Repository interface to filter RepositoryClasses out
        PhpClass repositoryInterface = PhpElementsUtil.getInterface(phpIndex, DoctrineTypes.REPOSITORY_INTERFACE);
        if(null == repositoryInterface) {
            return results.toArray();
        }

        if(this.doctrineManagers.contains(DoctrineTypes.Manager.ORM)) {
            attachRepositoryNames(results, phpIndex, repositoryInterface, ServiceXmlParserFactory.getInstance(getElement().getProject(), EntityNamesServiceParser.class).getEntityNameMap(), DoctrineTypes.Manager.ORM);
        }

        if(this.doctrineManagers.contains(DoctrineTypes.Manager.MONGO_DB)) {
            attachRepositoryNames(results, phpIndex, repositoryInterface, ServiceXmlParserFactory.getInstance(getElement().getProject(), DocumentNamespacesParser.class).getNamespaceMap(), DoctrineTypes.Manager.MONGO_DB);
        }

        return results.toArray();
    }

    private void attachRepositoryNames(List<LookupElement> results, PhpIndex phpIndex, PhpClass repositoryInterface, Map<String, String> entityNamespaces, DoctrineTypes.Manager manager) {
        for (Map.Entry<String, String> entry : entityNamespaces.entrySet()) {

            // search for classes that match the symfony2 namings
            Collection<PhpNamespace> entities = phpIndex.getNamespacesByName(entry.getValue());

            // @TODO: it looks like PhpIndex cant search for classes like \ns\Path\*\...
            // temporary only use flat entities and dont support "MyBundle:Folder\Entity"
            for (PhpNamespace entity_files : entities) {

                // build our symfony2 shortcut
                String filename = entity_files.getContainingFile().getName();
                String className = filename.substring(0, filename.lastIndexOf('.'));
                String repoName = entry.getKey() + ':'  + className;

                // dont add Repository classes and abstract entities
                PhpClass entityClass = PhpElementsUtil.getClass(phpIndex, entityNamespaces.get(entry.getKey()) + "\\" + className);
                if(null != entityClass && isEntity(entityClass, repositoryInterface)) {
                    results.add(new DoctrineEntityLookupElement(repoName, entityClass, this.useClassNameAsLookupString).withManager(manager));
                }

            }

        }
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {

        if(entityClass.isAbstract()) {
            return false;
        }

        Symfony2InterfacesUtil symfony2Util = new Symfony2InterfacesUtil();
        return !symfony2Util.isInstanceOf(entityClass, repositoryClass);
    }

}
