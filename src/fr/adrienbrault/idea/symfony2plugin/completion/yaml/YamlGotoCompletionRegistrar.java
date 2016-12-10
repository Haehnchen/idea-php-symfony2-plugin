package fr.adrienbrault.idea.symfony2plugin.completion.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.completion.DecoratedServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

public class YamlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // defaults:
        //   route: <caret>
        registrar.register(
            YamlElementPatternHelper.getSingleLineScalarKey("route"),
            RouteGotoCompletionProvider::new
        );

        // defaults:
        //   template: <caret>
        registrar.register(
            YamlElementPatternHelper.getSingleLineScalarKey("template"),
            TemplateGotoCompletionRegistrar::new
        );

        // foo.service:
        //   decorates: <caret>
        registrar.register(
            YamlElementPatternHelper.getSingleLineScalarKey("decorates"),
            MyDecoratedServiceCompletionProvider::new
        );
    }

    private static class MyDecoratedServiceCompletionProvider extends DecoratedServiceCompletionProvider {
        MyDecoratedServiceCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @Nullable
        @Override
        public String findClassForElement(@NotNull PsiElement psiElement) {
            YAMLMapping parentOfType = PsiTreeUtil.getParentOfType(psiElement, YAMLMapping.class);
            if(parentOfType == null) {
                return null;
            }

            return YamlHelper.getYamlKeyValueAsString(parentOfType, "class");
        }

        @Nullable
        @Override
        public String findIdForElement(@NotNull PsiElement psiElement) {
            YAMLMapping parentOfType = PsiTreeUtil.getParentOfType(psiElement, YAMLMapping.class);
            if(parentOfType == null) {
                return null;
            }

            return YamlHelper.getYamlKeyValueAsString(parentOfType, "id");
        }
    }
}
