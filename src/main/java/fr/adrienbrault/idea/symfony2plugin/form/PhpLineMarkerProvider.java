package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        for (PsiElement psiElement : psiElements) {
            if (PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
                attachFormDataClass(lineMarkerInfos, psiElement);
                attachPhpClassToFormDataClass(project, lineMarkerInfos, psiElement);
            }
        }
    }

    private void attachPhpClassToFormDataClass(@NotNull Project project, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        String fqn = ((PhpClass) phpClassContext).getFQN();

        Set<String> classes = FileBasedIndex.getInstance().getValues(FormDataClassStubIndex.KEY, "\\" + StringUtils.stripStart(fqn, "\\"), GlobalSearchScope.allScope(project))
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String clazz: classes) {
            phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, clazz));
        }

        if (!phpClasses.isEmpty()) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER)
                .setTargets(phpClasses)
                .setTooltipText("Navigate to form");

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }

    private void attachFormDataClass(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if (phpClassContext == null) {
            return;
        }

        boolean isFormTypeInstance = PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Form\\FormTypeInterface")
            || PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Form\\FormExtensionInterface");

        if (!isFormTypeInstance) {
            return;
        }

        PhpClass formDataClass = FormOptionsUtil.getFormPhpClassFromContext(leaf);
        if (formDataClass != null) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTargets(formDataClass)
                .setTooltipText("Navigate to data class");

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }
}
