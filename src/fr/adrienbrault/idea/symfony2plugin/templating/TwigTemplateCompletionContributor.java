package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.provider.AssetCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.FunctionInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import icons.PhpIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {

        extend(CompletionType.BASIC, TwigHelper.getTemplateFileReferenceTagPattern(),  new TemplateCompletionProvider());
        extend(CompletionType.BASIC, TwigHelper.getPrintBlockFunctionPattern("include"),  new TemplateCompletionProvider());

        // provides support for 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain')
        // provides support for 'a<xxx>'|transchoice(2, {'%foo%' : bar|default}, 'Domain')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTranslationPattern("trans", "transchoice"),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    TranslationStringMap map = TranslationIndex.getInstance(parameters.getPosition().getProject()).getTranslationMap();
                    if(map == null) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition();
                    String domainName =  TwigUtil.getPsiElementTranslationDomain(psiElement);

                    ArrayList<String> domainMap = map.getDomainMap(domainName);
                    if(domainMap == null) {
                        return;
                    }

                    for(String stringId : domainMap) {
                        resultSet.addElement(
                            new TranslatorLookupElement(stringId, domainName)
                        );
                    }

                }
            }

        );

        // provides support for 'a'|trans({'%foo%' : bar|default}, '<xxx>')
        // provides support for 'a'|transchoice(2, {'%foo%' : bar|default}, '<xxx>')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTransDomainPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    if(PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice"))) == null) {
                        return;
                    }

                    TranslationStringMap map = TranslationIndex.getInstance(parameters.getPosition().getProject()).getTranslationMap();
                    if(map != null) {
                        for(String domainKey : map.getDomainList()) {
                            resultSet.addElement(new TranslatorLookupElement(domainKey, domainKey));
                        }
                    }

                }


            }

        );

        // provides support for {% block |
        extend(
            CompletionType.BASIC,
            TwigHelper.getBlockTagPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    ArrayList<TwigBlock> blocks = new TwigBlockParser(twigFilesByName).walk(parameters.getPosition().getContainingFile());
                    ArrayList<String> uniqueList = new ArrayList<String>();
                    for (TwigBlock block : blocks) {
                        if(!uniqueList.contains(block.getName())) {
                            uniqueList.add(block.getName());
                            resultSet.addElement(new TwigBlockLookupElement(block));
                        }
                    }

                }
            }
        );

        // provides support for {% from 'twig..' import |
        extend(
            CompletionType.BASIC,
            TwigHelper.getTemplateImportFileReferenceTagPattern(),

            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    // find {% from "<template.name>"
                    PsiElement psiElement = PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), getFromTemplateElement());

                    if(psiElement == null) {
                        return;
                    }

                    String templateName = psiElement.getText();

                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    if(!twigFilesByName.containsKey(templateName)) {
                        return;
                    }

                    for (Map.Entry<String, String> entry: new TwigMarcoParser().getMacros(twigFilesByName.get(templateName)).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue(), true).withIcon(PhpIcons.TwigFileIcon));
                    }

                }

                private PsiElementPattern.Capture<PsiElement> getFromTemplateElement() {
                    return PlatformPatterns
                        .psiElement(TwigTokenTypes.STRING_TEXT)
                        .afterLeafSkipping(
                            PlatformPatterns.or(
                                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                                PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                                PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                                PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                            ),
                            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withText(PlatformPatterns.string().oneOf("from"))
                        )
                        .withLanguage(TwigLanguage.INSTANCE);
                }
            }
        );

        // provides support for 'a'|<xxx> but currently blocked on phpstorm see WI-19022
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement().withLanguage(TwigLanguage.INSTANCE)),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    // move this stuff to pattern fixed event stopping by phpstorm
                    PsiElement currElement = parameters.getPosition().getOriginalElement();
                    PsiElement prevElement = currElement.getPrevSibling();
                    if ((prevElement != null) && ((prevElement instanceof PsiWhiteSpace))) prevElement = prevElement.getPrevSibling();

                    if ((prevElement != null) && (prevElement.getNode().getElementType() == TwigTokenTypes.FILTER)) {
                        for(Map.Entry<String, String> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFilters().entrySet()) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue()));
                        }
                    }

                }

            }

        );

        // provides support for {{ '<xxx>' }}
        extend(
            CompletionType.BASIC,
            TwigHelper.getPrintBlockFunctionPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition().getOriginalElement();
                    PsiElement prevElement = psiElement.getPrevSibling();
                    if ((prevElement == null) || !(prevElement instanceof PsiWhiteSpace)) {
                        return;
                    }

                    for(Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFunctions().entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withIcon(TwigExtensionParser.getIcon(entry.getValue().getTwigExtensionType())).withTypeText(entry.getValue().getType()).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigMacro twigMacro: TwigUtil.getImportedMacros(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate()).withIcon(PhpIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigMacro twigMacro: TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate()).withIcon(PhpIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigSet twigSet: TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigSet.getName()).withTypeText("set"));
                    }

                }
            }

        );

        // {% trans_default_domain <> %}
        // {% trans_default_domain '<>' %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTransDefaultDomain(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    TranslationStringMap map = TranslationIndex.getInstance(parameters.getPosition().getProject()).getTranslationMap();
                    for(String domainKey : map.getDomainList()) {
                        resultSet.addElement(new TranslatorLookupElement(domainKey, domainKey));
                    }

                }
            }

        );

        extend(CompletionType.BASIC, TwigHelper.getPrintBlockFunctionPattern("controller"),  new ControllerCompletionProvider());


        // assets completion:
        // stylesheets and javascripts tags

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetPattern(), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader()
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("stylesheets"), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader().setFilterExtension("css", "less", "sass").setIncludeBundleDir(true)
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("javascripts"), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader().setFilterExtension("js", "dart", "coffee").setIncludeBundleDir(true)
        ));


        // routing completion like path() function
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableRoutePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    Symfony2ProjectComponent symfony2ProjectComponent = parameters.getPosition().getProject().getComponent(Symfony2ProjectComponent.class);
                    Map<String,Route> routes = symfony2ProjectComponent.getRoutes();

                    for (Route route : routes.values()) {
                        resultSet.addElement(new RouteLookupElement(route));
                    }
                }
            }
        );


    }

    private class TemplateCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters,
            ProcessingContext context,
            @NotNull CompletionResultSet resultSet) {

            if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                return;
            }

            Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
            for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
                resultSet.addElement(
                    new TemplateLookupElement(entry.getKey(), entry.getValue())
                );
            }
        }
    }

}
