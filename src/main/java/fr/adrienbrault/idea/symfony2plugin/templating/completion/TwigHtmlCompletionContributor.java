package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigHtmlCompletionContributor extends CompletionContributor {
    // <a><caret></a>
    // <li><caret></li>
    // <ol><caret></ol>
    // <span><caret></span>
    // <p><caret></p>
    // <button><caret></button>
    // <input value"<caret>"></input>
    // <h1...h6> value"<caret>"></h1...h6>
    private static final ElementPattern<PsiElement> TWIG_TRANSLATIONS = PlatformPatterns.or(
        TwigHtmlCompletionUtil.getTagAttributePattern("input", "value"),
        TwigHtmlCompletionUtil.getTagAttributePattern("input", "placeholder"),
        TwigHtmlCompletionUtil.getTagTextPattern("a", "li", "ol", "span", "p", "button", "h1", "h2", "h3", "h4", "h5", "h6")
    );

    public TwigHtmlCompletionContributor() {
        // add completion for href and provide twig insert handler
        // <a href="<caret>"/> => <a href="{{ path('foobar', {'foo' : 'bar'}) }}"/>
        // <form action="<caret>"/>
        extend(
            CompletionType.BASIC,
            PlatformPatterns.or(
                TwigHtmlCompletionUtil.getHrefAttributePattern(),
                TwigHtmlCompletionUtil.getFormActionAttributePattern()
            ),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    List<LookupElement> routesLookupElements = RouteHelper.getRoutesLookupElements(parameters.getPosition().getProject());
                    for (LookupElement element : routesLookupElements) {
                        if (element instanceof RouteLookupElement) {
                            ((RouteLookupElement) element).withInsertHandler(TwigPathFunctionInsertHandler.getInstance());
                        }
                    }

                    resultSet.addAllElements(routesLookupElements);
                }
            });

        // <link href="<caret>" rel="stylesheet" />
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetCssAttributePattern(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader(TwigUtil.CSS_FILES_EXTENSIONS, false).getAssetFiles(parameters.getPosition().getProject())) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <script src="<caret>"></script>
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetJsAttributePattern(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader(TwigUtil.JS_FILES_EXTENSIONS, false).getAssetFiles(parameters.getPosition().getProject())) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <img src="<caret>">
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetImageAttributePattern(), new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader(TwigUtil.IMG_FILES_EXTENSIONS, false).getAssetFiles(parameters.getPosition().getProject())) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <a><caret></a>
        // <input value"<caret>"></input>
        // <h1...h6> value"<caret>"></h1...h6>
        extend(CompletionType.BASIC, TWIG_TRANSLATIONS, new CompletionProvider<>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                PsiElement position = parameters.getOriginalPosition();
                if (position == null || !Symfony2ProjectComponent.isEnabled(position)) {
                    return;
                }

                PsiElement element = TwigUtil.getElementOnTwigViewProvider(position);
                TwigUtil.DomainScope domainScope = TwigUtil.getTwigFileDomainScope(element != null ? element : position);

                String finalDomain = domainScope.getDomain();
                List<LookupElement> collect = TranslationUtil.getTranslationLookupElementsOnDomain(position.getProject(), domainScope.getDomain())
                    .stream()
                    .map((Function<LookupElement, LookupElement>) lookupElement ->
                        new TranslatorLookupElement(lookupElement.getLookupString(), finalDomain, TwigTranslationFilterInsertHandler.getInstance())
                    ).collect(Collectors.toList());

                resultSet.addAllElements(collect);
            }
        });

        // <twig:Alert></twig:Alert>
        extend(
            CompletionType.BASIC,
            TwigHtmlCompletionUtil.getTwigNamespacePattern(),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                    PsiElement position = parameters.getOriginalPosition();
                    if (!Symfony2ProjectComponent.isEnabled(position)) {
                        return;
                    }

                    resultSet.addAllElements(UxUtil.getComponentLookupElements(position.getProject()));
                }
            }
        );

        // "<twig:Alert m<caret> :<caret>"
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(XmlAttribute.class).withParent(PlatformPatterns.psiElement(XmlTag.class).with(new PatternCondition<>("starting with 'twig:'") {
                @Override
                public boolean accepts(@NotNull XmlTag xmlTag, ProcessingContext context) {
                    return xmlTag.getName().startsWith("twig:");
                }
            }))),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                    PsiElement position = parameters.getOriginalPosition();
                    if (!Symfony2ProjectComponent.isEnabled(position)) {
                        return;
                    }

                    XmlTag parentOfType = PsiTreeUtil.getParentOfType(position, XmlTag.class);
                    if (parentOfType == null) {
                        return;
                    }

                    for (PhpClass phpClass : UxUtil.getTwigComponentNameTargets(position.getProject(), parentOfType.getName().substring(5))) {
                        UxUtil.visitComponentVariables(phpClass, pair -> {
                            PhpNamedElement field = pair.getSecond();

                            LookupElementBuilder element = LookupElementBuilder
                                .create(field.getName())
                                .withIcon(Symfony2Icons.SYMFONY)
                                .withTypeText(field.getType().toString(), true);

                            resultSet.addElement(element);

                            LookupElementBuilder element2 = LookupElementBuilder
                                .create(":" + field.getName())
                                .withIcon(Symfony2Icons.SYMFONY)
                                .withTypeText(field.getType().toString(), true);

                            resultSet.addElement(element2);
                        });
                    }
                }
            }
        );
    }
}
