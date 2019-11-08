package fr.adrienbrault.idea.symfonyplugin.completion.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfonyplugin.completion.DecoratedServiceCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.routing.RouteGotoCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.templating.TemplateGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfonyplugin.util.completion.PhpConstGotoCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
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

        // key: !php/const <caret>
        registrar.register(
            YamlElementPatternHelper.getPhpConstPattern(),
            PhpConstGotoCompletionProvider::new
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
