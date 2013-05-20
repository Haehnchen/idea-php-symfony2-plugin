package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IncorrectOperationException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TemplateAnnotationAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!PlatformPatterns.psiElement(PhpDocTag.class).accepts(element)) {
            return;
        }

        if (element instanceof PhpDocTag) {

            PhpDocTag phpDocTag = (PhpDocTag) element;

            String docTagName = phpDocTag.getName();
            if(docTagName == null || !docTagName.equals("@Template") || !phpDocTag.getTagValue().equals("()")) {
                return;
            }

            PhpDocComment docComment = PsiTreeUtil.getParentOfType(element, PhpDocComment.class);
            if(null == docComment) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if(null == method || !method.getName().endsWith("Action")) {
                return;
            }

            String shortcutName = TwigUtil.getControllerMethodShortcut(method);
            if(shortcutName == null) {
                return;
            }

            Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(element.getProject());
            TwigFile twigFile = twigFilesByName.get(shortcutName);

            if (null != twigFile) {
                return;
            }

            if(null != element.getFirstChild()) {
                holder.createWarningAnnotation(element.getFirstChild().getTextRange(), "Create Template")
                    .registerFix(new CreatePropertyQuickFix(method));
            }

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

                    final PsiDirectory bundlePsiDir = symfonyBundle.getDirectory();
                    if(bundlePsiDir == null) {
                        return;
                    }

                    String actionName = method.getName();
                    actionName = actionName.substring(0, actionName.lastIndexOf("Action"));

                    final String twigFileName = actionName + ".html.twig";

                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                        @Override
                        public void run() {

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
                            if(!path.startsWith("Controller")) {
                                return;
                            }

                            // gave use: Controller[/<subfolder>]/DefaultController
                            path = path.substring(10);
                            path += "/" + phpClass.getName();

                            // strip the last Controller fromt the class name
                            if(path.endsWith("Controller")) {
                                path = path.substring(0, path.length() - 10);
                            }

                            try {
                                VfsUtil.createDirectoryIfMissing(bundlePsiDir.getVirtualFile(), "Resources/views" + path);
                            } catch (IOException e) {
                                return;
                            }

                            VirtualFile twigDirectory = VfsUtil.findRelativeFile(bundlePsiDir.getVirtualFile(), ("Resources/views" + path).split("/"));
                            if(twigDirectory == null || !twigDirectory.exists()) {
                                return;
                            }

                            File f = new File(twigDirectory.getCanonicalPath() + "/" + twigFileName);
                            if(!f.exists()){
                                try {
                                    if(!f.createNewFile()) {
                                        return;
                                    }
                                } catch (IOException e) {
                                    return;
                                }
                            }

                            VirtualFile twigFile = VfsUtil.findFileByIoFile(f, true);
                            if(twigFile == null) {
                                return;
                            }

                            new OpenFileDescriptor(project, twigFile, 0).navigate(true);
                        }
                    });

                }
            });
        }
    }

}
