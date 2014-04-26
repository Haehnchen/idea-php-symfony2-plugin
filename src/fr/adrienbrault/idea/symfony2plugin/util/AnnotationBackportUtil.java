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
    public static PhpClass getAnnotationReference(PhpDocTag phpDocTag) {

        PsiFile containingFile;
        try {
            containingFile = phpDocTag.getContainingFile();
        } catch (PsiInvalidElementAccessException e) {
            return null;
        }

        // check annoation in current namespace
        String tagName = phpDocTag.getName();
        if(tagName.startsWith("@")) {
            tagName = tagName.substring(1);
        }

        PhpNamespace phpNamespace = PsiTreeUtil.getParentOfType(phpDocTag, PhpNamespace.class);
        if(phpNamespace != null) {
            String currentNsClass = phpNamespace.getFQN() + "\\" + tagName;
            PhpClass phpClass = PhpElementsUtil.getClass(phpDocTag.getProject(), currentNsClass);
            if(phpClass != null) {
                return phpClass;
            }
        }

        // resolve class name on imports and aliases
        if(getUseImportMap(containingFile).size() == 0) {
            return null;
        }

        return getAnnotationReference(phpDocTag, getUseImportMap(containingFile));

    }

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

}
