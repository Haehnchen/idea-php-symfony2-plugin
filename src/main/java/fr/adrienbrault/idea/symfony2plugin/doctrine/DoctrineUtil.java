package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.*;
import com.jetbrains.php.lang.PhpLanguage;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineUtil {

    public static final String[] MODEL_CLASS_ANNOTATION = new String[]{
        "\\Doctrine\\ORM\\Mapping\\Entity",
        "\\TYPO3\\Flow\\Annotations\\Entity",
        "\\Doctrine\\ODM\\MongoDB\\Mapping\\Annotations\\Document",
        "\\Doctrine\\ODM\\CouchDB\\Mapping\\Annotations\\Document",
    };

    private static final Key<CachedValue<Map<String, String>>> DOCTRINE_QUERY_BUILDER_NODE_FUNCTIONS = new Key<>("DOCTRINE_QUERY_BUILDER_NODE_FUNCTIONS");

    // Per-file cache for DQL function extraction
    private static final Key<CachedValue<Map<String, String>>> FILE_DOCTRINE_FUNCTIONS = new Key<>("FILE_DOCTRINE_FUNCTIONS");

    /**
     * Index metadata file with its class and repository.
     * As of often class stay in static only context
     */
    @Nullable
    public static Collection<DoctrineClassMetadata> getClassRepositoryPair(@NotNull PsiFile psiFile) {

        Collection<DoctrineClassMetadata> pairs = null;

        if(psiFile instanceof XmlFile) {
            pairs = getClassRepositoryPair((XmlFile) psiFile);
        } else if(psiFile instanceof YAMLFile) {
            pairs = getClassRepositoryPair((YAMLFile) psiFile);
        } else if(psiFile instanceof PhpFile phpFile) {
            pairs = getClassRepositoryPair(phpFile);
        }

        return pairs;
    }

    /**
     * Extract class and repository from xml meta files
     * We support orm and all odm syntax here
     */
    @Nullable
    private static Collection<DoctrineClassMetadata> getClassRepositoryPair(@NotNull XmlFile xmlFile) {

        XmlTag rootTag = xmlFile.getRootTag();
        if(rootTag == null || !rootTag.getName().toLowerCase().startsWith("doctrine")) {
            return null;
        }

        Collection<DoctrineClassMetadata> pairs = new ArrayList<>();

        for (XmlTag xmlTag : ArrayUtils.addAll(rootTag.findSubTags("document"), rootTag.findSubTags("entity"))) {

            XmlAttribute attr = xmlTag.getAttribute("name");
            if(attr == null) {
                continue;
            }

            String value = attr.getValue();
            if(StringUtils.isBlank(value)) {
                continue;
            }

            // extract repository-class; allow nullable
            String repositoryClass = null;
            XmlAttribute repositoryClassAttribute = xmlTag.getAttribute("repository-class");
            if(repositoryClassAttribute != null) {
                String repositoryClassAttributeValue = repositoryClassAttribute.getValue();
                if(StringUtils.isNotBlank(repositoryClassAttributeValue)) {
                    repositoryClass = repositoryClassAttributeValue;
                }
            }

            // extract table name: "table" for ORM entity, "collection" for ODM document
            String tableName = null;
            XmlAttribute tableAttr = xmlTag.getAttribute("table");
            if (tableAttr != null && StringUtils.isNotBlank(tableAttr.getValue())) {
                tableName = tableAttr.getValue();
            } else {
                XmlAttribute collectionAttr = xmlTag.getAttribute("collection");
                if (collectionAttr != null && StringUtils.isNotBlank(collectionAttr.getValue())) {
                    tableName = collectionAttr.getValue();
                }
            }

            pairs.add(new DoctrineClassMetadata(value, repositoryClass, tableName));
        }

        if(pairs.isEmpty()) {
            return null;
        }

        return pairs;
    }
    /**
     * Extract class and repository from all php annotations
     * We support multiple use case like orm an so on
     */
    @NotNull
    public static Collection<DoctrineClassMetadata> getClassRepositoryPair(@NotNull PhpFile phpFile) {
        final Collection<DoctrineClassMetadata> pairs = new ArrayList<>();
        Collection<PhpClass> classes = PhpPsiUtil.findAllClasses(phpFile);

        for (PhpClass phpClass : classes) {
            PhpDocComment docComment = phpClass.getDocComment();
            if (docComment != null) {
                pairs.addAll(extractAnnotations(phpClass, docComment));
            }
            for (PhpAttribute attribute : phpClass.getAttributes()) {
                String attributeFQN = attribute.getFQN();
                if (attributeFQN == null) continue;
                if (PhpElementsUtil.isEqualClassName(attributeFQN, MODEL_CLASS_ANNOTATION)) {
                    String repositoryClass = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "repositoryClass");
                    String tableName = extractTableNameFromAttributes(phpClass);
                    pairs.add(new DoctrineClassMetadata(
                        StringUtils.stripStart(phpClass.getFQN(), "\\"),
                        repositoryClass != null ? StringUtils.stripStart(repositoryClass, "\\") : null,
                        tableName
                    ));
                }
            }
        }

        return pairs;
    }

    @Nullable
    private static String extractTableNameFromAttributes(@NotNull PhpClass phpClass) {
        for (PhpAttribute attribute : phpClass.getAttributes()) {
            String fqn = attribute.getFQN();
            if (fqn != null && PhpElementsUtil.isEqualClassName(fqn, "\\Doctrine\\ORM\\Mapping\\Table")) {
                return PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, 0, "name");
            }
        }
        return null;
    }

    public static Collection<DoctrineClassMetadata> extractAnnotations(@NotNull PhpClass phpClass, @NotNull PhpDocComment docComment) {
        Collection<DoctrineClassMetadata> result = new ArrayList<>();
        PhpDocUtil.processTagElementsByPredicate(
            docComment,
            phpDocTag -> {
                if (AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                    return;
                }

                Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(phpDocTag);
                if (fileImports.isEmpty()) {
                    return;
                }

                String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
                if (Arrays.asList(MODEL_CLASS_ANNOTATION).contains(annotationFqnName)) {
                    String tableName = extractTableNameFromAnnotations(docComment);
                    result.add(new DoctrineClassMetadata(phpClass.getPresentableFQN(), getAnnotationRepositoryClass(phpDocTag, phpClass), tableName));
                }
            },
            phpDocTag -> true
        );

        return result;
    }

    @Nullable
    private static String extractTableNameFromAnnotations(@NotNull PhpDocComment docComment) {
        Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(docComment);
        for (PhpDocTag phpDocTag : PsiTreeUtil.findChildrenOfAnyType(docComment, PhpDocTag.class)) {
            if (AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                continue;
            }
            String fqn = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
            if ("\\Doctrine\\ORM\\Mapping\\Table".equals(fqn)) {
                return AnnotationUtil.getPropertyValue(phpDocTag, "name");
            }
        }
        return null;
    }

    /**
     * Extract text: @Entity(repositoryClass="foo")
     */
    @Nullable
    public static String getAnnotationRepositoryClass(@NotNull PhpDocTag phpDocTag, @NotNull PhpClass phpClass) {
        PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
        if(phpDocAttributeList == null) {
            return null;
        }

        // @TODO: use annotation plugin
        // repositoryClass="Foobar"
        String text = phpDocAttributeList.getText();

        String repositoryClass = EntityHelper.resolveDoctrineLikePropertyClass(
            phpClass,
            text,
            "repositoryClass",
            aVoid -> AnnotationBackportUtil.getUseImportMap(phpDocTag)
        );

        if (repositoryClass == null) {
            return null;
        }

        return StringUtils.stripStart(repositoryClass, "\\");
    }


    /**
     * Try find function via "\Doctrine\ORM\Query\AST\Functions\FunctionNode" implementations
     *
     * [
     *    'concat'    => Functions\ConcatFunction::class,
     *    'substring' => Functions\SubstringFunction::class,
     * ]
     */
    public static Map<String, String> getDoctrineOrmFunctions(@NotNull Project project)
    {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            DOCTRINE_QUERY_BUILDER_NODE_FUNCTIONS,
            new CachedValueProvider<>() {
                @Override
                public @NotNull Result<Map<String, String>> compute() {
                    Map<String, String> items = new HashMap<>();

                    Collection<PhpClass> phpClasses = PhpIndex.getInstance(project).getClassesByFQN("\\Doctrine\\ORM\\Query\\Parser");

                    // Collect unique files for per-file caching
                    Set<PsiFile> files = new HashSet<>();
                    for (PhpClass phpClass : phpClasses) {
                        PsiFile file = phpClass.getContainingFile();
                        if (file instanceof PhpFile) {
                            files.add(file);
                        }
                    }

                    // Parse each file (cached per file)
                    for (PsiFile file : files) {
                        Map<String, String> fileItems = CachedValuesManager.getCachedValue(file, FILE_DOCTRINE_FUNCTIONS, () -> {
                            Map<String, String> result = new HashMap<>();
                            // Resolve classes fresh from file inside cache provider
                            for (PhpClass phpClass : PhpPsiUtil.findAllClasses((PhpFile) file)) {
                                // Only process the Parser class
                                if (!"\\Doctrine\\ORM\\Query\\Parser".equals(phpClass.getFQN())) {
                                    continue;
                                }

                                for (Field ownField : phpClass.getOwnFields()) {
                                    ownField.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                                        @Override
                                        public void visitElement(@NotNull PsiElement element) {
                                            if (element instanceof ArrayHashElement arrayHashElement) {
                                                if (arrayHashElement.getKey() instanceof StringLiteralExpression stringLiteralExpression && arrayHashElement.getValue() instanceof ClassConstantReference classConstantReference) {
                                                    String contents = stringLiteralExpression.getContents();
                                                    if (StringUtils.isNotBlank(contents) && classConstantReference.getClassReference() instanceof ClassReference classReference) {
                                                        String fqn = classReference.getFQN();
                                                        if (StringUtils.isNotBlank(fqn) && fqn.toLowerCase().contains("\\Doctrine\\ORM\\Query\\Functions".toLowerCase()) || PhpElementsUtil.isInstanceOf(element.getProject(), fqn, "\\Doctrine\\ORM\\Query\\AST\\Functions\\FunctionNode")) {
                                                            result.put(contents, fqn);
                                                        }
                                                    }
                                                }
                                            }

                                            super.visitElement(element);
                                        }
                                    });
                                }
                            }
                            return CachedValueProvider.Result.create(result, file);
                        });

                        items.putAll(fileItems);
                    }

                    return CachedValueProvider.Result.create(Collections.unmodifiableMap(items), PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE));
                }
            },
            false
        );
    }


    /**
     * Extract class and repository from all yaml files
     * We need to filter on some condition, so we dont index files which not holds meta for doctrine
     *
     * Note: index context method, so file validity in each statement
     */
    @Nullable
    private static Collection<DoctrineClassMetadata> getClassRepositoryPair(@NotNull YAMLFile yamlFile) {

        // we are indexing all yaml files for prefilter on path,
        // if false if check on parse
        String name = yamlFile.getName().toLowerCase();
        boolean iAmMetadataFile = name.contains(".odm.")
            || name.contains(".orm.")
            || name.contains(".dcm.")
            || name.contains(".mongodb.")
            || name.contains(".couchdb.");

        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(yamlFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return null;
        }

        YAMLValue topLevelValue = yamlDocument.getTopLevelValue();
        if(!(topLevelValue instanceof YAMLMapping)) {
            return null;
        }

        Collection<DoctrineClassMetadata> pairs = new ArrayList<>();
        for (YAMLKeyValue yamlKey : ((YAMLMapping) topLevelValue).getKeyValues()) {
            String keyText = yamlKey.getKeyText();
            if (StringUtils.isBlank(keyText) || !PhpNameUtil.isValidNamespaceFullName(keyText, true, PhpLanguageLevel.current(yamlFile.getProject()))) {
                continue;
            }

            boolean isValidEntry = iAmMetadataFile;

            if (!isValidEntry) {
                @Nullable String type = YamlHelper.getYamlKeyValueAsString(yamlKey, "type");
                if ("entity".equalsIgnoreCase(type) || "embeddable".equalsIgnoreCase(type) || "mappedSuperclass".equalsIgnoreCase(type)) {
                    isValidEntry = YamlHelper.getYamlKeyValue(yamlKey, "fields") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "repositoryClass") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "id") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "embedded") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "associationOverride") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "manyToOne") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "manyToMany") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "oneToMany") != null
                        || YamlHelper.getYamlKeyValue(yamlKey, "oneToOne") != null;
                }

                if (!isValidEntry) {
                    @Nullable String db = YamlHelper.getYamlKeyValueAsString(yamlKey, "db");
                    @Nullable String typeOdm = YamlHelper.getYamlKeyValueAsString(yamlKey, "type");
                    if ("documents".equalsIgnoreCase(db) || "embeddable".equalsIgnoreCase(db) || "mappedSuperclass".equalsIgnoreCase(db) || "document".equalsIgnoreCase(typeOdm)|| "embeddedDocument".equalsIgnoreCase(typeOdm)) {
                        isValidEntry = YamlHelper.getYamlKeyValue(yamlKey, "fields") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "embedOne") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "embedMany") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "referenceOne") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "referenceMany") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "collection") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "db") != null;
                    }
                }

                if (!isValidEntry) {
                    @Nullable String db = YamlHelper.getYamlKeyValueAsString(yamlKey, "db");
                    if ("documents".equalsIgnoreCase(db) || "embeddable".equalsIgnoreCase(db) || "mappedSuperclass".equalsIgnoreCase(db)) {
                        isValidEntry = YamlHelper.getYamlKeyValue(yamlKey, "fields") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "embedOne") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "embedMany") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "referenceOne") != null
                            || YamlHelper.getYamlKeyValue(yamlKey, "referenceMany") != null;
                    }
                }
            }

            if (!isValidEntry) {
                continue;
            }

            String repositoryClassValue = YamlHelper.getYamlKeyValueAsString(yamlKey, "repositoryClass");

            // check blank condition
            String repositoryClass = null;
            if(StringUtils.isNotBlank(repositoryClassValue)) {
                repositoryClass = repositoryClassValue;
            }

            // extract table name: "table" for ORM, "collection" for ODM
            String tableNameValue = YamlHelper.getYamlKeyValueAsString(yamlKey, "table");
            if (tableNameValue == null) {
                tableNameValue = YamlHelper.getYamlKeyValueAsString(yamlKey, "collection");
            }
            String tableName = StringUtils.isNotBlank(tableNameValue) ? tableNameValue : null;

            pairs.add(new DoctrineClassMetadata(keyText, repositoryClass, tableName));
        }

        if(pairs.isEmpty()) {
            return null;
        }

        return pairs;
    }

}
