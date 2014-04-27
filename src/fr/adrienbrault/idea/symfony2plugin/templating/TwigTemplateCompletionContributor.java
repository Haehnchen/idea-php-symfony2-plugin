package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.provider.AssetCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.ControllerDocVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.FunctionInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {

        extend(CompletionType.BASIC, TwigHelper.getTemplateFileReferenceTagPattern(),  new TemplateCompletionProvider());
        extend(CompletionType.BASIC, TwigHelper.getPrintBlockFunctionPattern("include", "source"),  new TemplateCompletionProvider());

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

                    resultSet.addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(psiElement.getProject(), domainName));

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

                    resultSet.addAllElements(
                        TranslationUtil.getTranslationDomainLookupElements(parameters.getPosition().getProject())
                    );

                }


            }

        );

        // provides support for {% block |
        extend(CompletionType.BASIC, TwigHelper.getBlockTagPattern(), new BlockCompletionProvider());

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
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue(), true).withIcon(TwigIcons.TwigFileIcon));
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
            CompletionType.SMART,
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
                        for(Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFilters().entrySet()) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withIcon(TwigExtensionParser.getIcon(entry.getValue().getTwigExtensionType())).withTypeText(entry.getValue().getType()));
                        }
                    }

                }

            }

        );

        // provides support for {{ '<xxx>' }}
        extend(
            CompletionType.BASIC,
            TwigHelper.getCompletablePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition().getOriginalElement();

                    for(Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFunctions().entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withIcon(TwigExtensionParser.getIcon(entry.getValue().getTwigExtensionType())).withTypeText(entry.getValue().getType()).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigMacro twigMacro: TwigUtil.getImportedMacros(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate()).withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigMacro twigMacro: TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate()).withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    for(TwigSet twigSet: TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigSet.getName()).withTypeText("set"));
                    }

                    for(Map.Entry<String, PsiVariable> entry: TwigTypeResolveUtil.collectScopeVariables(parameters.getOriginalPosition()).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(TwigTypeResolveUtil.getTypeDisplayName(psiElement.getProject(), entry.getValue().getTypes())).withIcon(PhpIcons.CLASS));
                    }

                    for(Map.Entry<String, TwigGlobalVariable> entry: ServiceXmlParserFactory.getInstance(psiElement.getProject(), TwigGlobalsServiceParser.class).getTwigGlobals().entrySet()) {
                        if(entry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.TEXT) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue().getValue()).withIcon(PhpIcons.CONSTANT));
                        }
                    }
                }
            }

        );

        // {% for user in "users" %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getVariableTypePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getOriginalPosition();
                    if(psiElement == null) {
                        return;
                    }

                    for(Map.Entry<String, PsiVariable> entry: TwigTypeResolveUtil.collectScopeVariables(parameters.getOriginalPosition()).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(TwigTypeResolveUtil.getTypeDisplayName(psiElement.getProject(), entry.getValue().getTypes())).withIcon(PhpIcons.CLASS));
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
            new AssetDirectoryReader().setFilterExtension(TwigHelper.CSS_FILES_EXTENSIONS).setIncludeBundleDir(true)
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("javascripts"), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader().setFilterExtension(TwigHelper.JS_FILES_EXTENSIONS).setIncludeBundleDir(true)
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

                    resultSet.addAllElements(RouteHelper.getRoutesLookupElements(parameters.getPosition().getProject()));
                }
            }
        );

        // routing parameter completion
        extend(
            CompletionType.BASIC,
            TwigHelper.getPathAfterLeafPattern(),
            new PathParameterCompletionProvider()
        );

        // simulated php completion var.<foo>
        extend(
            CompletionType.BASIC,
            TwigHelper.getTypeCompletionPattern(),
            new TypeCompletionProvider()
        );

        // {# variable \Foo\ClassName #}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTwigTypeDocBlock(),
            new TwigDocBlockTypeClassCompletionProvider()
        );

        // {# @Container Foo:Bar #}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTwigDocBlockMatchPattern(ControllerDocVariableCollector.DOC_PATTERN_COMPLETION),
            new ControllerCompletionProvider()
        );

        // {# @Container Foo:Bar #}
        extend(
            CompletionType.BASIC,
            TwigHelper.getFormThemeFileTag(),
            new FormThemeCompletionProvider()
        );


    }

    private class FormThemeCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {

            PsiElement psiElement = parameters.getOriginalPosition();

            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
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

    /**
     * think of PhpClassCompletionProvider
     */
    private class TwigDocBlockTypeClassCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters paramV, ProcessingContext paramProcessingContext, @NotNull CompletionResultSet resultSet) {

            PsiElement psiElement = paramV.getOriginalPosition();

            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
            for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {
                resultSet.addElement(new PhpLookupElement(className, PhpClassIndex.KEY, psiElement.getProject(), PhpReferenceInsertHandler.getInstance()));
            }

        }
    }

    private class TypeCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext paramProcessingContext, @NotNull CompletionResultSet resultSet) {

            PsiElement psiElement = parameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            String[] possibleTypes = TwigTypeResolveUtil.formatPsiTypeName(psiElement);

            // find core function for that
            for(TwigTypeContainer twigTypeContainer: TwigTypeResolveUtil.resolveTwigMethodName(psiElement, possibleTypes)) {
                if(twigTypeContainer.getPhpNamedElement() instanceof PhpClass) {

                    for(Method method: ((PhpClass) twigTypeContainer.getPhpNamedElement()).getMethods()) {
                        if(!(!method.getModifier().isPublic() || method.getName().startsWith("set") || method.getName().startsWith("__"))) {
                            resultSet.addElement(new PhpTwigMethodLookupElement(method));
                        }
                    }

                    for(Field field: ((PhpClass) twigTypeContainer.getPhpNamedElement()).getFields()) {
                        if(field.getModifier().isPublic()) {
                            resultSet.addElement(new PhpTwigMethodLookupElement(field));
                        }
                    }

                }

                if(twigTypeContainer.getStringElement() != null) {
                    resultSet.addElement(LookupElementBuilder.create(twigTypeContainer.getStringElement()));
                }
            }

        }

    }

    private class PathParameterCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext paramProcessingContext, @NotNull CompletionResultSet paramCompletionResultSet) {

            PsiElement psiElement = parameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            String routeName = TwigHelper.getMatchingRouteNameOnParameter(parameters.getOriginalPosition());
            if(routeName == null) {
                return;
            }

            paramCompletionResultSet.addAllElements(Arrays.asList(
                RouteHelper.getRouteParameterLookupElements(parameters.getPosition().getProject(), routeName))
            );

        }

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


    class BlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

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

}
