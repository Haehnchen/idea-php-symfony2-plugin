package fr.adrienbrault.idea.symfony2plugin.templating.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
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
            new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                List<LookupElement> routesLookupElements = RouteHelper.getRoutesLookupElements(parameters.getPosition().getProject());
                for(LookupElement element: routesLookupElements) {
                    if(element instanceof RouteLookupElement) {
                        ((RouteLookupElement) element).withInsertHandler(TwigPathFunctionInsertHandler.getInstance());
                    }
                }

                resultSet.addAllElements(routesLookupElements);
            }
        });

        // <link href="<caret>" rel="stylesheet" />
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetCssAttributePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader().setProject(parameters.getPosition().getProject()).setFilterExtension(TwigHelper.CSS_FILES_EXTENSIONS).setIncludeBundleDir(false).getAssetFiles()) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <script src="<caret>"></script>
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetJsAttributePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader().setProject(parameters.getPosition().getProject()).setFilterExtension(TwigHelper.JS_FILES_EXTENSIONS).setIncludeBundleDir(false).getAssetFiles()) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <img src="<caret>">
        extend(CompletionType.BASIC, TwigHtmlCompletionUtil.getAssetImageAttributePattern(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                    return;
                }

                for (AssetFile assetFile : new AssetDirectoryReader().setProject(parameters.getPosition().getProject()).setFilterExtension(TwigHelper.IMG_FILES_EXTENSIONS).setIncludeBundleDir(false).getAssetFiles()) {
                    resultSet.addElement(new AssetLookupElement(assetFile, parameters.getPosition().getProject()).withInsertHandler(TwigAssetFunctionInsertHandler.getInstance()));
                }
            }
        });

        // <a><caret></a>
        // <input value"<caret>"></input>
        // <h1...h6> value"<caret>"></h1...h6>
        extend(CompletionType.BASIC, TWIG_TRANSLATIONS, new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                PsiElement position = parameters.getOriginalPosition();
                if(position == null || !Symfony2ProjectComponent.isEnabled(position)) {
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
    }
}
