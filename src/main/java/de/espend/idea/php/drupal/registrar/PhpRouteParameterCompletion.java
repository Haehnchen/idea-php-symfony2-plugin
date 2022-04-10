package de.espend.idea.php.drupal.registrar;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.drupal.DrupalProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteParameterCompletion implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(getElementPatternPattern(), psiElement -> {
            if(!DrupalProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new MyGotoCompletionProvider(psiElement);
        });
    }

    private PsiElementPattern.Capture<PsiElement> getElementPatternPattern() {
        return PlatformPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(
                    PlatformPatterns.or(PlatformPatterns.psiElement(PhpElementTypes.ARRAY_KEY), PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE))
                )
            )
        ;
    }

    private static class MyGotoCompletionProvider extends GotoCompletionProvider {
        MyGotoCompletionProvider(PsiElement psiElement) {
            super(psiElement);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            String routeName = getRouteName(getElement());
            if(StringUtils.isBlank(routeName)) {
                return Collections.emptyList();
            }

            return Arrays.asList(RouteHelper.getRouteParameterLookupElements(getElement().getProject(), routeName));
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            String routeName = getRouteName(psiElement);
            if(StringUtils.isBlank(routeName)) {
                return Collections.emptyList();
            }

            PsiElement parent = psiElement.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            return Arrays.asList(RouteHelper.getRouteParameterPsiElements(
                getElement().getProject(), routeName, ((StringLiteralExpression) parent).getContents()
            ));
        }

        /**
         * new Foo('route_name', ['foo' => ''])
         */
        @Nullable
        private String getRouteName(@NotNull PsiElement psiElement) {
            PsiElement parent = psiElement.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return null;
            }

            ArrayCreationExpression arrayCreationExpression = findArrayCreationExpression((StringLiteralExpression) parent);
            if(arrayCreationExpression == null) {
                return null;
            }

            PsiElement parameterList = arrayCreationExpression.getParent();
            if(!(parameterList instanceof ParameterList)) {
                return null;
            }

            PsiElement newExpression = parameterList.getParent();
            if(!(newExpression instanceof NewExpression)) {
                return null;
            }

            ClassReference classReference = ((NewExpression) newExpression).getClassReference();
            if(classReference == null) {
                return null;
            }

            if(!"Drupal\\Core\\Url".equalsIgnoreCase(StringUtils.stripStart(classReference.getFQN(), "\\"))) {
                return null;
            }

            PsiElement[] parameters = ((NewExpression) newExpression).getParameters();
            if(parameters.length == 0) {
                return null;
            }

            String stringValue = PhpElementsUtil.getStringValue(parameters[0]);
            if(StringUtils.isBlank(stringValue)) {
                return null;
            }

            return stringValue;
        }
    }

    @Nullable
    private static ArrayCreationExpression findArrayCreationExpression(@NotNull StringLiteralExpression psiElement) {

        // value inside array
        // $menu->addChild(array(
        //   'foo' => '',
        // ));
        PsiElement arrayKey = psiElement.getContext();
        if(arrayKey == null) {
            return null;
        }

        if(arrayKey.getNode().getElementType() == PhpElementTypes.ARRAY_KEY) {
            PsiElement arrayHashElement = arrayKey.getContext();
            if(arrayHashElement instanceof ArrayHashElement) {
                PsiElement arrayCreationExpression = arrayHashElement.getContext();
                if(arrayCreationExpression instanceof ArrayCreationExpression) {
                    PsiElement parameterList = arrayCreationExpression.getParent();
                    if(parameterList instanceof ParameterList) {
                        return (ArrayCreationExpression) arrayCreationExpression;
                    }
                }
            }
        } else if(arrayKey.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
            // on array creation key dont have value, so provide completion here also
            // array('foo' => 'bar', '<test>')

            PsiElement arrayCreationExpression = arrayKey.getContext();
            if(arrayCreationExpression instanceof ArrayCreationExpression) {
                PsiElement parameterList = arrayCreationExpression.getParent();
                if(parameterList instanceof ParameterList) {
                    return (ArrayCreationExpression) arrayCreationExpression;
                }
            }
        }

        return null;
    }

}
