package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Definition-side line marker for PHP array service resource prototypes.
 *
 * It mirrors the YAML/XML "Navigate to class" gutter on namespace entries like:
 * 'App\\' => ['resource' => '../src/']
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.getFirst().getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PsiElement firstElement = psiElements.getFirst();
        PsiFile containingFile = firstElement instanceof PsiFile ? (PsiFile) firstElement : firstElement.getContainingFile();
        if (!(containingFile instanceof PhpFile phpFile)) {
            return;
        }

        VirtualFile virtualFile = phpFile.getVirtualFile();
        if (virtualFile == null) {
            return;
        }

        for (PsiElement psiElement : psiElements) {
            if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                continue;
            }

            if (psiElement != PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteralExpression)) {
                continue;
            }

            String serviceId = StringUtils.stripStart(ServiceContainerUtil.normalizePhpStringValue(stringLiteralExpression.getContents()), "\\");
            if (StringUtils.isBlank(serviceId) || !serviceId.endsWith("\\")) {
                continue;
            }

            if (!isPhpServiceKey(stringLiteralExpression)) {
                continue;
            }

            ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, virtualFile, serviceId);
            if (service == null || service.getResource().isEmpty()) {
                continue;
            }

            result.add(NavigationGutterIconBuilder.create(AllIcons.Modules.SourceRoot)
                .setTargets(NotNullLazyValue.lazy(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, virtualFile, service)))
                .setTooltipText("Navigate to class")
                .createLineMarkerInfo(psiElement));
        }
    }

    /**
     * Accept only string keys that belong to a PHP `services` entry like:
     * 'App\\' => ['resource' => '../src/']
     */
    private static boolean isPhpServiceKey(@NotNull StringLiteralExpression stringLiteralExpression) {
        ArrayHashElement serviceEntry = PsiTreeUtil.getParentOfType(stringLiteralExpression, ArrayHashElement.class);
        if (serviceEntry == null || serviceEntry.getKey() != stringLiteralExpression) {
            return false;
        }

        if (!(serviceEntry.getValue() instanceof ArrayCreationExpression)) {
            return false;
        }

        ArrayCreationExpression servicesArray = PsiTreeUtil.getParentOfType(serviceEntry, ArrayCreationExpression.class);
        if (servicesArray == null) {
            return false;
        }

        ArrayHashElement servicesEntry = PsiTreeUtil.getParentOfType(servicesArray, ArrayHashElement.class);
        return servicesEntry != null && "services".equals(PhpElementsUtil.getStringValue(servicesEntry.getKey()));
    }
}
