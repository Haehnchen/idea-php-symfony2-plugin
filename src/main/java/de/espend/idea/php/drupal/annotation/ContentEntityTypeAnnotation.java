package de.espend.idea.php.drupal.annotation;

import com.intellij.psi.*;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import de.espend.idea.php.drupal.index.PermissionIndex;
import de.espend.idea.php.drupal.registrar.YamlPermissionGotoCompletion;
import de.espend.idea.php.drupal.utils.IndexUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * "@ContentEntityTypeAnnotation"
 */
public class ContentEntityTypeAnnotation implements PhpAnnotationCompletionProvider, PhpAnnotationReferenceProvider {
    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter parameter, AnnotationCompletionProviderParameter completionProviderParameter) {
        if(!isSupported(parameter)) {
            return;
        }

        if("field_ui_base_route".equalsIgnoreCase(parameter.getPropertyName())) {
            completionProviderParameter.getResult().addAllElements(RouteHelper.getRoutesLookupElements(parameter.getProject()));
        }

        if("admin_permission".equalsIgnoreCase(parameter.getPropertyName())) {
            completionProviderParameter.getResult().addAllElements(IndexUtil.getIndexedKeyLookup(parameter.getProject(), PermissionIndex.KEY));
        }
    }

    private boolean isSupported(@NotNull AnnotationPropertyParameter parameter) {
        return parameter.getType() != AnnotationPropertyParameter.Type.DEFAULT &&
            PhpLangUtil.equalsClassNames(StringUtils.stripStart(parameter.getPhpClass().getFQN(), "\\"), "Drupal\\Core\\Entity\\Annotation\\ContentEntityType");
    }

    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter parameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {
        if(!isSupported(parameter)) {
            return new PsiReference[0];
        }

        String propertyName = parameter.getPropertyName();

        if("field_ui_base_route".equalsIgnoreCase(propertyName)) {
            String contents = getContents(parameter.getElement());
            if(StringUtils.isBlank(contents)) {
                return new PsiReference[0];
            }

            return new PsiReference[] {new MyRoutePsiPolyVariantReferenceBase(parameter.getElement(), contents)};
        } else if("admin_permission".equalsIgnoreCase(propertyName)) {
            String contents = getContents(parameter.getElement());
            if(StringUtils.isBlank(contents)) {
                return new PsiReference[0];
            }

            return new PsiReference[] {new MyPermissionPsiPolyVariantReferenceBase(parameter.getElement(), contents)};
        }

        return new PsiReference[0];
    }

    @Nullable
    private String getContents(@NotNull PsiElement element) {
        if(!(element instanceof StringLiteralExpression)) {
            return null;
        }

        String contents = ((StringLiteralExpression) element).getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        return contents;
    }

    private static class MyRoutePsiPolyVariantReferenceBase extends PsiPolyVariantReferenceBase<PsiElement> {
        @NotNull
        private final PsiElement element;

        @NotNull
        private final String contents;

        MyRoutePsiPolyVariantReferenceBase(@NotNull PsiElement element, @NotNull String contents) {
            super(element);
            this.element = element;
            this.contents = contents;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {
            PsiElement routeNameTarget = RouteHelper.getRouteNameTarget(element.getProject(), contents);
            if(routeNameTarget == null) {
                return new ResolveResult[0];
            }

            return PsiElementResolveResult.createResults(routeNameTarget);
        }
    }

    public static class MyPermissionPsiPolyVariantReferenceBase extends PsiPolyVariantReferenceBase<PsiElement> {
        @NotNull
        private final PsiElement element;

        @NotNull
        private final String contents;

        MyPermissionPsiPolyVariantReferenceBase(@NotNull PsiElement element, @NotNull String contents) {
            super(element);
            this.element = element;
            this.contents = contents;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {
            return PsiElementResolveResult.createResults(YamlPermissionGotoCompletion.getPermissionPsiElements(element.getProject(), contents));
        }
    }
}
