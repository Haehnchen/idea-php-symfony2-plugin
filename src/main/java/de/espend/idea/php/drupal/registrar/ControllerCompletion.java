package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import de.espend.idea.php.drupal.utils.DrupalUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(YamlElementPatternHelper.getSingleLineScalarKey("_controller"), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new MyGotoCompletionProvider(psiElement);
        });

    }

    private static class MyGotoCompletionProvider extends GotoCompletionProvider {
        MyGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<>();

            Set<String> moduleNames = DrupalUtil.getModuleNames(getProject());

            Set<PhpClass> phpClasses = new HashSet<>(
                PhpIndex.getInstance(getProject()).getAllSubclasses("Drupal\\Core\\Controller\\ControllerBase")
            );

            for (String moduleName : moduleNames) {
                phpClasses.addAll(
                    PhpIndexUtil.getPhpClassInsideNamespace(getProject(), "\\Drupal\\" + moduleName + "\\Controller")
                );
            }

            for (PhpClass phpClass : phpClasses) {
                for (Method method : phpClass.getOwnMethods()) {
                    if(!method.getAccess().isPublic() || method.isAbstract() || method.getName().startsWith("__")) {
                        continue;
                    }

                    lookupElements.add(LookupElementBuilder.create(phpClass.getFQN() + "::" + method.getName()).withIcon(method.getIcon()));
                }
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            return Collections.emptyList();
        }
    }
}
