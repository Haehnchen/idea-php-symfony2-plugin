package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Some method from Php Annotations plugin to not fully set a "depends" entry on it
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

    public static Map<String, String> getUseImportMap(PhpDocComment phpDocComment) {

        // search for use alias in local file
        final Map<String, String> useImports = new HashMap<String, String>();

        PhpNamespace phpNamespace = PsiTreeUtil.getParentOfType(phpDocComment, PhpNamespace.class);
        if(phpNamespace == null) {
            return useImports;
        }

        phpNamespace.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PhpUse) {
                    visitUse((PhpUse) element);
                }
                super.visitElement(element);
            }

            private void visitUse(PhpUse phpUse) {
                String alias = phpUse.getAliasName();
                if (alias != null) {
                    useImports.put(alias, phpUse.getOriginal());
                } else {
                    useImports.put(phpUse.getName(), phpUse.getOriginal());
                }

            }

        });

        return useImports;
    }

    /**
     * Collect file use imports and resolve alias with their class name
     *
     * @param psiFile file to search
     * @return map with class names as key and fqn on value
     */
    public static Map<String, String> getUseImportMap(PsiFile psiFile) {

        // search for use alias in local file
        final Map<String, String> useImports = new HashMap<String, String>();

        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PhpUse) {
                    visitUse((PhpUse) element);
                }
                super.visitElement(element);
            }

            private void visitUse(PhpUse phpUse) {
                String alias = phpUse.getAliasName();
                if (alias != null) {
                    useImports.put(alias, phpUse.getOriginal());
                } else {
                    useImports.put(phpUse.getName(), phpUse.getOriginal());
                }

            }

        });

        return useImports;
    }

    @NotNull
    public static Collection<PhpDocTag> filterValidDocTags(Collection<PhpDocTag> phpDocTags) {

        Collection<PhpDocTag> filteredPhpDocTags = new ArrayList<PhpDocTag>();

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

    @Nullable
    public static String getAnnotationRouteName(@Nullable String rawDocText) {

        if(rawDocText == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("name\\s*=\\s*\"([\\w\\.-]+)\"").matcher(rawDocText);
        if (matcher.find()) {
            return matcher.group(1);
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
     * "@SensioBlogBundle/Controller/PostController.php => sensio_blog_post_index"
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

        if(name.endsWith("Action")) {
            name = name.substring(0, name.length() - "Action".length());
        }

        PhpClass containingClass = ((Method) method).getContainingClass();
        if(containingClass == null) {
            return null;
        }

        String fqn = containingClass.getFQN();
        if(fqn != null) {
            Matcher matcher = Pattern.compile("\\\\(\\w+)Bundle\\\\Controller\\\\(\\w+)Controller").matcher(fqn);
            if (matcher.find()) {
                return String.format("%s_%s_%s",
                    fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(matcher.group(1)),
                    fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(matcher.group(2)),
                    name
                );
            }
        }

        return null;
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

}
