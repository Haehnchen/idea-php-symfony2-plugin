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
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.refactoring.PhpNamespaceBraceConverter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpArrayServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Definition-side line marker for PHP DIC service definitions.
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
        LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues = null;

        for (PsiElement psiElement : psiElements) {
            PsiElement serviceIdElement = getServiceIdElement(psiElement);
            if (serviceIdElement == null) {
                continue;
            }

            PsiElement leafTarget = getLineMarkerLeafTarget(serviceIdElement);
            if (leafTarget == null) {
                continue;
            }

            String serviceId = getNormalizedServiceId(serviceIdElement);
            if (StringUtils.isBlank(serviceId)) {
                continue;
            }

            if (visitArrayServiceAttributeValue(leafTarget, serviceIdElement, serviceId, result)) {
                continue;
            }

            if (visitFluentDecoratesMethod(leafTarget, serviceIdElement, serviceId, result)) {
                continue;
            }

            if (!isServiceDefinitionId(serviceIdElement)) {
                continue;
            }

            visitServiceKeyForResources(project, leafTarget, serviceIdElement, serviceId, virtualFile, result);

            if (lazyDecoratedParentServiceValues == null) {
                lazyDecoratedParentServiceValues = new LazyDecoratedParentServiceValues(project);
            }

            visitServiceKeyForDecorates(project, leafTarget, serviceId, result, lazyDecoratedParentServiceValues);
        }
    }

    @Nullable
    private static PsiElement getServiceIdElement(@NotNull PsiElement psiElement) {
        if (psiElement instanceof StringLiteralExpression || psiElement instanceof ClassConstantReference) {
            return psiElement;
        }

        return null;
    }

    @Nullable
    private static String getNormalizedServiceId(@NotNull PsiElement psiElement) {
        return StringUtils.stripStart(ServiceContainerUtil.normalizePhpStringValue(PhpElementsUtil.getStringValue(psiElement)), "\\");
    }

    @Nullable
    private static PsiElement getLineMarkerLeafTarget(@NotNull PsiElement psiElement) {
        if (psiElement instanceof StringLiteralExpression stringLiteralExpression) {
            return PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteralExpression);
        }

        if (psiElement instanceof ClassConstantReference classConstantReference) {
            PsiElement classReference = classConstantReference.getClassReference();
            if (classReference != null) {
                return PsiTreeUtil.getDeepestFirst(classReference);
            }
        }

        return null;
    }

    /**
     * Accept only keys that belong to a PHP `services` entry like:
     * 'App\\' => ['resource' => '../src/']
     */
    private static boolean isServiceDefinitionId(@NotNull PsiElement keyElement) {
        return isPhpServiceKey(keyElement) || isFluentServiceSetId(keyElement);
    }

    private static boolean isPhpServiceKey(@NotNull PsiElement keyElement) {
        return PhpArrayServiceUtil.isServiceKey(keyElement);
    }

    /**
     * Accept service ids from fluent config chains like:
     * $services->set('app.mailer')->decorate(...)
     * $services->set(Mailer::class)->decorate(...)
     */
    private static boolean isFluentServiceSetId(@NotNull PsiElement keyElement) {
        PsiElement parent = keyElement.getParent();
        if (!(parent instanceof ParameterList) || PsiElementUtils.getParameterIndexValue(keyElement) != 0) {
            return false;
        }

        MethodReference methodReference = PsiTreeUtil.getParentOfType(keyElement, MethodReference.class);
        if (methodReference == null || methodReference.getParameterList() != parent || !"set".equals(methodReference.getName())) {
            return false;
        }

        PsiFile containingFile = methodReference.getContainingFile();
        return containingFile instanceof PhpFile phpFile && isContainerConfiguratorNamespace(phpFile);
    }

    private static void visitServiceKeyForResources(@NotNull Project project, @NotNull PsiElement leafTarget, @NotNull PsiElement keyElement, @NotNull String serviceId, @Nullable VirtualFile virtualFile, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (virtualFile == null || !serviceId.endsWith("\\")) {
            return;
        }

        ArrayHashElement serviceEntry = PsiTreeUtil.getParentOfType(keyElement, ArrayHashElement.class);
        if (serviceEntry == null || !(serviceEntry.getValue() instanceof ArrayCreationExpression)) {
            return;
        }

        ServiceSerializable service = ServiceIndexUtil.findServiceDefinition(project, virtualFile, serviceId);
        if (service == null || service.getResource().isEmpty()) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(AllIcons.Modules.SourceRoot)
            .setTargets(NotNullLazyValue.lazy(() -> ServiceIndexUtil.getClassesForServiceDefinition(project, virtualFile, service)))
            .setTooltipText("Navigate to class")
            .createLineMarkerInfo(leafTarget));
    }

    /**
     * Add reverse gutters on a PHP service key, eg:
     * Mailer::class => null
     * DecoratingMailer::class => ['decorates' => Mailer::class]
     */
    private static void visitServiceKeyForDecorates(@NotNull Project project, @NotNull PsiElement leafTarget, @NotNull String serviceId, @NotNull Collection<? super LineMarkerInfo<?>> result, @NotNull LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues) {
        NavigationGutterIconBuilder<PsiElement> decorateLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            serviceId
        );

        if (decorateLineMarker != null) {
            result.add(decorateLineMarker.createLineMarkerInfo(leafTarget));
        }

        NavigationGutterIconBuilder<PsiElement> parentLineMarker = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedParentServiceValues.getParentServices(),
            serviceId
        );

        if (parentLineMarker != null) {
            result.add(parentLineMarker.createLineMarkerInfo(leafTarget));
        }
    }

    private static boolean visitArrayServiceAttributeValue(@NotNull PsiElement leafTarget, @NotNull PsiElement valueElement, @NotNull String serviceId, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        String attributeName = getPhpServiceAttributeName(valueElement);
        if (attributeName == null) {
            return false;
        }

        if ("decorates".equals(attributeName)) {
            result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.DECORATE, serviceId));
            return true;
        }

        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, ServiceUtil.ServiceLineMarker.PARENT, serviceId));
        return true;
    }

    /**
     * Resolve "decorates" / "parent" for PHP array values like:
     * 'decorates' => Mailer::class
     */
    @Nullable
    private static String getPhpServiceAttributeName(@NotNull PsiElement valueElement) {
        String key = PhpArrayServiceUtil.getServiceAttributeName(valueElement);
        if (!"decorates".equals(key) && !"parent".equals(key)) {
            return null;
        }

        return key;
    }

    /**
     * Add a forward gutter on fluent config, eg:
     * $services->set(DecoratingMailer::class)->decorate(Mailer::class)
     */
    private static boolean visitFluentDecoratesMethod(@NotNull PsiElement leafTarget, @NotNull PsiElement parameterElement, @NotNull String serviceId, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        PsiElement parent = parameterElement.getParent();
        if (!(parent instanceof ParameterList) || PsiElementUtils.getParameterIndexValue(parameterElement) != 0) {
            return false;
        }

        MethodReference methodReference = PsiTreeUtil.getParentOfType(parameterElement, MethodReference.class);
        if (methodReference == null || methodReference.getParameterList() != parent) {
            return false;
        }

        ServiceUtil.ServiceLineMarker lineMarker = getFluentServiceLineMarker(methodReference);
        if (lineMarker == null || !isFluentServiceDecorateMethod(methodReference)) {
            return false;
        }

        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, lineMarker, serviceId));
        return true;
    }

    @Nullable
    private static ServiceUtil.ServiceLineMarker getFluentServiceLineMarker(@NotNull MethodReference methodReference) {
        String name = methodReference.getName();
        if ("decorate".equals(name)) {
            return ServiceUtil.ServiceLineMarker.DECORATE;
        }

        if ("parent".equals(name)) {
            return ServiceUtil.ServiceLineMarker.PARENT;
        }

        return null;
    }

    /**
     * Restrict "decorate(...)" to service configurator chains that originate from ->set(...).
     */
    private static boolean isFluentServiceDecorateMethod(@NotNull MethodReference methodReference) {
        PsiFile containingFile = methodReference.getContainingFile();
        if (!(containingFile instanceof PhpFile phpFile) || !isContainerConfiguratorNamespace(phpFile)) {
            return false;
        }

        PsiElement classReference = methodReference.getClassReference();
        while (classReference instanceof MethodReference chainedMethodReference) {
            if ("set".equals(chainedMethodReference.getName())) {
                return true;
            }

            classReference = chainedMethodReference.getClassReference();
        }

        return false;
    }

    private static boolean isContainerConfiguratorNamespace(@NotNull PhpFile phpFile) {
        for (PhpNamespace phpNamespace : PhpNamespaceBraceConverter.getAllNamespaces(phpFile)) {
            if ("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator".equals(phpNamespace.getFQN())) {
                return true;
            }
        }

        return false;
    }
}
