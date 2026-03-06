package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * {% javascripts
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * {% stylesheets
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigAssetsTagMissingInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        @NotNull
        private final ProblemsHolder holder;

        private ElementPattern<?> stylesheetsPattern;
        private ElementPattern<?> javascriptsPattern;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(getStylesheetsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {

                String templateName = element.getText();
                if (!isKnownAssetFileOrFolder(element, templateName, TwigUtil.CSS_FILES_EXTENSIONS)) {
                    holder.registerProblem(element, "Missing asset");
                }
            } else if (getJavascriptsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                String templateName = element.getText();
                if (!isKnownAssetFileOrFolder(element, templateName, TwigUtil.JS_FILES_EXTENSIONS)) {
                    holder.registerProblem(element, "Missing asset");
                }
            }

            super.visitElement(element);
        }

        private ElementPattern<?> getStylesheetsPattern() {
            return stylesheetsPattern != null ? stylesheetsPattern : (stylesheetsPattern = TwigPattern.getAutocompletableAssetTag("stylesheets"));
        }

        private ElementPattern<?> getJavascriptsPattern() {
            return javascriptsPattern != null ? javascriptsPattern : (javascriptsPattern = TwigPattern.getAutocompletableAssetTag("javascripts"));
        }
    }

    private static boolean isKnownAssetFileOrFolder(PsiElement element, String templateName, String... fileTypes) {
        // custom assets
        if(templateName.startsWith("@") && templateName.length() > 1) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(element.getProject(), TwigNamedAssetsServiceParser.class);
            Set<String> strings = twigPathServiceParser.getNamedAssets().keySet();
            if(strings.contains(templateName.substring(1))) {
                return true;
            }
        }

        return !TwigUtil.resolveAssetsFiles(element.getProject(), templateName, fileTypes).isEmpty();
    }
}
