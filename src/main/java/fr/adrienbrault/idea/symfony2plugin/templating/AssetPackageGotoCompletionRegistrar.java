package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * PHP version of "Twig" "asset" function
 *
 * "Package*::getUrl"
 * "Package*::getVersion"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetPackageGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern()), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterRecursiveMatcher(context, 0)
                    .withSignature("\\Symfony\\Component\\Asset\\Packages", "getUrl")
                    .withSignature("\\Symfony\\Component\\Asset\\Packages", "getVersion")
                    .withSignature("\\Symfony\\Component\\Asset\\PackageInterface", "getUrl")
                    .withSignature("\\Symfony\\Component\\Asset\\PackageInterface", "getVersion")
                    .match();

                if (methodMatchParameter == null) {
                    return null;
                }

                return new AssetContributor((StringLiteralExpression) context);
            }
        );
    }

    private static class AssetContributor extends GotoCompletionProvider {

        public AssetContributor(StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Project project = this.getProject();
            if(!Symfony2ProjectComponent.isEnabled(project)) {
                return Collections.emptyList();
            }

            Collection<LookupElement> items = new ArrayList<>();
            for (AssetFile assetFile : new AssetDirectoryReader().getAssetFiles(project)) {
                items.add(new AssetLookupElement(assetFile, project));
            }

            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            for (String s : twigPathServiceParser.getNamedAssets().keySet()) {
                items.add(LookupElementBuilder.create("@" + s).withIcon(PlatformIcons.FOLDER_ICON).withTypeText("Custom Assets", true));
            }

            return items;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getTextValueForElement(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> psiElements = new HashSet<>();

            for (VirtualFile virtualFile : TwigUtil.resolveAssetsFiles(element.getProject() ,contents, (String[]) ArrayUtils.addAll(TwigUtil.CSS_FILES_EXTENSIONS, TwigUtil.JS_FILES_EXTENSIONS))) {
                PsiElement target;
                if(virtualFile.isDirectory()) {
                    target = PsiManager.getInstance(element.getProject()).findDirectory(virtualFile);
                } else {
                    target = PsiManager.getInstance(element.getProject()).findFile(virtualFile);
                }

                if(target != null) {
                    psiElements.add(target);
                }
            }

            return psiElements;
        }
    }
}
