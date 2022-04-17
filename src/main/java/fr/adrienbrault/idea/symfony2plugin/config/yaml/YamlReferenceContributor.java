package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLScalar;

public class YamlReferenceContributor extends PsiReferenceContributor {
    private static final String TAG_PHP_CONST = "!php/const";

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar.class)
                .withText(StandardPatterns.string()
                    .contains(TAG_PHP_CONST)
                ),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                    if (!Symfony2ProjectComponent.isEnabled(element)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    var scalar = (YAMLScalar)element;
                    if (scalar.getTextValue().isEmpty()) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    return new PsiReference[]{
                        new ConstantYamlReference(scalar)
                    };
                }
            }
        );
    }
}
