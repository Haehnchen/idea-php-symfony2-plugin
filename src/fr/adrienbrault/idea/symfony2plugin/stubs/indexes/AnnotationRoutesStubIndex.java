package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpUse;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotationRoutesStubIndex extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.annotation_routes");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {
        return new DataIndexer<String, Void, FileContent>() {
            @NotNull
            @Override
            public Map<String, Void> map(FileContent inputData) {
                final Map<String, Void> map = new THashMap<String, Void>();

                PsiFile psiFile = inputData.getPsiFile();
                if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                    return map;
                }

                if(!(inputData.getPsiFile() instanceof PhpFile)) {
                    return map;
                }

                if(!RoutesStubIndex.isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                psiFile.accept(new MyPsiRecursiveElementWalkingVisitor(map));

                return map;
            }
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 8;
    }

    public static Map<String, String> getFileUseImports(PsiFile psiFile) {

        // search for use alias in local file
        final Map<String, String> useImports = new HashMap<String, String>();
        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element instanceof PhpUse) {
                    visitUse((PhpUse) element);
                }
                super.visitElement(element);
            }

            private void visitUse(PhpUse phpUse) {
                String alias = phpUse.getAliasName();
                if(alias != null) {
                    useImports.put(alias, phpUse.getOriginal());
                } else {
                    useImports.put(phpUse.getName(), phpUse.getOriginal());
                }

            }

        });

        return useImports;
    }

    @Nullable
    public static String getClassNameReference(PhpDocTag phpDocTag) {
        return getClassNameReference(phpDocTag, getFileUseImports(phpDocTag.getContainingFile()));
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

    private static class MyPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {

        private final Map<String, Void> map;
        private Map<String, String> fileImports;

        public MyPsiRecursiveElementWalkingVisitor(Map<String, Void> map) {
            this.map = map;
        }

        @Override
        public void visitElement(PsiElement element) {
            if ((element instanceof PhpDocTag)) {
                visitPhpDocTag((PhpDocTag) element);
            }
            super.visitElement(element);
        }

        public void visitPhpDocTag(PhpDocTag phpDocTag) {

            // "@var" and user non related tags dont need an action
            if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
                return;
            }

            // init file imports
            if(this.fileImports == null) {
                this.fileImports = getFileUseImports(phpDocTag.getContainingFile());
            }

            if(this.fileImports.size() == 0) {
                return;
            }

            String annotationFqnName = AnnotationRoutesStubIndex.getClassNameReference(phpDocTag, this.fileImports);
            if("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route".equals(annotationFqnName)) {
                PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
                if(phpDocAttributeList != null) {
                    // @TODO: use pattern
                    Matcher matcher = Pattern.compile("name\\s*=\\s*\"(\\w+)\"").matcher(phpDocAttributeList.getText());
                    if (matcher.find()) {
                        map.put(matcher.group(1), null);
                    }
                }
            }
        }

    }
}



