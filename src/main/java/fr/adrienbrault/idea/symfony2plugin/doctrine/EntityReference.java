package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProviderParameter;
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

        this.doctrineManagers = new ArrayList<>();
        this.doctrineManagers.add(DoctrineTypes.Manager.ORM);
        this.doctrineManagers.add(DoctrineTypes.Manager.MONGO_DB);
        this.doctrineManagers.add(DoctrineTypes.Manager.COUCH_DB);
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
        List<LookupElement> results = new ArrayList<>(getModelLookupElements(getElement().getProject(), this.useClassNameAsLookupString, this.doctrineManagers.toArray(new DoctrineTypes.Manager[0])));
        return results.toArray();
    }

    private static void attachRepositoryNames(Project project, final List<LookupElement> results, Map<String, String> entityNamespaces, final DoctrineTypes.Manager manager, final boolean useClassNameAsLookupString, final boolean isWeak) {
        for(DoctrineModel doctrineModel: EntityHelper.getModelClasses(project, entityNamespaces)) {
            String repositoryName = doctrineModel.getRepositoryName();
            if(repositoryName != null) {
                results.add(new DoctrineEntityLookupElement(repositoryName, doctrineModel.getPhpClass(), useClassNameAsLookupString, isWeak).withManager(manager));
            }
        }
    }

    public static List<LookupElement> getModelLookupElements(@NotNull Project project) {
        return getModelLookupElements(project, false, DoctrineTypes.Manager.ORM, DoctrineTypes.Manager.MONGO_DB, DoctrineTypes.Manager.COUCH_DB);
    }

    private static List<LookupElement> getModelLookupElements(Project project, boolean useClassNameAsLookupString, DoctrineTypes.Manager... managers) {

        List<LookupElement> results = new ArrayList<>();
        List<DoctrineTypes.Manager> managerList = Arrays.asList(managers);

        if(managerList.contains(DoctrineTypes.Manager.ORM)) {

            Map<String, String> entityNameMap = new HashMap<>(
                ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap()
            );

            attachRepositoryNames(project, results, entityNameMap, DoctrineTypes.Manager.ORM, useClassNameAsLookupString, false);

            // add bundle entity namespace
            attachRepositoryNames(project, results, EntityHelper.getWeakBundleNamespaces(project, entityNameMap, "Entity"), DoctrineTypes.Manager.ORM, useClassNameAsLookupString, true);
        }

        if(managerList.contains(DoctrineTypes.Manager.MONGO_DB) || managerList.contains(DoctrineTypes.Manager.COUCH_DB)) {

            Map<String, String> documentNameMap = new HashMap<>(
                ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap()
            );

            attachRepositoryNames(project, results, documentNameMap, DoctrineTypes.Manager.MONGO_DB, useClassNameAsLookupString, false);

            // add bundle document namespace
            attachRepositoryNames(project, results, EntityHelper.getWeakBundleNamespaces(project, documentNameMap, "Document"), DoctrineTypes.Manager.MONGO_DB, useClassNameAsLookupString, true);
        }

        // add custom doctrine classes
        Collection<DoctrineModelProviderParameter.DoctrineModel> doctrineModels = new ArrayList<>();
        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, doctrineModels);
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                results.add(new DoctrineEntityLookupElement(doctrineModel.getName(), doctrineModel.getPhpClass(), useClassNameAsLookupString, false));
            }
        }

        return results;
    }

}
