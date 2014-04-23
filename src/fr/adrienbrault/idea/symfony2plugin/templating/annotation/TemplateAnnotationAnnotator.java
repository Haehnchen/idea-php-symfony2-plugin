package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateAnnotator;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackPortUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateAnnotationAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject()) || !Settings.getInstance(element.getProject()).phpAnnotateTemplateAnnotation) {
            return;
        }

        if(!(element instanceof PhpDocTag)) {
            return;
        }

        PhpDocTag phpDocTag = (PhpDocTag) element;
        String docTagName = phpDocTag.getName();

        if(AnnotationBackPortUtil.NON_ANNOTATION_TAGS.contains(docTagName)) {
            return;
        }

        PhpClass phpClass = AnnotationBackPortUtil.getAnnotationReference(phpDocTag);
        if(phpClass == null) {
            return;
        }

        if(!PhpElementsUtil.isEqualClassName(phpClass, TwigHelper.TEMPLATE_ANNOTATION_CLASS)) {
            return;
        }

        PhpPsiElement phpDocAttrList = phpDocTag.getFirstPsiChild();
        if(phpDocAttrList == null) {
            return;
        }

        String tagValue = phpDocAttrList.getText();
        String templateName;

        // @Template("FooBundle:Folder:foo.html.twig")
        // @Template("FooBundle:Folder:foo.html.twig", "asdas")
        // @Template(tag="name")
        Matcher matcher = Pattern.compile("\\(\"(.*)\"").matcher(tagValue);
        if (matcher.find()) {
            templateName = matcher.group(1);
        } else {

            // find template name on last method
            PhpDocComment docComment = PsiTreeUtil.getParentOfType(element, PhpDocComment.class);
            if(null == docComment) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if(null == method || !method.getName().endsWith("Action")) {
                return;
            }

            templateName = TwigUtil.getControllerMethodShortcut(method);
        }

        if(templateName == null) {
            return;
        }

        if ( TwigHelper.getTemplatePsiElements(element.getProject(), templateName).length > 0) {
            return;
        }

        if(null != element.getFirstChild()) {
            holder.createWarningAnnotation(element.getFirstChild().getTextRange(), "Create Template")
                .registerFix(new PhpTemplateAnnotator.CreateTemplateFix(templateName));
        }


    }

    class CreatePropertyQuickFix extends BaseIntentionAction {
        private String key;

        private Method method;
        public CreatePropertyQuickFix(Method method) {
            this.method = method;
        }

        @NotNull
        @Override
        public String getText() {
            return "Create Template";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "Symfony2";
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
            return true;
        }

        @Override
        public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {

                    SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(PhpIndex.getInstance(project));

                    final SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(file);
                    if(null == symfonyBundle) {
                        return;
                    }

                    VirtualFile bundleDirectory = symfonyBundle.getVirtualDirectory();
                    if(bundleDirectory == null) {
                        return;
                    }

                    String actionName = method.getName();
                    actionName = actionName.substring(0, actionName.lastIndexOf("Action"));
                    String twigFileName = actionName + ".html.twig";

                    VirtualFile controllerFile = method.getContainingFile().getVirtualFile();
                    if(controllerFile == null) {
                        return;
                    }

                    // the directory of the controller file: can be in sub folder or bundle controller root dir
                    VirtualFile bundleController = controllerFile.getParent();

                    // get controller name from class name
                    PhpClass phpClass = method.getContainingClass();
                    if(phpClass == null) {
                        return;
                    }

                    // all controllers need to be in <bundle>/Controller
                    String path = symfonyBundle.getRelative(bundleController);
                    if(path == null || !path.startsWith("Controller")) {
                        return;
                    }

                    // gave use: Controller[/<subfolder>]/DefaultController
                    path = path.substring(10);
                    path += "/" + phpClass.getName();

                    // strip the last Controller from the class name
                    if(path.endsWith("Controller")) {
                        path = path.substring(0, path.length() - 10);
                    }

                    ApplicationManager.getApplication().runWriteAction(IdeHelper.getRunnableCreateAndOpenFile(project, bundleDirectory, "Resources/views" + path + "/" + twigFileName));

                }
            });
        }
    }

}
