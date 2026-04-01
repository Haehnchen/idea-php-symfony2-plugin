package fr.adrienbrault.idea.symfony2plugin.dic.linemarker;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * Shared helpers for service line markers across XML and YAML providers.
 */
final class ServiceLineMarkerUtil {
    private ServiceLineMarkerUtil() {
    }

    /**
     * Collect forward line markers for direct service references:
     * <service decorates="foobar" />, <service parent="foobar" />,
     * decorates: foobar, parent: foobar
     */
    public static void collectDecoratesAndParentTargets(
        @NotNull PsiElement leafTarget,
        @NotNull Collection<? super LineMarkerInfo<?>> result,
        @Nullable String decorates,
        @Nullable String parent
    ) {
        // <service decorates="foobar" /> / decorates: foobar
        if (StringUtils.isNotBlank(decorates)) {
            addDirectLineMarker(leafTarget, result, ServiceUtil.ServiceLineMarker.DECORATE, decorates);
        }

        // <service parent="foobar" /> / parent: foobar
        if (StringUtils.isNotBlank(parent)) {
            addDirectLineMarker(leafTarget, result, ServiceUtil.ServiceLineMarker.PARENT, parent);
        }
    }

    /**
     * Collect reverse line markers for services that reference the current service
     * via "decorates" or "parent".
     */
    public static void collectDecoratedAndParentTargets(
        @NotNull Project project,
        @NotNull PsiElement leafTarget,
        @NotNull Collection<? super LineMarkerInfo<?>> result,
        @NotNull LazyDecoratedParentServiceValues lazyDecoratedParentServiceValues,
        @NotNull String serviceId
    ) {
        // foreign "decorates" linemarker
        addRelatedLineMarker(
            project,
            leafTarget,
            result,
            ServiceUtil.ServiceLineMarker.DECORATE,
            lazyDecoratedParentServiceValues.getDecoratedServices(),
            serviceId
        );

        // foreign "parent" linemarker
        addRelatedLineMarker(
            project,
            leafTarget,
            result,
            ServiceUtil.ServiceLineMarker.PARENT,
            lazyDecoratedParentServiceValues.getParentServices(),
            serviceId
        );
    }

    private static void addDirectLineMarker(
        @NotNull PsiElement leafTarget,
        @NotNull Collection<? super LineMarkerInfo<?>> result,
        @NotNull ServiceUtil.ServiceLineMarker lineMarker,
        @NotNull String serviceId
    ) {
        result.add(ServiceUtil.getLineMarkerForDecoratesServiceId(leafTarget, lineMarker, serviceId));
    }

    private static void addRelatedLineMarker(
        @NotNull Project project,
        @NotNull PsiElement leafTarget,
        @NotNull Collection<? super LineMarkerInfo<?>> result,
        @NotNull ServiceUtil.ServiceLineMarker lineMarker,
        @NotNull Map<String, Collection<ContainerService>> serviceIds,
        @NotNull String serviceId
    ) {
        NavigationGutterIconBuilder<PsiElement> lineMarkerBuilder = ServiceUtil.getLineMarkerForDecoratedServiceId(
            project,
            lineMarker,
            serviceIds,
            serviceId
        );

        if (lineMarkerBuilder != null) {
            result.add(lineMarkerBuilder.createLineMarkerInfo(leafTarget));
        }
    }
}
