package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.JsonRoute;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RouteInterface;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AnnotationRoutesStubIndex extends FileBasedIndexExtension<String, RouteInterface> {

    public static final ID<String, RouteInterface> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.annotation_routes_json");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static JsonDataExternalizer JSON_EXTERNALIZER = new JsonDataExternalizer();

    @NotNull
    @Override
    public ID<String, RouteInterface> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, RouteInterface, FileContent> getIndexer() {
        return new DataIndexer<String, RouteInterface, FileContent>() {
            @NotNull
            @Override
            public Map<String, RouteInterface> map(@NotNull FileContent inputData) {
                final Map<String, RouteInterface> map = new THashMap<String, RouteInterface>();

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

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<RouteInterface> getValueExternalizer() {
        return JSON_EXTERNALIZER;
    }

    @NotNull
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
        return 9;
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
                    useImports.put(alias, phpUse.getFQN());
                } else {
                    useImports.put(phpUse.getName(), phpUse.getFQN());
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

        private final Map<String, RouteInterface> map;
        private Map<String, String> fileImports;

        public MyPsiRecursiveElementWalkingVisitor(Map<String, RouteInterface> map) {
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
            if(!"\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route".equals(annotationFqnName)) {
                return;
            }

            PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
            if(!(phpDocAttributeList instanceof PhpPsiElement)) {
                return;
            }

            String routeName = AnnotationBackportUtil.getAnnotationRouteName(phpDocAttributeList.getText());
            if(routeName == null) {
                routeName = AnnotationBackportUtil.getRouteByMethod(phpDocTag);
            }

            if(routeName != null && StringUtils.isNotBlank(routeName)) {

                JsonRoute route = new JsonRoute(routeName);

                String path = "";

                // get class scope pattern
                String classPath = getClassRoutePattern(phpDocTag);
                if(classPath != null) {
                    path += classPath;
                }

                // extract method path
                PhpPsiElement firstPsiChild = ((PhpPsiElement) phpDocAttributeList).getFirstPsiChild();
                if(firstPsiChild instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) firstPsiChild).getContents();
                    if(StringUtils.isNotBlank(contents)) {
                        path += contents;
                    }
                }

                if (path.length() > 0) {
                    route.setPath(path);
                }

                route.setController(getController(phpDocTag));

                map.put(routeName, route);
            }
        }

        /**
         * FooController::fooAction
         */
        @Nullable
        private String getController(@NotNull PhpDocTag phpDocTag) {
            Method method = AnnotationBackportUtil.getMethodScope(phpDocTag);

            if(method != null) {
                PhpClass containingClass = method.getContainingClass();
                if(containingClass != null) {
                    String fqn = containingClass.getFQN();
                    if(fqn != null) {
                        return String.format("%s::%s",
                            StringUtils.stripStart(fqn, "\\"),
                            method.getName()
                        );
                    }

                }
            }

            return null;
        }

        @Nullable
        private String getClassRoutePattern(@NotNull PhpDocTag phpDocTag) {
            PhpClass phpClass = PsiTreeUtil.getParentOfType(phpDocTag, PhpClass.class);
            if(phpClass == null) {
                return null;
            }

            PhpDocComment docComment = phpClass.getDocComment();
            for (PhpDocTag docTag : PsiTreeUtil.getChildrenOfTypeAsList(docComment, PhpDocTag.class)) {
                if(!"\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route".equals(AnnotationRoutesStubIndex.getClassNameReference(docTag, this.fileImports))) {
                    continue;
                }

                PsiElement docAttr = PsiElementUtils.getChildrenOfType(docTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
                if(!(docAttr instanceof PhpPsiElement)) {
                    continue;
                }

                PhpPsiElement firstPsiChild = ((PhpPsiElement) docAttr).getFirstPsiChild();
                if(!(firstPsiChild instanceof StringLiteralExpression)) {
                    continue;
                }

                String contents = ((StringLiteralExpression) firstPsiChild).getContents();
                if(StringUtils.isNotBlank(contents)) {
                    return contents;
                }
            }

            return null;
        }
    }

    private static class JsonDataExternalizer implements DataExternalizer<RouteInterface> {

        private static final EnumeratorStringDescriptor myStringEnumerator = new EnumeratorStringDescriptor();
        private static final Gson GSON = new Gson();

        @Override
        public void save(@NotNull DataOutput dataOutput, RouteInterface fileResource) throws IOException {
            myStringEnumerator.save(dataOutput, GSON.toJson(fileResource));
        }

        @Override
        public RouteInterface read(@NotNull DataInput in) throws IOException {
            try {
                return GSON.fromJson(myStringEnumerator.read(in), JsonRoute.class);
            } catch (JsonSyntaxException e) {
                return null;
            }
        }
    }
}



