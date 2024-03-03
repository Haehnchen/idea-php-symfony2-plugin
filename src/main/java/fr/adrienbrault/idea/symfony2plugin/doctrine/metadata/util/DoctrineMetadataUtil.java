package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver.*;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.lookup.DoctrineRepositoryLookupElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.DoctrineMetadataFileStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataUtil {

    private static final Key<CachedValue<Set<String>>> CLASS_KEYS = new Key<>("CLASS_KEYS");

    private static final DoctrineMappingDriverInterface[] MAPPING_DRIVERS = new DoctrineMappingDriverInterface[] {
        new DoctrinePhpMappingDriver(),
        new DoctrinePhpAttributeMappingDriver(),
        new DoctrineXmlMappingDriver(),
        new DoctrineYamlMappingDriver(),
    };

    @NotNull
    public static Collection<LookupElement> getObjectRepositoryLookupElements(@NotNull Project project) {
        PhpIndex index = PhpIndex.getInstance(project);
        Collection<PhpClass> collection = index.getAllSubclasses("\\Doctrine\\Common\\Persistence\\ObjectRepository");
        collection.addAll(index.getAllSubclasses("\\Doctrine\\Persistence\\ObjectRepository"));

        return new ArrayList<>(DoctrineRepositoryLookupElement.create(collection));
    }

    /**
     * Try to find repository class on models scope on its metadata definition
     */
    @Nullable
    public static PhpClass getClassRepository(final @NotNull Project project, final @NotNull String className) {

        for (VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, className, GlobalSearchScope.allScope(project))) {

            final String[] phpClass = {null};

            FileBasedIndex.getInstance().processValues(DoctrineMetadataFileStubIndex.KEY, className, virtualFile, (virtualFile1, model) -> {
                if (phpClass[0] != null  || model == null || model.getRepositoryClass() == null) {
                    return true;
                }

                // piping value out of this index thread
                phpClass[0] = model.getRepositoryClass();

                return true;
            }, GlobalSearchScope.allScope(project));

            if(phpClass[0] != null) {
                return PhpElementsUtil.getClassInsideNamespaceScope(project, className, phpClass[0]);
            }
        }

        return null;
    }

    @NotNull
    public static Collection<VirtualFile> findMetadataFiles(@NotNull Project project, @NotNull String className) {

        final Collection<VirtualFile> virtualFiles = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(DoctrineMetadataFileStubIndex.KEY, new HashSet<>(Collections.singletonList(className)), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        }, GlobalSearchScope.allScope(project));

        return virtualFiles;
    }
    @NotNull
    public static Collection<VirtualFile> findMetadataForRepositoryClass(@NotNull PhpClass phpClass) {
        return findMetadataForRepositoryClass(
            phpClass.getProject(),
            StringUtils.stripStart(phpClass.getPresentableFQN(), "\\")
        );
    }

    private static final Key<CachedValue<Map<String, Collection<String>>>> DOCTRINE_REPOSITORY_CACHE;

    static {
        DOCTRINE_REPOSITORY_CACHE = new Key<>("DOCTRINE_REPOSITORY_CACHE");
    }

    @NotNull
    public static Collection<VirtualFile> findMetadataForRepositoryClass(final @NotNull Project project, @NotNull String repositoryClass) {
        Map<String, Collection<String>> cache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            DOCTRINE_REPOSITORY_CACHE,
            () -> {
                Map<String, Collection<String>> repositoryMap = new HashMap<>();
                for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
                    for (DoctrineModelInterface repositoryDefinition : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                        if(StringUtils.isBlank(repositoryDefinition.getRepositoryClass())) {
                            continue;
                        }

                        PhpClass phpClass = PhpElementsUtil.getClassInsideNamespaceScope(project, key, repositoryDefinition.getRepositoryClass());
                        if(phpClass != null) {
                            String presentableFQN = phpClass.getPresentableFQN();
                            if(!repositoryMap.containsKey(presentableFQN)) {
                                repositoryMap.put(presentableFQN, new HashSet<>());
                            }

                            repositoryMap.get(presentableFQN).add(key);
                        }
                    }
                }

                return CachedValueProvider.Result.create(repositoryMap, FileIndexCaches.getModificationTrackerForIndexId(project, DoctrineMetadataFileStubIndex.KEY));
            },
            false
        );

        repositoryClass = StringUtils.stripStart(repositoryClass,"\\");
        if(!cache.containsKey(repositoryClass)) {
            return Collections.emptyList();
        }

        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (String s : cache.get(repositoryClass)) {
            virtualFiles.addAll(
                FileBasedIndex.getInstance().getContainingFiles(DoctrineMetadataFileStubIndex.KEY, s, GlobalSearchScope.allScope(project))
            );
        }

        return virtualFiles;
    }

    /**
     * Find metadata model in which the given repository class is used
     * eg "@ORM\Entity(repositoryClass="FOOBAR")", xml or yaml
     */
    @NotNull
    public static Collection<DoctrineModelInterface> findMetadataModelForRepositoryClass(final @NotNull Project project, @NotNull String repositoryClass) {
        repositoryClass = StringUtils.stripStart(repositoryClass,"\\");

        Collection<DoctrineModelInterface> models = new ArrayList<>();

        for (String key : FileIndexCaches.getIndexKeysCache(project, CLASS_KEYS, DoctrineMetadataFileStubIndex.KEY)) {
            for (DoctrineModelInterface repositoryDefinition : FileBasedIndex.getInstance().getValues(DoctrineMetadataFileStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                String myRepositoryClass = repositoryDefinition.getRepositoryClass();
                if(StringUtils.isBlank(myRepositoryClass) ||
                    !repositoryClass.equalsIgnoreCase(StringUtils.stripStart(myRepositoryClass, "\\"))) {
                    continue;
                }

                models.add(repositoryDefinition);
            }
        }

        return models;
    }

    @NotNull
    public static Collection<Pair<String, PsiElement>> getTables(@NotNull Project project) {

        Collection<Pair<String, PsiElement>> pair = new ArrayList<>();

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
                    pair.add(new Pair<>(table, psiFile));
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

        Collection<PhpClass> phpClasses = new ArrayList<>();
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
            PsiElement firstParent = PsiTreeUtil.findFirstParent(psiElement, psiElement1 -> {
                if (!(psiElement1 instanceof XmlTag)) {
                    return false;
                }

                String name = ((XmlTag) psiElement1).getName();
                return name.equals("entity") || name.equals("document") || name.equals("embedded") || name.equals("embedded-document");
            });

            if(firstParent instanceof XmlTag) {
                String name = ((XmlTag) firstParent).getAttributeValue("name");
                if(StringUtils.isNotBlank(name)) {
                    return name;
                }
            }
        } else if(psiElement.getContainingFile() instanceof YAMLFile) {
            PsiFile psiFile = psiElement.getContainingFile();
            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument != null) {
                YAMLValue topLevelValue = yamlDocument.getTopLevelValue();
                if(topLevelValue instanceof YAMLMapping) {
                    YAMLKeyValue firstChild = PsiTreeUtil.findChildOfType(topLevelValue, YAMLKeyValue.class);
                    if(firstChild != null) {
                        String keyText = firstChild.getKeyText();
                        if(StringUtils.isNotBlank(keyText)) {
                            return keyText;
                        }
                    }
                }
            }
        }

        // @TODO: yml scope

        return null;
    }

    @NotNull
    public static Collection<PhpClass> getClassInsideScope(@NotNull PsiElement psiElement, @NotNull String originValue) {
        Collection<PhpClass> classesInterface = new ArrayList<>();
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
