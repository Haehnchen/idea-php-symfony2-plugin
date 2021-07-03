package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlText;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class XmlLanguageInjector implements MultiHostInjector {

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!Symfony2ProjectComponent.isEnabled(context.getProject())) {
            return;
        }

        if (isExpressionLanguageString(context)) {
            registrar
                .startInjecting(ExpressionLanguage.INSTANCE)
                .addPlace(null, null, (PsiLanguageInjectionHost) context, ElementManipulators.getValueTextRange(context))
                .doneInjecting();
        }
    }

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(XmlText.class);
    }

    private boolean isExpressionLanguageString(@NotNull PsiElement element) {
        return PlatformPatterns.or(
            getExpressionArgumentPattern(),
            getRouteConditionPattern()
        ).accepts(element);
    }

    /**
     * <argument type="expression">container.get('service_id')</argument>
     */
    private XmlElementPattern.XmlTextPattern getExpressionArgumentPattern() {
        return XmlPatterns
            .xmlText()
            .withParent(XmlPatterns
                .xmlTag()
                .withName("argument")
                .withAttributeValue("type", "expression")
            )
            .inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <routes ...>
     *     <route ...>
     *        <condition>context.getMethod() in ['GET', 'HEAD']</condition>
     *     </route>
     * </routes>
     */
    private XmlElementPattern.XmlTextPattern getRouteConditionPattern() {
        return XmlPatterns
            .xmlText()
            .withParent(XmlPatterns
                .xmlTag()
                .withName("condition")
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("route")
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withName("routes")
                    )
                )
            )
            .inFile(XmlHelper.getXmlFilePattern());
    }
}
