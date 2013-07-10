package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityReference extends PsiReferenceBase<PsiElement> implements PsiReference {
    private String entityName;
    private boolean useClassNameAsLookupString = false;

    public EntityReference(@NotNull StringLiteralExpression element, boolean useClassNameAsLookupString) {
        this(element);
        this.useClassNameAsLookupString = useClassNameAsLookupString;
    }

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

        Map<String, String> entityNamespaces = ServiceXmlParserFactory.getInstance(getElement().getProject(), EntityNamesServiceParser.class).getEntityNameMap();

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
