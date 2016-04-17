package fr.adrienbrault.idea.symfony2plugin.dic.container.instance;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceSuggestionCollector {
    @NotNull
    Collection<String> collect(@NotNull PsiElement psiElement, @NotNull Collection<ContainerService> serviceMap);
}
