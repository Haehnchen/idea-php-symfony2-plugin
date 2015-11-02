package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup.DoctrineRepositoryLookupElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataUtil {

    private static final Key<CachedValue<Set<String>>> CLASS_KEYS = new Key<CachedValue<Set<String>>>("CLASS_KEYS");

    private static DoctrineMappingDriverInterface[] MAPPING_DRIVERS = new DoctrineMappingDriverInterface[] {
        new DoctrineXmlMappingDriver(),
        new DoctrineYamlMappingDriver(),
        new DoctrinePhpMappingDriver(),
    };

    @NotNull
    public static Collection<LookupElement> getObjectRepositoryLookupElements(@NotNull Project project) {
        return new ArrayList<LookupElement>(DoctrineRepositoryLookupElement.create(PhpIndex.getInstance(project).getAllSubclasses("\\Doctrine\\Common\\Persistence\\ObjectRepository")));
    }

    /**
     * Try to find repository class on models scope on its metadata definition
     */
    @Nullable
    public static PhpClass getClassRepository(final @NotNull Project project, final @NotNull String className) {

        final PhpClass[] phpClass = {null};
        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, className, GlobalSearchScope.allScope(project))) {

            FileBasedIndex.getInstance().processValues(DoctrineMetadataFileStubIndex.KEY, className, virtualFile, new FileBasedIndex.ValueProcessor<String>() {
                @Override
                public boolean process(VirtualFile virtualFile, String s) {
                    if (s == null || phpClass[0] != null) {
                        return true;
                    }

                    phpClass[0] = PhpElementsUtil.getClassInsideNamespaceScope(project, className, s);
                    return true;
                }
            }, GlobalSearchScope.allScope(project));

            if(phpClass[0] != null) {
                return phpClass[0];
            }
        }

        return phpClass[0];
    }

    @NotNull
    public static Collection<VirtualFile> findMetadataFiles(@NotNull Project project, @NotNull String className) {

        final Collection<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();

        FileBasedIndex.getInstance().getFilesWithKey(DoctrineMetadataFileStubIndex.KEY, new HashSet<String>(Collections.singletonList(className)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {
                virtualFiles.add(virtualFile);
                return true;
            }
        }, GlobalSearchScope.allScope(project));

        return virtualFiles;
    }
    @NotNull
    public static Collection<VirtualFile> findMetadataForRepositoryClass(@NotNull PhpClass phpClass) {
        String presentableFQN = phpClass.getPresentableFQN();
        if(presentableFQN == null) {
            return Collections.emptyList();
        }

        if(presentableFQN.startsWith("\\")) {
            presentableFQN = presentableFQN.substring(1);
        }

        return findMetadataForRepositoryClass(phpClass.getProject(), presentableFQN);
    }

    private static final Key<CachedValue<Map<String, Collection<String>>>> DOCTRINE_REPOSITORY_CACHE = new Key<CachedValue<Map<String, Collection<String>>>>("DOCTRINE_REPOSITORY_CACHE");

    @NotNull
    public static Collection<VirtualFile> findMetadataForRepositoryClass(final @NotNull Project project, @NotNull String repositoryClass) {

        CachedValue<Map<String, Collection<String>>> cache = project.getUserData(DOCTRINE_REPOSITORY_CACHE);
        if(cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, Collection<String>>>() {
                @Nullable
                @Override
                public Result<Map<String, Collection<String>>> compute() {
                    Map<String, Collection<String>> repositoryMap = new HashMap<String, Collection<String>>();
                    for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
                        for (String repositoryDefinition : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                            if(StringUtils.isBlank(repositoryDefinition)) {
                                continue;
                            }

                            PhpClass phpClass = PhpElementsUtil.getClassInsideNamespaceScope(project, key, repositoryDefinition);
                            if(phpClass != null && phpClass.getPresentableFQN() != null) {
                                String presentableFQN = phpClass.getPresentableFQN();
                                if(!repositoryMap.containsKey(presentableFQN)) {
                                    repositoryMap.put(presentableFQN, new HashSet<String>());
                                }

                                repositoryMap.get(presentableFQN).add(key);
                            }
                        }
                    }

                    return Result.create(repositoryMap, PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);

            project.putUserData(DOCTRINE_REPOSITORY_CACHE, cache);
        }

        if(!cache.getValue().containsKey(repositoryClass)) {
            return Collections.emptyList();
        }

        Set<VirtualFile> virtualFiles = new HashSet<VirtualFile>();

        for (String s : cache.getValue().get(repositoryClass)) {
            virtualFiles.addAll(
                FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, s, GlobalSearchScope.allScope(project))
            );
        }

        return virtualFiles;
    }

    @NotNull
    public static Collection<Pair<String, PsiElement>> getTables(@NotNull Project project) {

        Collection<Pair<String, PsiElement>> pair = new ArrayList<Pair<String, PsiElement>>();

        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile == null) {
                    continue;
                }

                DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, key);

                for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                    DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                    if(metadata == null) {
                        continue;
                    }

                    String table = metadata.getTable();
                    if(table == null) {
                        continue;
                    }

                    // @TODO: add target
                    pair.add(new Pair<String, PsiElement>(table, psiFile));
                }
            }
        }

        return pair;
    }

    @Nullable
    public static DoctrineMetadataModel getMetadataByTable(@NotNull Project project, @NotNull String tableName) {

        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(psiFile == null) {
                    continue;
                }

                DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, key);

                for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                    DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                    if(metadata == null) {
                        continue;
                    }

                    String table = metadata.getTable();
                    if(table != null && tableName.equals(table)) {
                        return metadata;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public static DoctrineMetadataModel getModelFields(@NotNull Project project, @NotNull String className) {

        for (VirtualFile file : findMetadataFiles(project, className)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if(psiFile == null) {
                continue;
            }

            DoctrineMappingDriverArguments arguments = new DoctrineMappingDriverArguments(project, psiFile, className);
            for (DoctrineMappingDriverInterface mappingDriver : MAPPING_DRIVERS) {
                DoctrineMetadataModel metadata = mappingDriver.getMetadata(arguments);
                if(metadata != null) {
                    return metadata;
                }
            }
        }

        return null;
    }

    @NotNull
    public static Collection<PhpClass> getModels(@NotNull Project project) {

        Collection<PhpClass> phpClasses = new ArrayList<PhpClass>();
        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, key);
            if(classInterface != null) {
                phpClasses.add(classInterface);
            }
        }

        return phpClasses;
    }

    @Nullable
    public static DoctrineManagerEnum findManagerByScope(@NotNull PsiElement psiElement) {
        String name = psiElement.getContainingFile().getName();
        Matcher matcher = Pattern.compile(".(mongodb|couchdb|orm|document|odm).(xml|yaml|yml)$", Pattern.CASE_INSENSITIVE).matcher(name);

        if(matcher.find()) {
            DoctrineManagerEnum managerEnum = DoctrineManagerEnum.getEnumFromString(matcher.group(1));
            if(managerEnum != null) {
                return managerEnum;
            }
        }

        // @TODO: implement psiElement position scope
        return null;
    }

    @Nullable
    public static String findModelNameInScope(@NotNull PsiElement psiElement) {

        if(psiElement.getLanguage().equals(XMLLanguage.INSTANCE)) {
            PsiElement firstParent = PsiTreeUtil.findFirstParent(psiElement, new Condition<PsiElement>() {
                @Override
                public boolean value(PsiElement psiElement) {
                    if (!(psiElement instanceof XmlTag)) {
                        return false;
                    }

                    String name = ((XmlTag) psiElement).getName();
                    return name.equals("entity") || name.equals("document") || name.equals("embedded") || name.equals("embedded-document");
                }
            });

            if(firstParent instanceof XmlTag) {
                String name = ((XmlTag) firstParent).getAttributeValue("name");
                if(StringUtils.isNotBlank(name)) {
                    return name;
                }
            }
        }

        // @TODO: yml scope

        return null;
    }

    @NotNull
    public static Collection<PhpClass> getClassInsideScope(@NotNull PsiElement psiElement, @NotNull String originValue) {
        Collection<PhpClass> classesInterface = new ArrayList<PhpClass>();
        String modelNameInScope = DoctrineMetadataUtil.findModelNameInScope(psiElement);
        if(modelNameInScope != null) {
            PhpClass classInsideNamespaceScope = PhpElementsUtil.getClassInsideNamespaceScope(psiElement.getProject(), modelNameInScope, originValue);
            if(classInsideNamespaceScope != null) {
                classesInterface = Collections.singletonList(classInsideNamespaceScope);
            }
        } else {
            classesInterface = PhpElementsUtil.getClassesInterface(psiElement.getProject(), originValue);
        }
        // @TODO: multi classes
        return classesInterface;
    }
}
