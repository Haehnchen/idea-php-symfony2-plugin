package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * ReferencesSearch executor that finds Twig template usages using indexes.
 * <p>
 * Finds usages in:
 * - Twig {@code {% include %}}, {@code {% embed %}}, {@code {% import %}}, {@code {% from %}}, {@code {% form_theme %}} tags
 * - Twig {@code {% extends %}} tags
 * - PHP controller render() calls (via PhpTwigTemplateUsageStubIndex)
 * - Twig component usages (via TwigComponentUsageStubIndex)
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateReferencesSearchExecutor implements QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {

    @Override
    public boolean execute(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
        PsiElement target = queryParameters.getElementToSearch();

        if (!(target instanceof TwigFile targetFile)) {
            return true;
        }

        Project project = targetFile.getProject();

        ApplicationManager.getApplication().runReadAction(() -> {
            doSearch(targetFile, project, queryParameters, consumer);
        });

        return true;
    }

    private void doSearch(@NotNull TwigFile targetFile, @NotNull Project project, @NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
        VirtualFile virtualFile = targetFile.getVirtualFile();
        if (virtualFile == null) {
            return;
        }

        Set<String> templateNames = new HashSet<>(TwigUtil.getTemplateNamesForFile(project, virtualFile));
        if (templateNames.isEmpty()) {
            return;
        }

        FileBasedIndex index = FileBasedIndex.getInstance();
        SearchScope searchScope = queryParameters.getEffectiveSearchScope();
        GlobalSearchScope scope = searchScope instanceof GlobalSearchScope ? (GlobalSearchScope) searchScope : GlobalSearchScope.projectScope(project);

        PsiManager psiManager = PsiManager.getInstance(project);

        Set<VirtualFile> processedIncludeFiles = new HashSet<>();
        for (String templateName : templateNames) {
            String normalizedName = TwigUtil.normalizeTemplateName(templateName);

            // Twig include / embed / import / form_theme
            index.processValues(TwigIncludeStubIndex.KEY, normalizedName, null, (file, includeObj) -> {
                if (!processedIncludeFiles.add(file)) {
                    return true;
                }

                PsiFile psiFile = psiManager.findFile(file);
                if (!(psiFile instanceof TwigFile)) {
                    return true;
                }

                findIncludeReferences((TwigFile) psiFile, normalizedName, targetFile, consumer);

                return true;
            }, scope);

            // Twig extends
            index.processValues(TwigExtendsStubIndex.KEY, normalizedName, null, (file, v) -> {
                PsiFile psiFile = psiManager.findFile(file);

                if (!(psiFile instanceof TwigFile)) {
                    return true;
                }

                findExtendsReferences((TwigFile) psiFile, normalizedName, targetFile, consumer);
                return true;
            }, scope);

            // PHP controller render() / @Template usages — resolve to the exact string literal
            Set<Function> processedControllerFunctions = new HashSet<>();
            Set<PsiElement> processedPhpSourceElements = new HashSet<>();
            for (Function function : TwigUtil.getTwigFileMethodUsageOnIndex(project, Set.of(normalizedName))) {
                if (!processedControllerFunctions.add(function)) {
                    continue;
                }

                PhpMethodVariableResolveUtil.visitRenderTemplateFunctions(function, triple -> {
                    if (!normalizedName.equals(triple.getFirst())) {
                        return;
                    }

                    PsiElement sourceElement = resolveControllerSourceElement(triple, function);
                    if (sourceElement != null && processedPhpSourceElements.add(sourceElement)) {
                        consumer.process(new TwigTemplateUsageReference(
                            sourceElement,
                            targetFile,
                            new TextRange(0, sourceElement.getTextLength())
                        ));
                    }
                });
            }

            // Twig component usages
            Set<PsiElement> processedComponentElements = new HashSet<>();
            GlobalSearchScope twigScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, TwigFileType.INSTANCE);
            for (VirtualFile templateVFile : TwigUtil.getTemplateFiles(project, normalizedName)) {
                PsiFile psiFile = psiManager.findFile(templateVFile);
                if (!(psiFile instanceof TwigFile templateTwigFile)) {
                    continue;
                }

                Set<String> componentNames = UxUtil.getTemplateComponentNames(templateTwigFile);
                if (componentNames.isEmpty()) {
                    continue;
                }

                Set<String> normalizedComponentNames = new HashSet<>();
                for (String componentName : componentNames) {
                    String normalized = TwigComponentUsageStubIndex.normalizeComponentName(componentName);
                    if (normalized != null) {
                        normalizedComponentNames.add(normalized);
                    }
                }

                for (String normalizedComponentName : normalizedComponentNames) {
                    index.processValues(TwigComponentUsageStubIndex.KEY, normalizedComponentName, null, (usageFile, types) -> {
                        if (usageFile.equals(templateVFile)) {
                            return true; // skip self-reference
                        }

                        PsiFile usagePsiFile = psiManager.findFile(usageFile);
                        if (!(usagePsiFile instanceof TwigFile usageTwigFile)) {
                            return true;
                        }

                        for (PsiElement element : TwigComponentUsageStubIndex.getComponentUsages(usageTwigFile, componentNames)) {
                            if (processedComponentElements.add(element)) {
                                consumer.process(createComponentUsageReference(element, targetFile));
                            }
                        }

                        return true;
                    }, twigScope);
                }
            }
        }
    }

    /**
     * Resolves the best source PSI element for a controller render() usage.
     * <p>
     * For render() / renderView() calls: the {@code StringLiteralExpression} that is the first
     * parameter of the call (e.g. {@code $this->render('home/index.html.twig')}).
     * For complex parameter expressions: the {@code FunctionReference} (the call itself).
     * For {@code @Template} / {@code #[Template]} annotations without a call: the method identifier.
     */
    private static PsiElement resolveControllerSourceElement(@NotNull kotlin.Triple<String, PhpNamedElement, FunctionReference> triple, @NotNull Function function) {
        FunctionReference functionReference = triple.getThird();
        if (functionReference != null) {
            PsiElement[] params = functionReference.getParameters();
            if (params.length > 0 && params[0] instanceof StringLiteralExpression) {
                return params[0];
            }
            // Complex expression (ternary, variable…): navigate to the render() call itself
            return functionReference;
        }

        // @Template / #[Template] annotation — navigate to the method name identifier
        PsiElement nameIdentifier = triple.getSecond().getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier : function.getNameIdentifier();
    }

    private void findIncludeReferences(@NotNull TwigFile sourceFile, @NotNull String templateName, @NotNull TwigFile targetFile, @NotNull Processor<? super PsiReference> consumer) {
        TwigUtil.visitTemplateIncludes(sourceFile, templateInclude -> {
            String includeTemplateName = TwigUtil.normalizeTemplateName(templateInclude.getTemplateName());
            if (includeTemplateName.equals(templateName)) {
                PsiElement sourceElement = templateInclude.getPsiElement();
                PsiReference reference = createStringReference(sourceElement, targetFile, templateInclude.getTemplateName());
                if (reference != null) {
                    consumer.process(reference);
                }
            }
        });
    }

    private void findExtendsReferences(@NotNull TwigFile sourceFile, @NotNull String templateName, @NotNull TwigFile targetFile, @NotNull Processor<? super PsiReference> consumer) {
        TwigUtil.visitTemplateExtends(sourceFile, pair -> {
            String extendsTemplateName = TwigUtil.normalizeTemplateName(pair.getFirst());
            if (extendsTemplateName.equals(templateName)) {
                PsiElement sourceElement = pair.getSecond();
                if (sourceElement != null) {
                    PsiReference ref = createStringReference(sourceElement, targetFile, pair.getFirst());
                    if (ref != null) {
                        consumer.process(ref);
                    }
                }
            }
        });
    }

    private @Nullable PsiReference createStringReference(@NotNull PsiElement sourceElement, @NotNull TwigFile targetFile, @NotNull String templateNameText) {
        String elementText = sourceElement.getText();
        int startIndex = elementText.indexOf(templateNameText);

        if (startIndex < 0) {
            return null;
        }

        TextRange range = new TextRange(startIndex, startIndex + templateNameText.length());

        return new TwigTemplateUsageReference(sourceElement, targetFile, range);
    }

    private @NotNull PsiReference createComponentUsageReference(@NotNull PsiElement sourceElement, @NotNull TwigFile targetFile) {
        TextRange range = new TextRange(0, sourceElement.getTextLength());

        if (sourceElement instanceof XmlTag xmlTag) {
            String prefix = xmlTag.getNamespacePrefix();
            String localName = xmlTag.getLocalName();
            String tagName = prefix.isBlank() ? localName : prefix + ":" + localName;

            if (!tagName.isBlank()) {
                String text = sourceElement.getText();
                int start = text.indexOf("<" + tagName);
                if (start >= 0) {
                    start += 1; // skip "<"
                    range = TextRange.from(start, tagName.length());
                } else {
                    int fallback = text.indexOf(tagName);
                    if (fallback >= 0) {
                        range = TextRange.from(fallback, tagName.length());
                    }
                }
            }
        }

        return new TwigTemplateUsageReference(sourceElement, targetFile, range);
    }
}
