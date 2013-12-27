package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
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
        this.doctrineManagers.add(DoctrineTypes.Manager.ODM);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<ResolveResult> results = new ArrayList<ResolveResult>();

        PhpClass phpClass = EntityHelper.getEntityRepositoryClass(getElement().getProject(), this.entityName);
        if(phpClass != null) {
            results.add(new PsiElementResolveResult(phpClass));
        }

        PhpClass entity = EntityHelper.resolveShortcutName(getElement().getProject(), this.entityName);
        if(entity != null) {
            results.add(new PsiElementResolveResult(entity));
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        Map<String, String> entityNamespaces = new HashMap<String, String>();

        if(this.doctrineManagers.contains(DoctrineTypes.Manager.ORM)) {
            entityNamespaces.putAll(ServiceXmlParserFactory.getInstance(getElement().getProject(), EntityNamesServiceParser.class).getEntityNameMap());
        }

        if(this.doctrineManagers.contains(DoctrineTypes.Manager.ODM)) {
            entityNamespaces.putAll(ServiceXmlParserFactory.getInstance(getElement().getProject(), DocumentNamespacesParser.class).getNamespaceMap());
        }

        List<LookupElement> results = new ArrayList<LookupElement>();

        // find Repository interface to filter RepositoryClasses out
        PhpClass repositoryInterface = PhpElementsUtil.getInterface(phpIndex, DoctrineTypes.REPOSITORY_INTERFACE);
        if(null == repositoryInterface) {
            return results.toArray();
        }

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
                    results.add(new DoctrineEntityLookupElement(repoName, entityClass, this.useClassNameAsLookupString));
                }

            }

        }

        return results.toArray();
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {

        if(entityClass.isAbstract()) {
            return false;
        }

        Symfony2InterfacesUtil symfony2Util = new Symfony2InterfacesUtil();
        return !symfony2Util.isInstanceOf(entityClass, repositoryClass);
    }

}
