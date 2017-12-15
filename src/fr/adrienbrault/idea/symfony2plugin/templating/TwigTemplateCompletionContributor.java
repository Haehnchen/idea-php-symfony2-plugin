package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.provider.AssetCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.completion.QuotedInsertionLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.collector.ControllerDocVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.completion.FunctionInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.or(
            TwigHelper.getTemplateFileReferenceTagPattern(),
            TwigHelper.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG)
        ), new TemplateCompletionProvider());

        // all file template "include" pattern
        extend(CompletionType.BASIC, PlatformPatterns.or(
            TwigHelper.getPrintBlockFunctionPattern("include", "source"),
            TwigHelper.getIncludeTagArrayPattern(),
            TwigHelper.getTagTernaryPattern(TwigElementTypes.INCLUDE_TAG)
        ),  new TemplateCompletionProvider());

        // provides support for 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain')
        // provides support for 'a<xxx>'|transchoice(2, {'%foo%' : bar|default}, 'Domain')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTranslationPattern("trans", "transchoice"),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
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
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
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
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    // find {% from "<template.name>"
                    PsiElement psiElement = PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), TwigHelper.getFromTemplateElement());
                    if(psiElement == null) {
                        return;
                    }

                    // {% from _self
                    if(psiElement.getNode().getElementType() == TwigTokenTypes.RESERVED_ID) {
                        attachLookupElements(resultSet, new PsiFile[]{psiElement.getContainingFile()});
                        return;
                    }

                    String templateName = psiElement.getText();
                    if(StringUtils.isBlank(templateName)) {
                        return;
                    }

                    PsiFile[] twigFilesByName = TwigHelper.getTemplatePsiElements(parameters.getPosition().getProject(), templateName);
                    if(twigFilesByName.length == 0) {
                        return;
                    }

                    attachLookupElements(resultSet, twigFilesByName);
                }

                private void attachLookupElements(@NotNull CompletionResultSet resultSet, PsiFile[] psiFiles) {
                    for (PsiFile psiFile : psiFiles) {
                        for (TwigMacroTagInterface entry: TwigUtil.getMacros(psiFile)) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getName()).withTypeText(entry.getParameters(), true).withIcon(TwigIcons.TwigFileIcon));
                        }
                    }
                }

            }
        );

        // {{ 'test'|<caret> }}
        extend(
            CompletionType.BASIC,
            TwigHelper.getFilterPattern(),
            new FilterCompletionProvider()
        );

        // provides support for {{ '<xxx>' }}
        extend(
            CompletionType.BASIC,
            TwigHelper.getCompletablePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition().getOriginalElement();

                    for(Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFunctions().entrySet()) {
                        resultSet.addElement(new TwigExtensionLookupElement(psiElement.getProject(), entry.getKey(), entry.getValue()));
                    }

                    // {% import 'forms.html' as forms %}
                    for(TwigMacro twigMacro: TwigUtil.getImportedMacros(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate(), true).withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    // {% from 'forms.html' import input as input_field, textarea %}
                    for(TwigMacro twigMacro: TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName())
                            .withTypeText(twigMacro.getTemplate(), true)
                            .withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance())
                        );
                    }

                    for(TwigSet twigSet: TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigSet.getName()).withTypeText("set", true));
                    }

                    for(Map.Entry<String, PsiVariable> entry: TwigTypeResolveUtil.collectScopeVariables(parameters.getOriginalPosition()).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(TwigTypeResolveUtil.getTypeDisplayName(psiElement.getProject(), entry.getValue().getTypes()), true).withIcon(PhpIcons.CLASS));
                    }

                    for(Map.Entry<String, TwigGlobalVariable> entry: ServiceXmlParserFactory.getInstance(psiElement.getProject(), TwigGlobalsServiceParser.class).getTwigGlobals().entrySet()) {
                        if(entry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.TEXT) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue().getValue(), true).withIcon(PhpIcons.CONSTANT));
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
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
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
        extend(CompletionType.BASIC, TwigHelper.getTransDefaultDomainPattern(), new TranslationDomainCompletionProvider());

        // {% trans from "<carpet>" %}
        // {% transchoice from "<carpet>" %}
        extend(CompletionType.BASIC, TwigHelper.getTranslationTokenTagFromPattern(), new TranslationDomainCompletionProvider());

        // {{ controller('<caret>') }}
        // {% render(controller('<caret>')) %}
        extend(CompletionType.BASIC, TwigHelper.getPrintBlockOrTagFunctionPattern("controller"), new ControllerCompletionProvider());

        // {% render '<caret>' %}"
        extend(CompletionType.BASIC, TwigHelper.getStringAfterTagNamePattern("render"), new ControllerCompletionProvider());

        // assets completion:
        // stylesheets and javascripts tags

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetPattern(), new AssetCompletionProvider().setAssetParser(
            new AssetDirectoryReader()
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("stylesheets"), new AssetCompletionProvider().setIncludeCustom(true).setAssetParser(
            new AssetDirectoryReader().setFilterExtension(TwigHelper.CSS_FILES_EXTENSIONS).setIncludeBundleDir(true)
        ));

        extend(CompletionType.BASIC, TwigHelper.getAutocompletableAssetTag("javascripts"), new AssetCompletionProvider().setIncludeCustom(true).setAssetParser(
            new AssetDirectoryReader().setFilterExtension(TwigHelper.JS_FILES_EXTENSIONS).setIncludeBundleDir(true)
        ));

        // routing completion like path() function
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableRoutePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
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

        // {% import 'detail/index.html.twig' as foobar %}
        // {{ foobar.<caret> }}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTypeCompletionPattern(),
            new MyMacroImportAsCompletionProvider()
        );

        // {# @var variable \Foo\ClassName #}
        // {# variable \Foo\ClassName #}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTwigTypeDocBlock(),
            new PhpClassCompletionProvider(true).withTrimLeadBackslash(true)
        );

        // {# @Container Foo:Bar #}
        extend(
            CompletionType.BASIC,
            TwigHelper.getTwigDocBlockMatchPattern(ControllerDocVariableCollector.DOC_PATTERN_COMPLETION),
            new ControllerCompletionProvider()
        );

        // {% form_theme * %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getFormThemeFileTag(),
            new FormThemeCompletionProvider()
        );

        // {% <carpet> %}
        extend(CompletionType.BASIC,
            TwigHelper.getTagTokenParserPattern(),
            new TagTokenParserCompletionProvider()
        );

        // {% if foo is defined %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getAfterIsTokenPattern(),
            new TwigSimpleTestParametersCompletionProvider()
        );

        // {% if foo.bar <carpet> %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getAfterOperatorPattern(),
            new TwigOperatorCompletionProvider()
        );

        // {% constant('FOO') %}
        extend(
            CompletionType.BASIC,
            TwigHelper.getPrintBlockOrTagFunctionPattern("constant"),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    PsiElement position = parameters.getPosition();
                    if(!Symfony2ProjectComponent.isEnabled(position)) {
                        return;
                    }

                    PhpIndex instance = PhpIndex.getInstance(position.getProject());
                    for(String constant : instance.getAllConstantNames(PrefixMatcher.ALWAYS_TRUE)) {
                        resultSet.addElement(LookupElementBuilder.create(constant).withIcon(PhpIcons.CONSTANT));
                    }

                    int foo = parameters.getOffset() - position.getTextRange().getStartOffset();
                    String before = position.getText().substring(0, foo);
                    String[] parts = before.split("::");

                    if(parts.length >= 1) {
                        PhpClass phpClass = PhpElementsUtil.getClassInterface(position.getProject(), parts[0].replace("\\\\", "\\"));
                        if(phpClass != null) {
                            phpClass.getFields().stream().filter(Field::isConstant).forEach(field ->
                                resultSet.addElement(LookupElementBuilder.create(phpClass.getPresentableFQN().replace("\\", "\\\\") + "::" + field.getName()).withIcon(PhpIcons.CONSTANT))
                            );
                        }
                    }
                }
            }
        );
    }

    private static class FilterCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                return;
            }

            // move this stuff to pattern fixed event stopping by phpstorm
            PsiElement currElement = parameters.getPosition().getOriginalElement();
            PsiElement prevElement = currElement.getPrevSibling();
            if ((prevElement != null) && ((prevElement instanceof PsiWhiteSpace))) prevElement = prevElement.getPrevSibling();

            if ((prevElement != null) && (prevElement.getNode().getElementType() == TwigTokenTypes.FILTER)) {
                for(Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(parameters.getPosition().getProject()).getFilters().entrySet()) {
                    resultSet.addElement(new TwigExtensionLookupElement(currElement.getProject(), entry.getKey(), entry.getValue()));
                }
            }
        }
    }

    /**
     * Parse all classes that implements Twig_TokenParserInterface::getTag and provide completion on string
     */
    private static class TagTokenParserCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            TwigUtil.visitTokenParsers(position.getProject(), pair ->
                resultSet.addElement(LookupElementBuilder.create(pair.getFirst()).withIcon(Symfony2Icons.SYMFONY))
            );

            // add special tag ending, provide a static list. there no suitable safe way to extract them
            // search able via: "return $token->test(array('end"
            for (String s : new String[]{"endtranschoice", "endtrans"}) {
                resultSet.addElement(LookupElementBuilder.create(s).withIcon(Symfony2Icons.SYMFONY));
            }
        }
    }

    private static class TranslationDomainCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                return;
            }

            List<LookupElement> translationDomainLookupElements = TranslationUtil.getTranslationDomainLookupElements(
                parameters.getPosition().getProject()
            );

            // decorate lookup elements to attach insert handle for quoted wrap
            resultSet.addAllElements(
                ContainerUtil.map(translationDomainLookupElements, QuotedInsertionLookupElement::new)
            );
        }
    }

    private static class TwigSimpleTestParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            Project project = position.getProject();
            for (Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(project).getSimpleTest().entrySet()) {
                resultSet.addElement(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
            }
        }
    }

    private static class TwigOperatorCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            Project project = position.getProject();
            for (Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(project).getOperators().entrySet()) {
                resultSet.addElement(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
            }
        }
    }

    private class FormThemeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement psiElement = parameters.getOriginalPosition();

            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            resultSet.addAllElements(TwigHelper.getTwigLookupElements(parameters.getPosition().getProject()));
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

            resultSet.addAllElements(TwigHelper.getTwigLookupElements(parameters.getPosition().getProject()));
        }
    }

    private class BlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            // wtf: need to prefix the block tag itself. remove this behavior and strip for new Matcher
            // Find first Identifier "b" char or fallback to empty:
            // "{% block b", "{% block"
            String blockNamePrefix = resultSet.getPrefixMatcher().getPrefix();
            int spacePos = blockNamePrefix.lastIndexOf(' ');
            blockNamePrefix = spacePos > 0 ? blockNamePrefix.substring(spacePos + 1) : "";
            CompletionResultSet myResultSet = resultSet.withPrefixMatcher(blockNamePrefix);

            // collect blocks in all related files
            Pair<PsiFile[], Boolean> scopedContext = TwigHelper.findScopedFile(position);

            Set<String> uniqueList = new HashSet<>();
            for (TwigBlock block : new TwigBlockParser(scopedContext.getSecond()).visit(scopedContext.getFirst())) {
                if(uniqueList.contains(block.getName())) {
                    continue;
                }

                uniqueList.add(block.getName());
                myResultSet.addElement(new TwigBlockLookupElement(block));
            }
        }
    }

    /**
     * {% import 'detail/index.html.twig' as foobar %}
     * {{ foobar.<caret> }}
     */
    private static class MyMacroImportAsCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement psiElement = parameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            // "foobar".<caret>
            String[] possibleTypes = TwigTypeResolveUtil.formatPsiTypeName(psiElement);
            if(possibleTypes.length != 1) {
                return;
            }

            resultSet.addAllElements(
                TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile()).stream()
                    .filter(twigMacro ->
                        twigMacro.getName().startsWith(possibleTypes[0] + ".")
                    )
                    .map((Function<TwigMacro, LookupElement>) twigMacro ->
                        LookupElementBuilder.create(twigMacro.getName().substring(possibleTypes[0].length() + 1))
                            .withTypeText(twigMacro.getTemplate(), true)
                            .withTailText(twigMacro.getParameter(), true)
                            .withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance())
                    ).collect(Collectors.toList())
            );
        }
    }
}

