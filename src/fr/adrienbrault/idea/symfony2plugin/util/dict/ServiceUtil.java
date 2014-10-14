package fr.adrienbrault.idea.symfony2plugin.util.dict;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerParameterStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;

public class ServiceUtil {

    /**
     * %test%, service, \Class\Name to PhpClass
     */
    @Nullable
    public static PhpClass getResolvedClassDefinition(Project project, String serviceClassParameterName) {

        // match parameter
        if(serviceClassParameterName.startsWith("%") && serviceClassParameterName.endsWith("%")) {
            String serviceClass = ContainerCollectionResolver.resolveParameter(project, serviceClassParameterName);

            if(serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }

            return null;
        }

        // service names dont have namespaces
        if(!serviceClassParameterName.contains("\\")) {
            String serviceClass = ContainerCollectionResolver.resolveService(project, serviceClassParameterName);
            if(serviceClass != null) {
                return PhpElementsUtil.getClassInterface(project, serviceClass);
            }
        }

        // fallback to class name with and without namespaces
        return PhpElementsUtil.getClassInterface(project, serviceClassParameterName);
    }

    /**
     * Get parameter def inside xml or yaml file
     */
    public static Collection<PsiElement> getParameterDefinition(Project project, String parameterName) {

        if(parameterName.length() > 2 && parameterName.startsWith("%") && parameterName.endsWith("%")) {
            parameterName = parameterName.substring(1, parameterName.length() - 1);
        }

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        Collection<VirtualFile> fileCollection = FileBasedIndex.getInstance().getContainingFiles(ContainerParameterStubIndex.KEY, parameterName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
        for(VirtualFile virtualFile: fileCollection) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                psiElements.addAll(ServiceIndexUtil.findParameterDefinitions(psiFile, parameterName));
            }
        }

        return psiElements;

    }

    public static Collection<PsiElement> getServiceClassTargets(Project project, String value) {

        List<PsiElement> resolveResults = new ArrayList<PsiElement>();

        if(StringUtils.isBlank(value)) {
            return resolveResults;
        }

        // resolve class or parameter class
        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, value);
        if(phpClass != null) {
            resolveResults.add(phpClass);
        }

        // get parameter def target
        if(value.startsWith("%") && value.endsWith("%")) {
            resolveResults.addAll(ServiceUtil.getParameterDefinition(project, value));
        }

        return resolveResults;
    }

    public static Set<String> getTaggedServices(Project project, String tagName) {

        SymfonyProcessors.CollectProjectUniqueKeys projectUniqueKeysStrong = new SymfonyProcessors.CollectProjectUniqueKeys(project, ServicesTagStubIndex.KEY);
        FileBasedIndexImpl.getInstance().processAllKeys(ServicesTagStubIndex.KEY, projectUniqueKeysStrong, project);

        Set<String> service = new HashSet<String>();

        for(String serviceName: projectUniqueKeysStrong.getResult()) {

            List<String[]> serviceDefinitions = FileBasedIndexImpl.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
            for(String[] strings: serviceDefinitions) {
                if(Arrays.asList(strings).contains(tagName)) {
                    service.add(serviceName);
                }
            }

        }

        return service;
    }

    public static Collection<PhpClass> getTaggedClasses(Project project, String tagName) {

        List<PhpClass> phpClasses = new ArrayList<PhpClass>();

        Set<String> taggedServices = getTaggedServices(project, tagName);
        if(taggedServices.size() == 0) {
            return phpClasses;
        }

        ContainerCollectionResolver.ServiceCollector collector = new ContainerCollectionResolver.ServiceCollector(project, ContainerCollectionResolver.Source.COMPILER, ContainerCollectionResolver.Source.INDEX);
        for(String serviceName: taggedServices) {
            String resolvedService = collector.resolve(serviceName);
            if(resolvedService != null) {
                PhpClass phpClass = PhpElementsUtil.getClass(project, resolvedService);
                if(phpClass != null) {
                    phpClasses.add(phpClass);
                }
            }
        }

        return phpClasses;
    }

    public static Collection<PhpClass> getTaggedClassesWithCompiled(Project project, String tagName) {

        Set<String> uniqueClass = new HashSet<String>();

        Collection<PhpClass> taggedClasses = new ArrayList<PhpClass>();
        for(PhpClass phpClass: getTaggedClasses(project, tagName)) {
            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN != null && !uniqueClass.contains(presentableFQN)) {
                uniqueClass.add(presentableFQN);
                taggedClasses.add(phpClass);
            }
        }

        XmlTagParser xmlTagParser = ServiceXmlParserFactory.getInstance(project, XmlTagParser.class);

        List<String> taggedCompiledClasses= xmlTagParser.getTaggedClass(tagName);
        if(taggedCompiledClasses == null) {
            return taggedClasses;
        }

        for(String className: taggedCompiledClasses) {
            if(!uniqueClass.contains(className)) {
                PhpClass phpClass = PhpElementsUtil.getClass(project, className);
                if(phpClass != null) {
                    taggedClasses.add(phpClass);
                }
            }
        }

        return taggedClasses;
    }


}
