package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Some method from Php Annotations plugin to not fully set a "depends" entry on it
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationBackportUtil {

    public static Set<String> NON_ANNOTATION_TAGS = new HashSet<String>() {{
        addAll(Arrays.asList(PhpDocUtil.ALL_TAGS));
        add("@Annotation");
        add("@inheritDoc");
        add("@Enum");
        add("@inheritdoc");
        add("@Target");
    }};

    @Nullable
    public static PhpClass getAnnotationReference(PhpDocTag phpDocTag, final Map<String, String> useImports) {

        String tagName = phpDocTag.getName();
        if(tagName.startsWith("@")) {
            tagName = tagName.substring(1);
        }

        String className = tagName;
        String subNamespaceName = "";
        if(className.contains("\\")) {
            className = className.substring(0, className.indexOf("\\"));
            subNamespaceName = tagName.substring(className.length());
        }

        if(!useImports.containsKey(className)) {
            return null;
        }

        return PhpElementsUtil.getClass(phpDocTag.getProject(), useImports.get(className) + subNamespaceName);

    }

    @NotNull
    public static Map<String, String> getUseImportMap(@NotNull PhpDocTag phpDocTag) {
        PhpDocComment phpDoc = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
        if(phpDoc == null) {
            return Collections.emptyMap();
        }

        return getUseImportMap(phpDoc);
    }

    @NotNull
    public static Map<String, String> getUseImportMap(@NotNull PhpDocComment phpDocComment) {

        // search for use alias in local file
        final Map<String, String> useImports = new HashMap<>();

        PhpPsiElement scope = PhpCodeInsightUtil.findScopeForUseOperator(phpDocComment);
        if(scope == null) {
            return useImports;
        }

        for (PhpUseList phpUseList : PhpCodeInsightUtil.collectImports(scope)) {
            for (PhpUse phpUse : phpUseList.getDeclarations()) {
                String alias = phpUse.getAliasName();
                if (alias != null) {
                    useImports.put(alias, phpUse.getFQN());
                } else {
                    useImports.put(phpUse.getName(), phpUse.getFQN());
                }
            }
        }

        return useImports;
    }

    @NotNull
    public static Collection<PhpDocTag> filterValidDocTags(Collection<PhpDocTag> phpDocTags) {

        Collection<PhpDocTag> filteredPhpDocTags = new ArrayList<>();

        for(PhpDocTag phpDocTag: phpDocTags) {
            if(!NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                filteredPhpDocTags.add(phpDocTag);
            }
        }

        return filteredPhpDocTags;
    }

    public static boolean hasReference(@Nullable PhpDocComment docComment, String... className) {
        if(docComment == null) return false;

        Map<String, String> uses = AnnotationBackportUtil.getUseImportMap(docComment);

        for(PhpDocTag phpDocTag: PsiTreeUtil.findChildrenOfAnyType(docComment, PhpDocTag.class)) {
            if(!AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                PhpClass annotationReference = AnnotationBackportUtil.getAnnotationReference(phpDocTag, uses);
                if(annotationReference != null && PhpElementsUtil.isEqualClassName(annotationReference, className)) {
                    return true;
                }
            }

        }

        return false;
    }

    public static PhpDocTag getReference(@Nullable PhpDocComment docComment, String className) {
        if(docComment == null) return null;

        Map<String, String> uses = AnnotationBackportUtil.getUseImportMap(docComment);

        for(PhpDocTag phpDocTag: PsiTreeUtil.findChildrenOfAnyType(docComment, PhpDocTag.class)) {
            if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                continue;
            }

            PhpClass annotationReference = AnnotationBackportUtil.getAnnotationReference(phpDocTag, uses);
            if(annotationReference != null && PhpElementsUtil.isEqualClassName(annotationReference, className)) {
                return phpDocTag;
            }
        }

        return null;
    }

    /**
     * Get class path on "use" path statement
     */
    @Nullable
    public static String getQualifiedName(@NotNull PsiElement psiElement, @NotNull String fqn) {

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(psiElement);
        if (scopeForUseOperator == null) {
            return null;
        }

        PhpReference reference = PhpPsiUtil.getParentByCondition(psiElement, false, PhpReference.INSTANCEOF);
        String qualifiedName = PhpCodeInsightUtil.createQualifiedName(scopeForUseOperator, fqn, reference, false);
        if (!PhpLangUtil.isFqn(qualifiedName)) {
            return qualifiedName;
        }

        // @TODO: remove full fqn fallback
        if(qualifiedName.startsWith("\\")) {
            qualifiedName = qualifiedName.substring(1);
        }

        return qualifiedName;
    }

    /**
     * "AppBundle\Controller\DefaultController::fooAction" => app_default_foo"
     * "Foo\ParkResortBundle\Controller\SubController\BundleController\FooController::nestedFooAction" => foo_parkresort_sub_bundle_foo_nestedfoo"
     */
    public static String getRouteByMethod(@NotNull PhpDocTag phpDocTag) {
        PhpPsiElement method = getMethodScope(phpDocTag);
        if (method == null) {
            return null;
        }

        String name = method.getName();
        if(name == null) {
            return null;
        }

        // strip action
        if(name.endsWith("Action")) {
            name = name.substring(0, name.length() - "Action".length());
        }

        PhpClass containingClass = ((Method) method).getContainingClass();
        if(containingClass == null) {
            return null;
        }

        String[] fqn = org.apache.commons.lang.StringUtils.strip(containingClass.getFQN(), "\\").split("\\\\");

        // remove empty and controller only namespace
        List<String> filter = ContainerUtil.filter(fqn, s ->
            org.apache.commons.lang.StringUtils.isNotBlank(s) && !"controller".equalsIgnoreCase(s)
        );

        if(filter.size() == 0) {
            return null;
        }

        return org.apache.commons.lang.StringUtils.join(ContainerUtil.map(filter, s -> {
            String content = s.toLowerCase();
            if (content.endsWith("bundle") && !content.equalsIgnoreCase("bundle")) {
                return content.substring(0, content.length() - "bundle".length());
            }
            if (content.endsWith("controller") && !content.equalsIgnoreCase("controller")) {
                return content.substring(0, content.length() - "controller".length());
            }
            return content;
        }), "_") + (!name.startsWith("_") ? "_" : "") + name.toLowerCase();
    }

    @Nullable
    public static Method getMethodScope(@NotNull PhpDocTag phpDocTag) {
        PhpDocComment parentOfType = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
        if(parentOfType == null) {
            return null;
        }

        PhpPsiElement method = parentOfType.getNextPsiSibling();
        if(!(method instanceof Method)) {
            return null;
        }

        return (Method) method;
    }

    @Nullable
    public static String getClassNameReference(PhpDocTag phpDocTag, Map<String, String> useImports) {

        if(useImports.size() == 0) {
            return null;
        }

        String annotationName = phpDocTag.getName();
        if(StringUtils.isBlank(annotationName)) {
            return null;
        }

        if(annotationName.startsWith("@")) {
            annotationName = annotationName.substring(1);
        }

        String className = annotationName;
        String subNamespaceName = "";
        if(className.contains("\\")) {
            className = className.substring(0, className.indexOf("\\"));
            subNamespaceName = annotationName.substring(className.length());
        }

        if(!useImports.containsKey(className)) {
            return null;
        }

        // normalize name
        String annotationFqnName = useImports.get(className) + subNamespaceName;
        if(!annotationFqnName.startsWith("\\")) {
            annotationFqnName = "\\" + annotationFqnName;
        }

        return annotationFqnName;
    }
}
