package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.refactoring.PhpNameUtil;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AnnotationElementWalkingVisitor;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AttributeElementWalkingVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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

    /**
     * Index metadata file with its class and repository.
     * As of often class stay in static only context
     */
    @Nullable
    public static Collection<Pair<String, String>> getClassRepositoryPair(@NotNull PsiFile psiFile) {

        Collection<Pair<String, String>> pairs = null;

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
    private static Collection<Pair<String, String>> getClassRepositoryPair(@NotNull XmlFile xmlFile) {

        XmlTag rootTag = xmlFile.getRootTag();
        if(rootTag == null || !rootTag.getName().toLowerCase().startsWith("doctrine")) {
            return null;
        }

        Collection<Pair<String, String>> pairs = new ArrayList<>();

        for (XmlTag xmlTag : (XmlTag[]) ArrayUtils.addAll(rootTag.findSubTags("document"), rootTag.findSubTags("entity"))) {

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

            pairs.add(Pair.create(value, repositoryClass));
        }

        if(pairs.size() == 0) {
            return null;
        }

        return pairs;
    }
    /**
     * Extract class and repository from all php annotations
     * We support multiple use case like orm an so on
     */
    @NotNull
    public static Collection<Pair<String, String>> getClassRepositoryPair(@NotNull PhpFile phpFile) {
        final Collection<Pair<String, String>> pairs = new ArrayList<>();
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
                    String repositoryClass = PhpPsiAttributesUtil.getAttributeValueByNameAsString(attribute, "repositoryClass");
                    pairs.add(Pair.create(StringUtils.stripStart(phpClass.getFQN(), "\\"),
                            repositoryClass != null ? StringUtils.stripStart(repositoryClass, "\\") : null));
                }
            }
        }

        return pairs;
    }

    public static Collection<Pair<String, String>> extractAnnotations(@NotNull PhpClass phpClass, @NotNull PhpDocComment docComment) {
        Collection<Pair<String, String>> result = new ArrayList<>();
        PhpDocUtil.processTagElementsByName(docComment, null, phpDocTag -> {
            if (AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                return true;
            }

            Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(phpDocTag);
            if (fileImports.isEmpty()) {
                return true;
            }

            String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, fileImports);
            if (ContainerUtil.exists(MODEL_CLASS_ANNOTATION, c -> c.equals(annotationFqnName))) {
                result.add(Pair.create(phpClass.getPresentableFQN(), getAnnotationRepositoryClass(phpDocTag, phpClass)));
            }
            return true;
        });
        return result;
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
            aVoid -> AnnotationUtil.getUseImportMap(phpDocTag)
        );

        if (repositoryClass == null) {
            return null;
        }

        return StringUtils.stripStart(repositoryClass, "\\");
    }

    /**
     * Extract class and repository from all yaml files
     * We need to filter on some condition, so we dont index files which not holds meta for doctrine
     *
     * Note: index context method, so file validity in each statement
     */
    @Nullable
    private static Collection<Pair<String, String>> getClassRepositoryPair(@NotNull YAMLFile yamlFile) {

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

        Collection<Pair<String, String>> pairs = new ArrayList<>();
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

            pairs.add(Pair.create(keyText, repositoryClass));
        }

        if(pairs.size() == 0) {
            return null;
        }

        return pairs;
    }

}
