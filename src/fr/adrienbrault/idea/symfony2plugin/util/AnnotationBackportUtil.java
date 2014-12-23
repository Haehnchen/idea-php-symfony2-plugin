package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.PhpUse;
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

}
