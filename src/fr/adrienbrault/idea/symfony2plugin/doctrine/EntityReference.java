package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityReference extends PsiReferenceBase<PsiElement> implements PsiReference {
    private String entityName;

    public EntityReference(@NotNull StringLiteralExpression element) {
        super(element);

        entityName = element.getText().substring(
                element.getValueRange().getStartOffset(),
                element.getValueRange().getEndOffset()
        );
    }

    @Nullable
    @Override
    public PsiElement resolve() {

        PhpClass entity = EntityHelper.resolveShortcutName(getElement().getProject(), this.entityName);
        if(entity != null) {
            return new PsiElementResolveResult(entity).getElement();
        }

        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {

        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());

        Symfony2ProjectComponent symfony2ProjectComponent = getElement().getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, String> entityNamespaces = symfony2ProjectComponent.getEntityNamespacesMap();

        List<LookupElement> results = new ArrayList<LookupElement>();

        // find Repository interface to filter RepositoryClasses out
        PhpClass repositoryClass = getRepositoryClass(phpIndex);
        if(null == repositoryClass) {
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
                PhpClass entityClass = getClass(phpIndex, entityNamespaces.get(entry.getKey()) + "\\" + className);
                if(null != entityClass && isEntity(entityClass, repositoryClass)) {
                    results.add(new DoctrineEntityLookupElement(repoName, entityClass));
                }

            }

        }

        return results.toArray();
    }

    @Nullable
    public static PhpClass getRepositoryClass(PhpIndex phpIndex) {
        Collection<PhpClass> classes = phpIndex.getInterfacesByFQN("\\Doctrine\\Common\\Persistence\\ObjectRepository");
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    @Nullable
    protected PhpClass getClass(PhpIndex phpIndex, String className) {
        Collection<PhpClass> classes = phpIndex.getClassesByFQN(className);
        return classes.isEmpty() ? null : classes.iterator().next();
    }

    public static boolean isEntity(PhpClass entityClass, PhpClass repositoryClass) {

        if(entityClass.isAbstract()) {
            return false;
        }

        Symfony2InterfacesUtil symfony2Util = new Symfony2InterfacesUtil();
        return !symfony2Util.isInstanceOf(entityClass, repositoryClass);
    }

}
