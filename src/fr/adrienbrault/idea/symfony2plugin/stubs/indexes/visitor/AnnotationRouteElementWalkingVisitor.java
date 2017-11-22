package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "@Route("/blog", name="blog_list")"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationRouteElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
    @NotNull
    private final Map<String, StubIndexedRoute> map;

    @Nullable
    private Map<String, String> fileImports;

    public AnnotationRouteElementWalkingVisitor(@NotNull Map<String, StubIndexedRoute> map) {
        this.map = map;
    }

    @Override
    public void visitElement(PsiElement element) {
        if ((element instanceof PhpDocTag)) {
            visitPhpDocTag((PhpDocTag) element);
        }

        super.visitElement(element);
    }

    private void visitPhpDocTag(@NotNull PhpDocTag phpDocTag) {

        // "@var" and user non related tags dont need an action
        if(AnnotationBackportUtil.NON_ANNOTATION_TAGS.contains(phpDocTag.getName())) {
            return;
        }

        // init file imports
        if(this.fileImports == null) {
            this.fileImports = AnnotationBackportUtil.getUseImportMap(phpDocTag);
        }

        if(this.fileImports.size() == 0) {
            return;
        }

        String annotationFqnName = AnnotationBackportUtil.getClassNameReference(phpDocTag, this.fileImports);
        if(annotationFqnName == null || !RouteHelper.isRouteClassAnnotation(annotationFqnName)) {
            return;
        }

        PsiElement phpDocAttributeList = PsiElementUtils.getChildrenOfType(phpDocTag, PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList));
        if(!(phpDocAttributeList instanceof PhpPsiElement)) {
            return;
        }

        String routeName = AnnotationUtil.getPropertyValue(phpDocTag, "name");
        if(routeName == null) {
            routeName = AnnotationBackportUtil.getRouteByMethod(phpDocTag);
        }

        if(routeName != null && StringUtils.isNotBlank(routeName)) {
            // prepend route name on PhpClass scope
            String routeNamePrefix = getRouteNamePrefix(phpDocTag);
            if(routeNamePrefix != null) {
                routeName = routeNamePrefix + routeName;
            }

            StubIndexedRoute route = new StubIndexedRoute(routeName);

            String path = "";

            // extract class path @Route("/foo") => "/foo" for prefixing upcoming methods
            String classPath = getClassRoutePattern(phpDocTag);
            if(classPath != null) {
                path += classPath;
            }

            // extract method path @Route("/foo") => "/foo"
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

            // @Method(...)
            extractMethods(phpDocTag, route);

            map.put(routeName, route);
        }
    }

    /**
     * Extract route name of parent class "@Route(name="foo_")"
     */
    @Nullable
    private String getRouteNamePrefix(@NotNull PhpDocTag phpDocTag) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(phpDocTag, PhpClass.class);
        if (phpClass == null) {
            return null;
        }

        PhpDocComment docComment = phpClass.getDocComment();
        if (docComment == null) {
            return null;
        }

        for (PhpDocTag docTag : PsiTreeUtil.getChildrenOfTypeAsList(docComment, PhpDocTag.class)) {
            String annotationFqnName = AnnotationBackportUtil.getClassNameReference(docTag, this.fileImports);

            // check @Route or alias
            if(annotationFqnName == null || !RouteHelper.isRouteClassAnnotation(annotationFqnName)) {
                continue;
            }

            // extract "name" property
            String annotationRouteName = AnnotationUtil.getPropertyValue(docTag, "name");
            if(StringUtils.isNotBlank(annotationRouteName)) {
                return annotationRouteName;
            }
        }

        return null;
    }

    private void extractMethods(@NotNull PhpDocTag phpDocTag, @NotNull StubIndexedRoute route) {
        PsiElement phpDoc = phpDocTag.getParent();
        if(!(phpDoc instanceof PhpDocComment)) {
            return;
        }

        PsiElement methodTag = ContainerUtil.find(phpDoc.getChildren(), psiElement ->
            psiElement instanceof PhpDocTag &&
                "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Method".equals(
                    AnnotationBackportUtil.getClassNameReference((PhpDocTag) psiElement, fileImports)
                )
        );

        if(!(methodTag instanceof PhpDocTag)) {
            return;
        }

        PhpPsiElement attrList = ((PhpDocTag) methodTag).getFirstPsiChild();
        if(attrList == null || attrList.getNode().getElementType() != PhpDocElementTypes.phpDocAttributeList) {
            return;
        }

        String content = attrList.getText();

        // ({"POST", "GET"}), ("POST")
        Matcher matcher = Pattern.compile("\"([\\w]{3,7})\"", Pattern.DOTALL).matcher(content);

        Collection<String> methods = new HashSet<>();
        while (matcher.find()) {
            methods.add(matcher.group(1).toLowerCase());
        }

        if(methods.size() > 0) {
            route.setMethods(methods);
        }
    }

    /**
     * FooController::fooAction
     */
    @Nullable
    private String getController(@NotNull PhpDocTag phpDocTag) {
        Method method = AnnotationBackportUtil.getMethodScope(phpDocTag);
        if(method == null) {
            return null;
        }

        PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return null;
        }

        return String.format("%s::%s",
            StringUtils.stripStart(containingClass.getFQN(), "\\"),
            method.getName()
        );
    }

    @Nullable
    private String getClassRoutePattern(@NotNull PhpDocTag phpDocTag) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(phpDocTag, PhpClass.class);
        if(phpClass == null) {
            return null;
        }

        PhpDocComment docComment = phpClass.getDocComment();
        for (PhpDocTag docTag : PsiTreeUtil.getChildrenOfTypeAsList(docComment, PhpDocTag.class)) {
            String classNameReference = AnnotationBackportUtil.getClassNameReference(docTag, this.fileImports);
            if(classNameReference == null) {
                continue;
            }

            if(!RouteHelper.isRouteClassAnnotation(classNameReference)) {
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
