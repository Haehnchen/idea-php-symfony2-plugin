package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.provider.AssetCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.completion.QuotedInsertionLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormDataHolder;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.collector.ControllerDocVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.FunctionInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.or(
            TwigPattern.getTemplateFileReferenceTagPattern(),
            TwigPattern.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG)
        ), new TemplateCompletionProvider());

        // all file template "include" pattern
        extend(CompletionType.BASIC, PlatformPatterns.or(
            TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source"),
            TwigPattern.getIncludeTagArrayPattern(),
            TwigPattern.getTagTernaryPattern(TwigElementTypes.INCLUDE_TAG)
        ), new TemplateCompletionProvider());

        // provides support for 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain')
        // provides support for 'a<xxx>'|transchoice(2, {'%foo%' : bar|default}, 'Domain')
        extend(
            CompletionType.BASIC,
            TwigPattern.getTranslationKeyPattern("trans", "transchoice"),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition();
                    String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

                    resultSet.addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(psiElement.getProject(), domainName));
                }
            }
        );

        // provides support for 'a'|trans({'%foo%' : bar|default}, '<xxx>')
        // provides support for 'a'|transchoice(2, {'%foo%' : bar|default}, '<xxx>')
        extend(
            CompletionType.BASIC,
            TwigPattern.getTransDomainPattern(),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    if (PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice"))) == null) {
                        return;
                    }

                    resultSet.addAllElements(
                        TranslationUtil.getTranslationDomainLookupElements(parameters.getPosition().getProject())
                    );
                }
            }
        );

        // provides support for {% block |
        extend(CompletionType.BASIC, TwigPattern.getBlockTagPattern(), new BlockCompletionProvider());

        // provides support for {% from 'twig..' import |
        extend(
            CompletionType.BASIC,
            TwigPattern.getTemplateImportFileReferenceTagPattern(),

            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

                    PsiElement position = parameters.getPosition();
                    if (!Symfony2ProjectComponent.isEnabled(position)) {
                        return;
                    }

                    PsiElement parent = position.getParent();
                    if (parent == null) {
                        return;
                    }

                    // find {% from "<template.name>"
                    PsiElement psiElement = PsiElementUtils.getPrevSiblingOfType(parent, TwigPattern.getFromTemplateElementPattern());
                    if (psiElement == null) {
                        return;
                    }

                    // {% from _self
                    if (psiElement.getNode().getElementType() == TwigTokenTypes.RESERVED_ID) {
                        attachLookupElements(resultSet, Collections.singletonList(psiElement.getContainingFile()));
                        return;
                    }

                    String templateName = psiElement.getText();
                    if (StringUtils.isBlank(templateName)) {
                        return;
                    }

                    Collection<PsiFile> twigFilesByName = TwigUtil.getTemplatePsiElements(position.getProject(), templateName);
                    if (twigFilesByName.size() == 0) {
                        return;
                    }

                    attachLookupElements(resultSet, twigFilesByName);
                }

                private void attachLookupElements(@NotNull CompletionResultSet resultSet, Collection<PsiFile> psiFiles) {
                    for (PsiFile psiFile : psiFiles) {
                        for (TwigMacroTagInterface entry : TwigUtil.getMacros(psiFile)) {
                            resultSet.addElement(LookupElementBuilder.create(entry.name()).withTypeText(entry.parameters(), true).withIcon(TwigIcons.TwigFileIcon));
                        }
                    }
                }

            }
        );

        // {{ 'test'|<caret> }}
        extend(
            CompletionType.BASIC,
            TwigPattern.getFilterPattern(),
            new FilterCompletionProvider()
        );

        // {% apply upper %}This text becomes uppercase{% endapply %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getApplyFilterPattern(),
            new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                    Project project = completionParameters.getPosition().getProject();
                    for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFilters(project).entrySet()) {
                        completionResultSet.addElement(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
                    }
                }
            }
        );

        // provides support for {{ '<xxx>' }}
        extend(
            CompletionType.BASIC,
            TwigPattern.getCompletablePattern(),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {

                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition().getOriginalElement();

                    for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFunctions(parameters.getPosition().getProject()).entrySet()) {
                        resultSet.addElement(new TwigExtensionLookupElement(psiElement.getProject(), entry.getKey(), entry.getValue()));
                    }

                    // {{ _self.inp }} => {{ _self.input() }}
                    if (TwigPattern.getSelfMacroIdentifierPattern().accepts(psiElement)) {
                        TwigUtil.visitMacros(psiElement.getContainingFile(), pair -> {
                            TwigMacroTag twigMacro = pair.getFirst();

                            resultSet.addElement(LookupElementBuilder
                                .create(twigMacro.name())
                                .withIcon(TwigIcons.TwigFileIcon)
                                .withInsertHandler(FunctionInsertHandler.getInstance())
                            );
                        });
                    }

                    // {% import 'forms.html' as forms %}
                    for (TwigMacro twigMacro : TwigUtil.getImportedMacros(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName()).withTypeText(twigMacro.getTemplate(), true).withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance()));
                    }

                    // {% from 'forms.html' import input as input_field, textarea %}
                    for (TwigMacro twigMacro : TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigMacro.getName())
                            .withTypeText(twigMacro.getTemplate(), true)
                            .withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance())
                        );
                    }

                    for (String twigSet : TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
                        resultSet.addElement(LookupElementBuilder.create(twigSet).withTypeText("set", true));
                    }

                    for (Map.Entry<String, PsiVariable> entry : TwigTypeResolveUtil.collectScopeVariables(parameters.getOriginalPosition()).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(TwigTypeResolveUtil.getTypeDisplayName(psiElement.getProject(), entry.getValue().getTypes()), true).withIcon(PhpIcons.CLASS));
                    }

                    for (Map.Entry<String, TwigGlobalVariable> entry : ServiceXmlParserFactory.getInstance(psiElement.getProject(), TwigGlobalsServiceParser.class).getTwigGlobals().entrySet()) {
                        if (entry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.TEXT) {
                            resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(entry.getValue().getValue(), true).withIcon(PhpIcons.CONSTANT));
                        }
                    }
                }
            }
        );

        // {% for user in "users" %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getVariableTypePattern(),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    PsiElement psiElement = parameters.getOriginalPosition();
                    if (psiElement == null) {
                        return;
                    }

                    for (Map.Entry<String, PsiVariable> entry : TwigTypeResolveUtil.collectScopeVariables(parameters.getOriginalPosition()).entrySet()) {
                        resultSet.addElement(LookupElementBuilder.create(entry.getKey()).withTypeText(TwigTypeResolveUtil.getTypeDisplayName(psiElement.getProject(), entry.getValue().getTypes())).withIcon(PhpIcons.CLASS));
                    }
                }
            }
        );

        // {% trans_default_domain <> %}
        // {% trans_default_domain '<>' %}
        extend(CompletionType.BASIC, TwigPattern.getTransDefaultDomainPattern(), new TranslationDomainCompletionProvider());

        // {% trans from "<carpet>" %}
        // {% transchoice from "<carpet>" %}
        extend(CompletionType.BASIC, TwigPattern.getTranslationTokenTagFromPattern(), new TranslationDomainCompletionProvider());

        // {{ controller('<caret>') }}
        // {% render(controller('<caret>')) %}
        extend(CompletionType.BASIC, TwigPattern.getPrintBlockOrTagFunctionPattern("controller"), new ControllerCompletionProvider());

        // {% foo() %}
        // {% foo.bar() %}
        // {{ 'test'|<caret> }}
        // {% apply <caret> %}foobar{% endapply %}
        extend(CompletionType.BASIC,
            TwigPattern.getFunctionStringParameterPattern(),
            new PhpProxyForTwigTypCompletionProvider()
        );

        // {% render '<caret>' %}"
        extend(CompletionType.BASIC, TwigPattern.getStringAfterTagNamePattern("render"), new ControllerCompletionProvider());

        // assets completion:
        // stylesheets and javascripts tags

        // {{ asset('<caret>') }}
        extend(CompletionType.BASIC, TwigPattern.getAutocompletableAssetPattern(), new AssetCompletionProvider(
            new AssetDirectoryReader()
        ));

        extend(
            CompletionType.BASIC,
            TwigPattern.getAutocompletableAssetTag("stylesheets"), new AssetCompletionProvider(
                new AssetDirectoryReader(TwigUtil.CSS_FILES_EXTENSIONS, true),
                true
            )
        );

        extend(
            CompletionType.BASIC,
            TwigPattern.getAutocompletableAssetTag("javascripts"), new AssetCompletionProvider(
                new AssetDirectoryReader(TwigUtil.JS_FILES_EXTENSIONS, true),
                true
            )
        );

        // routing completion like path() function
        extend(
            CompletionType.BASIC,
            TwigPattern.getAutocompletableRoutePattern(),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    resultSet.addAllElements(RouteHelper.getRoutesLookupElements(parameters.getPosition().getProject()));
                }
            }
        );

        // {{ component('<caret>'}) }}
        // {% component FOO
        extend(
            CompletionType.BASIC,
            PlatformPatterns.or(TwigPattern.getComponentPattern(), TwigPattern.getArgumentAfterTagNamePattern("component")),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    if (!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    resultSet.addAllElements(UxUtil.getComponentLookupElements(parameters.getPosition().getProject()));
                }
            }
        );

        // routing parameter completion
        extend(
            CompletionType.BASIC,
            TwigPattern.getPathAfterLeafPattern(),
            new PathParameterCompletionProvider()
        );

        // simulated php completion var.<foo>
        extend(
            CompletionType.BASIC,
            TwigPattern.getTypeCompletionPattern(),
            new TypeCompletionProvider()
        );

        // {% import 'detail/index.html.twig' as foobar %}
        // {{ foobar.<caret> }}
        extend(
            CompletionType.BASIC,
            TwigPattern.getTypeCompletionPattern(),
            new MyMacroImportAsCompletionProvider()
        );

        // {# @var variable \Foo\ClassName #}
        // {# variable \Foo\ClassName #}
        extend(
            CompletionType.BASIC,
            TwigPattern.getTwigTypeDocBlockPattern(),
            new PhpClassCompletionProvider(true).withTrimLeadBackslash(true)
        );

        // {# @Container Foo:Bar #}
        extend(
            CompletionType.BASIC,
            TwigPattern.getTwigDocBlockMatchPattern(ControllerDocVariableCollector.DOC_PATTERN_COMPLETION),
            new ControllerCompletionProvider()
        );

        // {% form_theme * %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getFormThemeFileTagPattern(),
            new FormThemeCompletionProvider()
        );

        // {% <carpet> %}
        extend(CompletionType.BASIC,
            TwigPattern.getTagTokenParserPattern(),
            new TagTokenParserCompletionProvider()
        );

        // {% if foo is defined %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getAfterIsTokenPattern(),
            new TwigSimpleTestParametersCompletionProvider()
        );

        // {% if foo.bar <carpet> %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getAfterOperatorPattern(),
            new TwigOperatorCompletionProvider()
        );

        // {% constant('FOO') %}
        extend(
            CompletionType.BASIC,
            TwigPattern.getPrintBlockOrTagFunctionPattern("constant"),
            new CompletionProvider<>() {
                public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
                    PsiElement position = parameters.getPosition();
                    if (!Symfony2ProjectComponent.isEnabled(position)) {
                        return;
                    }

                    PhpIndex instance = PhpIndex.getInstance(position.getProject());
                    for (String constant : instance.getAllConstantNames(PrefixMatcher.ALWAYS_TRUE)) {
                        resultSet.addElement(LookupElementBuilder.create(constant).withIcon(PhpIcons.CONSTANT));
                    }

                    int foo = parameters.getOffset() - position.getTextRange().getStartOffset();
                    String before = position.getText().substring(0, foo);
                    String[] parts = before.split("::");

                    if (parts.length >= 1) {
                        PhpClass phpClass = PhpElementsUtil.getClassInterface(position.getProject(), parts[0].replace("\\\\", "\\"));
                        if (phpClass != null) {
                            phpClass.getFields().stream().filter(Field::isConstant).forEach(field ->
                                resultSet.addElement(LookupElementBuilder.create(phpClass.getPresentableFQN().replace("\\", "\\\\") + "::" + field.getName()).withIcon(PhpIcons.CONSTANT))
                            );
                        }
                    }
                }
            }
        );

        // {% e => {% extends '...'
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME).withSuperParent(2, PsiFile.class),
            new IncompleteExtendsCompletionProvider()
        );

        // {% in => {% include '...'
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteIncludeCompletionProvider()
        );

        // {% em => {% embed '...'
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteEmbedCompletionProvider()
        );

        // {% bl => {% block '...'
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteBlockCompletionProvider()
        );

        // {% com => {% com '...'
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteComponentCompletionProvider()
        );

        // {{ com => {{ com('...')
        extend(
            CompletionType.BASIC,
            TwigPattern.getCompletablePattern(),
            new IncompleteComponentPrintBlockCompletionProvider()
        );

        // {{ in => {{ include('...')
        extend(
            CompletionType.BASIC,
            TwigPattern.getCompletablePattern(),
            new IncompleteIncludePrintBlockCompletionProvider()
        );

        // {% for => "for flash in app.flashes"
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteForCompletionProvider()
        );

        // {% if => "if app.debug"
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME),
            new IncompleteIfCompletionProvider()
        );
    }

    private boolean isCompletionStartingMatch(@NotNull String fullText, @NotNull CompletionParameters completionParameters, int minLength) {
        PsiElement originalPosition = completionParameters.getOriginalPosition();
        if (originalPosition != null) {
            String text = originalPosition.getText();
            if (text.length() >= minLength && fullText.startsWith(text)) {
                return true;
            }
        }

        PsiElement position = completionParameters.getPosition();
        String text = position.getText().toLowerCase().replace("intellijidearulezzz", "");
        if (text.length() >= minLength && fullText.startsWith(text)) {
            return true;
        }

        return false;
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
                for(Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getFilters(parameters.getPosition().getProject()).entrySet()) {
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

            for (String namedTokenParserTag : TwigUtil.getNamedTokenParserTags(position.getProject())) {
                resultSet.addElement(LookupElementBuilder.create(namedTokenParserTag).withIcon(Symfony2Icons.SYMFONY));
            }

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
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            Project project = position.getProject();
            for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getSimpleTest(project).entrySet()) {
                resultSet.addElement(new TwigExtensionLookupElement(project, entry.getKey(), entry.getValue()));
            }
        }
    }

    private static class TwigOperatorCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            Project project = position.getProject();
            for (Map.Entry<String, TwigExtension> entry : TwigExtensionParser.getOperators(project).entrySet()) {
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

            resultSet.addAllElements(TwigUtil.getTwigLookupElements(parameters.getPosition().getProject()));
        }
    }

    private class TypeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext paramProcessingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement psiElement = parameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            Collection<String> possibleTypes = TwigTypeResolveUtil.formatPsiTypeName(psiElement);

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
                    LookupElementBuilder lookupElement = LookupElementBuilder.create(twigTypeContainer.getStringElement());

                    // form
                    Object dataHolder = twigTypeContainer.getDataHolder();
                    if (dataHolder instanceof FormDataHolder) {
                        lookupElement = lookupElement.withIcon(Symfony2Icons.FORM_TYPE);

                        lookupElement = lookupElement.withTypeText(((FormDataHolder) dataHolder).getPhpClass().getName());
                        lookupElement = lookupElement.withTailText("(" + ((FormDataHolder) dataHolder).getFormType().getName() + ")", true);
                    }

                    resultSet.addElement(lookupElement);
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

            String routeName = TwigUtil.getMatchingRouteNameOnParameter(parameters.getOriginalPosition());
            if(routeName == null) {
                return;
            }

            paramCompletionResultSet.addAllElements(Arrays.asList(
                RouteHelper.getRouteParameterLookupElements(parameters.getPosition().getProject(), routeName))
            );

        }

    }

    private static class TemplateCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters,
                                   @NotNull ProcessingContext context,
                                   @NotNull CompletionResultSet resultSet) {

            PsiElement psiElement = parameters.getPosition();
            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            List<String> prioritizedKeys = new ArrayList<>();
            Project project = psiElement.getProject();

            if (TwigPattern.getTemplateFileReferenceTagPattern("extends").accepts(psiElement)) {
                prioritizedKeys.addAll(TwigUtil.getExtendsTemplateUsageAsOrderedList(project));
            } else if (TwigPattern.getTemplateFileReferenceTagPattern("embed").accepts(psiElement)) {
                prioritizedKeys.addAll(TwigUtil.getEmbedTemplateUsageAsOrderedList(project));
            } else if (TwigPattern.getTemplateFileReferenceTagPattern("include").accepts(psiElement) || TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source").accepts(psiElement)) {
                prioritizedKeys.addAll(TwigUtil.getIncludeTemplateUsageAsOrderedList(project));
            }

            if (prioritizedKeys.size() > 0) {
                CompletionSorter completionSorter = CompletionService.getCompletionService()
                    .defaultSorter(parameters, resultSet.getPrefixMatcher())
                    .weighBefore("priority", new ServiceCompletionProvider.MyLookupElementWeigher(prioritizedKeys));

                resultSet = resultSet.withRelevanceSorter(completionSorter);
            }

            resultSet.addAllElements(TwigUtil.getTwigLookupElements(project, new HashSet<>(prioritizedKeys)));
        }
    }

    private class BlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            PsiElement position = parameters.getOriginalPosition();
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
            Pair<Collection<PsiFile>, Boolean> scopedContext = TwigUtil.findScopedFile(position);

            myResultSet.addAllElements(TwigUtil.getBlockLookupElements(
                position.getProject(),
                TwigFileUtil.collectParentFiles(scopedContext.getSecond(), scopedContext.getFirst())
            ));
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
            Collection<String> possibleTypes = TwigTypeResolveUtil.formatPsiTypeName(psiElement);
            if(possibleTypes.size() != 1) {
                return;
            }

            String rootElement = possibleTypes.iterator().next();

            resultSet.addAllElements(
                TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile()).stream()
                    .filter(twigMacro ->
                        twigMacro.getName().startsWith(rootElement + ".")
                    )
                    .map((Function<TwigMacro, LookupElement>) twigMacro ->
                        LookupElementBuilder.create(twigMacro.getName().substring(rootElement.length() + 1))
                            .withTypeText(twigMacro.getTemplate(), true)
                            .withTailText(twigMacro.getParameter(), true)
                            .withIcon(TwigIcons.TwigFileIcon).withInsertHandler(FunctionInsertHandler.getInstance())
                    ).collect(Collectors.toList())
            );
        }
    }

    /**
     * {% e => {% extends '...'
     */
    private class IncompleteExtendsCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            if (!isCompletionStartingMatch("extends", completionParameters, 1)) {
                return;
            }

            List<String> extendsTemplateUsageAsOrderedList = TwigUtil.getExtendsTemplateUsageAsOrderedList(completionParameters.getPosition().getProject());

            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(completionParameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new ServiceCompletionProvider.MyLookupElementWeigher(extendsTemplateUsageAsOrderedList));

            resultSet = resultSet.withRelevanceSorter(completionSorter);

            for (String s : extendsTemplateUsageAsOrderedList) {
                resultSet.addElement(LookupElementBuilder.create(String.format("extends '%s'", s)).withIcon(TwigIcons.TwigFileIcon));
            }
        }
    }

    /**
     * {% in => {% include '...'
     */
    private class IncompleteIncludeCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("include startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "include".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("include", completionParameters, 2)) {
                return;
            }

            List<String> extendsTemplateUsageAsOrderedList = TwigUtil.getIncludeTemplateUsageAsOrderedList(completionParameters.getPosition().getProject());

            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(completionParameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new ServiceCompletionProvider.MyLookupElementWeigher(extendsTemplateUsageAsOrderedList));

            resultSet = resultSet.withRelevanceSorter(completionSorter);

            for (String s : extendsTemplateUsageAsOrderedList) {
                resultSet.addElement(LookupElementBuilder.create(String.format("include '%s'", s)).withIcon(TwigIcons.TwigFileIcon));
            }
        }
    }

    /**
     * {% em => {% embed '...'
     */
    private class IncompleteEmbedCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("embed startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "embed".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("embed", completionParameters, 2)) {
                return;
            }

            List<String> extendsTemplateUsageAsOrderedList = TwigUtil.getEmbedTemplateUsageAsOrderedList(completionParameters.getPosition().getProject());

            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(completionParameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new ServiceCompletionProvider.MyLookupElementWeigher(extendsTemplateUsageAsOrderedList));

            resultSet = resultSet.withRelevanceSorter(completionSorter);

            for (String s : extendsTemplateUsageAsOrderedList) {
                resultSet.addElement(LookupElementBuilder.create(String.format("embed '%s'", s)).withIcon(TwigIcons.TwigFileIcon));
            }
        }
    }

    /**
     * {{ in => {{ include('...')
     */
    private class IncompleteIncludePrintBlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("include startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "include".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("include", completionParameters, 2)) {
                return;
            }

            List<String> extendsTemplateUsageAsOrderedList = TwigUtil.getIncludeTemplateUsageAsOrderedList(completionParameters.getPosition().getProject());

            CompletionSorter completionSorter = CompletionService.getCompletionService()
                .defaultSorter(completionParameters, resultSet.getPrefixMatcher())
                .weighBefore("priority", new ServiceCompletionProvider.MyLookupElementWeigher(extendsTemplateUsageAsOrderedList));

            resultSet = resultSet.withRelevanceSorter(completionSorter);

            for (String s : extendsTemplateUsageAsOrderedList) {
                resultSet.addElement(LookupElementBuilder.create(String.format("include('%s')", s)).withIcon(TwigIcons.TwigFileIcon));
            }
        }
    }

    /**
     * {% for => "for flash in app.flashes"
     */
    private class IncompleteForCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if (!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("for startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "for".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("for", completionParameters, 2)) {
                return;
            }

            resultSet.addAllElements(processVariables(
                completionParameters.getPosition(),
                PhpType::isArray,
                pair -> {
                    String var = pair.getValue().getFirst();
                    String unpluralize = StringUtil.unpluralize(var);
                    if (unpluralize != null) {
                        var = unpluralize;
                    }

                    return String.format("for %s in %s", var, pair.getKey());
                }
            ));
        }
    }

    /**
     * {% if => "if app.debug"
     */
    private class IncompleteIfCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("if startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "if".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("if", completionParameters, 2)) {
                return;
            }

            resultSet.addAllElements(processVariables(
                completionParameters.getPosition(),
                PhpType::isBoolean,
                entry -> String.format("if %s", entry.getKey())
            ));
        }
    }

    /**
     * {% bl => {% block '...'
     */
    private class IncompleteBlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement position = completionParameters.getOriginalPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("embed startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "block".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("block", completionParameters, 2)) {
                return;
            }

            Pair<Collection<PsiFile>, Boolean> scopedContext = TwigUtil.findScopedFile(position);

            Collection<LookupElement> blockLookupElements = TwigUtil.getBlockLookupElements(
                position.getProject(),
                TwigFileUtil.collectParentFiles(scopedContext.getSecond(), scopedContext.getFirst())
            );

            for (LookupElement blockLookupElement : blockLookupElements) {
                resultSet.addElement(LookupElementBuilder.create("block " + blockLookupElement.getLookupString()).withIcon(TwigIcons.TwigFileIcon));
            }
        }
    }

    /**
     * {% com => {% com '...'
     */
    private class IncompleteComponentCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            PsiElement position = completionParameters.getOriginalPosition();
            if(!Symfony2ProjectComponent.isEnabled(position)) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(1).with(new PatternCondition<>("component startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "component".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("component", completionParameters, 2)) {
                return;
            }

            for (LookupElement blockLookupElement : UxUtil.getComponentLookupElements(position.getProject())) {
                resultSet.addElement(LookupElementBuilder.create("component " + blockLookupElement.getLookupString()).withIcon(Symfony2Icons.SYMFONY));
            }
        }
    }


    /**
     * {{ com => {{ component('...')
     */
    private class IncompleteComponentPrintBlockCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            resultSet.restartCompletionOnPrefixChange(StandardPatterns.string().longerThan(2).with(new PatternCondition<>("component startsWith") {
                @Override
                public boolean accepts(@NotNull String s, ProcessingContext processingContext) {
                    return "component".startsWith(s);
                }
            }));

            if (!isCompletionStartingMatch("component", completionParameters, 2)) {
                return;
            }

            for (LookupElement element : UxUtil.getComponentLookupElements(completionParameters.getPosition().getProject())) {
                resultSet.addElement(LookupElementBuilder.create(LookupElementBuilder.create(String.format("component('%s')", element.getLookupString()))).withIcon(Symfony2Icons.SYMFONY));
            }
        }
    }

    private static class PhpProxyForTwigTypCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
            PsiElement prevSibling = PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
            if (prevSibling == null) {
                return;
            }

            // @TODO: support more then the first argument
            int wantParameter = 0;

            Collection<Pair<PhpNamedElement, Integer>> sources = new ArrayList<>();
            if (PlatformPatterns.or(TwigPattern.getFilterPattern(), TwigPattern.getApplyFilterPattern()).accepts(prevSibling)) {
                // filters are move by one parameter to rights; its the value
                sources.addAll(getTargetFunctionCallWithParameterIndex(TwigTemplateGoToDeclarationHandler.getFilterGoTo(prevSibling), wantParameter + 1));
            } else {
                // {{ foo.foo() }}
                // {{ foo() }}
                Collection<PsiElement> typeGoto = TwigTemplateGoToDeclarationHandler.getTypeGoto(prevSibling);
                for (PsiElement psiElement : typeGoto) {
                    if(psiElement instanceof com.jetbrains.php.lang.psi.elements.Function) {
                        sources.add(Pair.pair((com.jetbrains.php.lang.psi.elements.Function) psiElement, wantParameter));
                    }
                }

                // not for "foobar" on "foo.foobar"
                PsiElement prevSibling1 = prevSibling.getPrevSibling();
                if (prevSibling1 == null || prevSibling1.getNode().getElementType() != TwigTokenTypes.DOT) {
                    sources.addAll(getTargetFunctionCallWithParameterIndex(TwigTemplateGoToDeclarationHandler.getFunctions(prevSibling), wantParameter));
                }
            }

            Collection<PsiElement> targets = new ArrayList<>();

            for (Pair<PhpNamedElement, Integer> pair : sources) {
                int i = pair.getSecond() + 1;

                // prefix empty and append our to the end
                String[] a = new String[i];
                Arrays.fill(a, "''");

                PsiElement originalPosition = parameters.getOriginalPosition();
                if (originalPosition != null) {
                    String text = PsiElementUtils.trimQuote(originalPosition.getText());
                    if (StringUtils.isNotBlank(text)) {
                        a[a.length - 1] = "'" + text + "'";
                    }
                }

                String join = StringUtils.join(a, ", ");

                PhpNamedElement psiElement = pair.getFirst();
                if (psiElement instanceof Method) {
                    PhpClass containingClass = ((Method) psiElement).getContainingClass();
                    if (containingClass != null) {
                        @NotNull ParameterList phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(parameters.getPosition().getProject(), ParameterList.class, "" +
                            "<?php\n" +
                            "/** @var " + containingClass.getFQN() + " $____proxy____ */\n" +
                            "$____proxy____->" + psiElement.getName() + "(" + join + ");\n");

                        PsiElement[] parameters1 = phpPsiFromText.getParameters();
                        targets.add(parameters1[parameters1.length - 1]);
                    }
                } else if (psiElement instanceof com.jetbrains.php.lang.psi.elements.Function) {
                    @NotNull ParameterList phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(parameters.getPosition().getProject(), ParameterList.class, "" +
                        "<?php\n" +
                        "$____proxy____ = " + psiElement.getName() + "(" + join + ");\n");

                    PsiElement[] parameters1 = phpPsiFromText.getParameters();
                    targets.add(parameters1[parameters1.length - 1]);
                } else if (psiElement instanceof PhpClass) {
                    @NotNull ParameterList phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(parameters.getPosition().getProject(), ParameterList.class, "" +
                        "<?php\n" +
                        "$____proxy____ = new " + psiElement.getFQN() + "(" + join + ");\n");

                    PsiElement[] parameters1 = phpPsiFromText.getParameters();
                    targets.add(parameters1[parameters1.length - 1]);
                }
            }

            for (PsiElement target : targets) {
                PsiElement firstChild = target.getFirstChild();

                try {
                    CompletionService.getCompletionService().performCompletion(new CompletionParameters(
                        firstChild,
                        firstChild.getContainingFile(),
                        CompletionType.BASIC,
                        firstChild.getTextOffset(),
                        parameters.getInvocationCount(),
                        parameters.getEditor(),
                        parameters.getProcess()
                    ), completionResult -> result.addElement(completionResult.getLookupElement()));
                } catch (Throwable e) {
                    // catch all external issues
                    Symfony2ProjectComponent.getLogger().info("Twig proxy completion issue: " + e.getMessage());
                }

                for (PsiReference reference : target.getReferences()) {
                    for (Object variant : reference.getVariants()) {
                        if (variant instanceof LookupElement) {
                            result.addElement((LookupElement) variant);
                        }
                    }
                }
            }
        }

        @NotNull
        private Collection<Pair<PhpNamedElement, Integer>> getTargetFunctionCallWithParameterIndex(@NotNull Collection<PsiElement> filterGoTo, int wantParameter) {
            Collection<Pair<PhpNamedElement, Integer>> sources = new ArrayList<>();

            for (PsiElement psiElement : filterGoTo) {
                if (!(psiElement instanceof com.jetbrains.php.lang.psi.elements.Function)) {
                    continue;
                }

                Parameter[] parameters = ((com.jetbrains.php.lang.psi.elements.Function) psiElement).getParameters();

                // "needs_environment" remove
                // @TODO: its also possible based on extension parser; we known the value
                if (parameters.length > 0) {
                    boolean needStripFirstEnvParameter = parameters[0].getDeclaredType().getTypes().stream()
                        .anyMatch(s ->  s.equalsIgnoreCase("\\Twig\\Environment") || s.equalsIgnoreCase("\\Twig_Environment"));

                    if (needStripFirstEnvParameter) {
                        parameters = Arrays.copyOfRange(parameters, 1, parameters.length);
                    }
                }

                if (wantParameter <= parameters.length - 1) {
                    Parameter parameter = parameters[wantParameter];
                    sources.addAll(collectFunctions((com.jetbrains.php.lang.psi.elements.Function) psiElement, parameter).values());
                }
            }

            return sources;
        }

        @NotNull
        private static Map<String ,Pair<PhpNamedElement, Integer>> collectFunctions(@NotNull com.jetbrains.php.lang.psi.elements.Function functionScope, @NotNull Parameter parameter) {
            String text = parameter.getName();
            PsiElement[] psiElements = PsiTreeUtil.collectElements(functionScope, psiElement -> psiElement instanceof Variable && text.equals(((Variable) psiElement).getName()));

            Map<String ,Pair<PhpNamedElement, Integer>> targets = new HashMap<>();

            for (PsiElement psiElement : psiElements) {
                PsiElement variableContext = psiElement.getContext();
                if(!(variableContext instanceof ParameterList parameterList)) {
                    continue;
                }

                PsiElement context = parameterList.getContext();
                // @TODO: support functions also
                if (context instanceof MethodReference) {
                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if(currentIndex == null) {
                        continue;
                    }

                    MethodReferenceBag methodParameterReferenceBag = PhpElementsUtil.getMethodParameterReferenceBag(psiElement);
                    if (methodParameterReferenceBag == null) {
                        continue;
                    }

                    int index = methodParameterReferenceBag.getParameterBag().getIndex();
                    Collection<com.jetbrains.php.lang.psi.elements.Function> functions = PhpElementsUtil.getMethodReferenceMethods(methodParameterReferenceBag.getMethodReference());

                    for (com.jetbrains.php.lang.psi.elements.Function methodReferenceMethod : functions) {
                        String fqn = methodReferenceMethod.getFQN();
                        targets.put(fqn, new Pair<>(methodReferenceMethod, index));
                    }
                } else if (context instanceof NewExpression) {
                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if(currentIndex == null) {
                        continue;
                    }

                    for (PhpClass phpClass : PhpElementsUtil.getNewExpressionPhpClasses((NewExpression) context)) {
                        targets.put(phpClass.getFQN(), new Pair<>(phpClass, currentIndex.getIndex()));
                    }
                }
            }

            return targets;
        }
    }

    @NotNull
    private Collection<LookupElement> processVariables(@NotNull PsiElement psiElement, @NotNull Predicate<PhpType> filter, @NotNull Function<Map.Entry<String, Pair<String, LookupElement>>, String> map) {
        Project project = psiElement.getProject();

        Map<String, Pair<String, LookupElement>> arrays = new HashMap<>();
        for(Map.Entry<String, PsiVariable> entry: TwigTypeResolveUtil.collectScopeVariables(psiElement).entrySet()) {
            Collection<PhpClass> classFromPhpTypeSet = PhpElementsUtil.getClassFromPhpTypeSet(project, entry.getValue().getTypes());
            for (PhpClass phpClass : classFromPhpTypeSet) {
                for(Method method: phpClass.getMethods()) {
                    if(!(!method.getModifier().isPublic() || method.getName().startsWith("set") || method.getName().startsWith("__"))) {
                        if (filter.test(PhpIndex.getInstance(project).completeType(project, method.getType(), new HashSet<>()))) {
                            String propertyShortcutMethodName = TwigTypeResolveUtil.getPropertyShortcutMethodName(method);
                            arrays.put(entry.getKey() + "." + propertyShortcutMethodName, Pair.create(propertyShortcutMethodName, new PhpTwigMethodLookupElement(method)));
                        }
                    }
                }

                for(Field field: phpClass.getFields()) {
                    if(field.getModifier().isPublic()) {
                        if (filter.test(PhpIndex.getInstance(project).completeType(project, field.getType(), new HashSet<>()))) {
                            arrays.put(entry.getKey() + "." + field.getName(), Pair.create(field.getName(), new PhpTwigMethodLookupElement(field)));
                        }
                    }
                }
            }
        }

        Collection<LookupElement> items = new ArrayList<>();

        for (Map.Entry<String, Pair<String, LookupElement>> entry : arrays.entrySet()) {
            LookupElementPresentation lookupElementPresentation = new LookupElementPresentation();
            entry.getValue().getSecond().renderElement(lookupElementPresentation);

            Set<String> types = new HashSet<>();
            PsiElement typeElement = entry.getValue().getSecond().getPsiElement();
            if (typeElement instanceof PhpTypedElement) {
                types.addAll(((PhpTypedElement) typeElement).getType().getTypes());
            }

            LookupElementBuilder lookupElement = LookupElementBuilder.create(map.apply(entry))
                .withIcon(lookupElementPresentation.getIcon())
                .withStrikeoutness(lookupElementPresentation.isStrikeout())
                .withTypeText(StringUtils.stripStart(TwigTypeResolveUtil.getTypeDisplayName(project, types), "\\"));

            items.add(lookupElement);
        }

        return items;
    }
}

