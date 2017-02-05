package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AnnotationElementWalkingVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        } else if(psiFile instanceof PhpFile) {
            pairs = getClassRepositoryPair((PhpFile) psiFile);
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
    @Nullable
    private static Collection<Pair<String, String>> getClassRepositoryPair(@NotNull PhpFile phpFile) {

        final Collection<Pair<String, String>> pairs = new ArrayList<>();

        phpFile.acceptChildren(new AnnotationElementWalkingVisitor(phpDocTag -> {
            PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
            if (phpDocComment == null) {
                return false;
            }

            PhpPsiElement phpClass = phpDocComment.getNextPsiSibling();
            if (!(phpClass instanceof PhpClass)) {
                return false;
            }

            PhpClass phpClassScope = (PhpClass) phpClass;

            pairs.add(Pair.create(
                phpClassScope.getPresentableFQN(),
                getAnnotationRepositoryClass(phpDocTag, phpClassScope))
            );

            return false;
        }, MODEL_CLASS_ANNOTATION));

        return pairs;
    }

    /**
     * Extract text: @Entity(repositoryClass="foo")
     */
    @Nullable
    private static String getAnnotationRepositoryClass(@NotNull PhpDocTag phpDocTag, @NotNull PhpClass phpClass) {
        PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
        if(phpDocAttributeList == null) {
            return null;
        }

        // repositoryClass="Foobar"
        String text = phpDocAttributeList.getText();
        Matcher matcher = Pattern.compile("repositoryClass\\s*=\\s*\"([^\"]*)\"").matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // repositoryClass=Foobar::class
        // @TODO: use annotation plugin
        matcher = Pattern.compile("repositoryClass\\s*=\\s*([^\\s:]*)::class").matcher(text);
        if (matcher.find()) {
            PhpClass classConstant = EntityHelper.getAnnotationRepositoryClass(phpClass, matcher.group(1));
            if(classConstant != null) {
                return StringUtils.stripStart(classConstant.getFQN(), "\\");
            }
        }

        return null;
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
        boolean iAmMetadataFile = name.contains(".odm.") || name.contains(".orm.") || name.contains(".mongodb.") || name.contains(".couchdb.");

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
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            String repositoryClass = YamlHelper.getYamlKeyValueAsString(yamlKey, "repositoryClass");

            // fine repositoryClass exists a valid metadata file
            if(!iAmMetadataFile && repositoryClass != null) {
                iAmMetadataFile = true;
            }

            // currently not valid metadata file find valid keys
            // else we are not allowed to store values
            if(!iAmMetadataFile) {
                Set<String> keySet = YamlHelper.getKeySet(yamlKey);
                if(keySet == null) {
                    continue;
                }

                if(!(keySet.contains("fields") || keySet.contains("id") || keySet.contains("collection") || keySet.contains("db") || keySet.contains("indexes"))) {
                    continue;
                }

                iAmMetadataFile = true;
            }

            pairs.add(Pair.create(keyText, repositoryClass));
        }

        if(pairs.size() == 0) {
            return null;
        }

        return pairs;
    }

}
