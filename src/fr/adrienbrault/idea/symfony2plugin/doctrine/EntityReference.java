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
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.DocumentNamespacesParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.component.EntityNamesServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineEntityLookupElement;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineTypes;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.DoctrineModelProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
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

    private static void attachRepositoryNames(Project project, final List<LookupElement> results, Map<String, String> entityNamespaces, final DoctrineTypes.Manager manager, final boolean useClassNameAsLookupString, final boolean isWeak) {
        for(DoctrineModel doctrineModel: EntityHelper.getModelClasses(project, entityNamespaces)) {
            String repositoryName = doctrineModel.getRepositoryName();
            if(repositoryName != null) {
                results.add(new DoctrineEntityLookupElement(repositoryName, doctrineModel.getPhpClass(), useClassNameAsLookupString, isWeak).withManager(manager));
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

            Map<String, String> entityNameMap = new HashMap<String, String>(
                ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap()
            );

            attachRepositoryNames(project, results, entityNameMap, DoctrineTypes.Manager.ORM, useClassNameAsLookupString, false);

            // add bundle entity namespace
            attachRepositoryNames(project, results, getWeakBundleNamespaces(project, entityNameMap, "Entity"), DoctrineTypes.Manager.ORM, useClassNameAsLookupString, true);
        }

        if(managerList.contains(DoctrineTypes.Manager.MONGO_DB)) {

            Map<String, String> documentNameMap = new HashMap<String, String>(
                ServiceXmlParserFactory.getInstance(project, DocumentNamespacesParser.class).getNamespaceMap()
            );

            attachRepositoryNames(project, results, documentNameMap, DoctrineTypes.Manager.MONGO_DB, useClassNameAsLookupString, false);

            // add bundle document namespace
            attachRepositoryNames(project, results, getWeakBundleNamespaces(project, documentNameMap, "Document"), DoctrineTypes.Manager.MONGO_DB, useClassNameAsLookupString, true);
        }

        // add custom doctrine classes
        Collection<DoctrineModelProviderParameter.DoctrineModel> doctrineModels = new ArrayList<DoctrineModelProviderParameter.DoctrineModel>();
        DoctrineModelProviderParameter containerLoaderExtensionParameter = new DoctrineModelProviderParameter(project, doctrineModels);
        for(DoctrineModelProvider provider : EntityHelper.MODEL_POINT_NAME.getExtensions()) {
            for(DoctrineModelProviderParameter.DoctrineModel doctrineModel: provider.collectModels(containerLoaderExtensionParameter)) {
                results.add(new DoctrineEntityLookupElement(doctrineModel.getName(), doctrineModel.getPhpClass(), useClassNameAsLookupString, false));
            }
        }

        return results;
    }

    private static Map<String, String> getWeakBundleNamespaces(Project project, Map<String, String> entityNameMap, String subFolder) {

        Map<String, String> missingMap = new HashMap<String, String>();

        Collection<SymfonyBundle> symfonyBundles = new SymfonyBundleUtil(project).getBundles();
        for(SymfonyBundle symfonyBundle: symfonyBundles) {
            if(!symfonyBundle.isTestBundle()) {
                String bundleName = symfonyBundle.getName();

                if(!entityNameMap.containsKey(bundleName) && symfonyBundle.getRelative(subFolder) != null) {
                    String entityNs = symfonyBundle.getNamespaceName() + subFolder;
                    missingMap.put(bundleName, entityNs);
                }
            }
        }

        return missingMap;
    }


}
