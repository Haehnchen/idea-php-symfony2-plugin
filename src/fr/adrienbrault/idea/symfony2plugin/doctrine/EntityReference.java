package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
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

        results.addAll(getModelLookupElements(getElement().getProject(), this.useClassNameAsLookupString, this.doctrineManagers.toArray(new DoctrineTypes.Manager[this.doctrineManagers.size()])));

        return results.toArray();
    }

    /**
     * TODO: dont use lookup elements, refactor complete class we need more soon
     */
    public static Collection<String> getEntityNames(Project project) {

        List<LookupElement> results = new ArrayList<LookupElement>();
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Set<String> models = new HashSet<String>();

        // find Repository interface to filter RepositoryClasses out
        PhpClass repositoryInterface = PhpElementsUtil.getInterface(phpIndex, DoctrineTypes.REPOSITORY_INTERFACE);
        if(null == repositoryInterface) {
            return models;
        }

        attachRepositoryNames(project, results, ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap(), DoctrineTypes.Manager.ORM, true);
        attachRepositoryNames(project, results, ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap(), DoctrineTypes.Manager.MONGO_DB, true);

        for(LookupElement lookupElement: results) {
            models.add(lookupElement.getLookupString());
        }

        return models;

    }

    private static void attachRepositoryNames(Project project, final List<LookupElement> results, Map<String, String> entityNamespaces, final DoctrineTypes.Manager manager, final boolean useClassNameAsLookupString) {

        for(DoctrineModel doctrineModel: EntityHelper.getModelClasses(project, entityNamespaces)) {
            String repositoryName = doctrineModel.getRepositoryName();
            if(repositoryName != null) {
                results.add(new DoctrineEntityLookupElement(repositoryName, doctrineModel.getPhpClass(), useClassNameAsLookupString).withManager(manager));
            }
        }

        // add custom doctrine classes
        Collection<DoctrineModelProviderParameter.DoctrineModel> doctrineModels = new ArrayList<DoctrineModelProviderParameter.DoctrineModel>();
        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, doctrineModels);
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                results.add(new DoctrineEntityLookupElement(doctrineModel.getName(), doctrineModel.getPhpClass(), useClassNameAsLookupString));
            }
        }

    }

    public static List<LookupElement> getModelLookupElements(Project project, DoctrineTypes.Manager... managers) {
        return getModelLookupElements(project, false, managers);
    }

    private static List<LookupElement> getModelLookupElements(Project project, boolean useClassNameAsLookupString, DoctrineTypes.Manager... managers) {

        List<LookupElement> results = new ArrayList<LookupElement>();

        List<DoctrineTypes.Manager> managerList = Arrays.asList(managers);

        if(managerList.contains(DoctrineTypes.Manager.ORM)) {
            attachRepositoryNames(project, results, ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap(), DoctrineTypes.Manager.ORM, useClassNameAsLookupString);
        }

        if(managerList.contains(DoctrineTypes.Manager.MONGO_DB)) {
            attachRepositoryNames(project, results, ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap(), DoctrineTypes.Manager.MONGO_DB, useClassNameAsLookupString);
        }

        return results;
    }


}
