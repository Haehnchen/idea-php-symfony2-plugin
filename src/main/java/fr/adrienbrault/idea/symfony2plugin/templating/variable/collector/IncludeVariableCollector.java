package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigIncludeContextParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IncludeVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collectPsiVariables(@NotNull final TwigFileVariableCollectorParameter parameter, @NotNull final Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getContainingFile();
        if (!(psiFile instanceof TwigFile) || PsiTreeUtil.getChildOfType(psiFile, TwigExtendsTag.class) != null) {
            return;
        }

        Collection<VirtualFile> files = getImplements((TwigFile) psiFile);
        if (files.isEmpty()) {
            return;
        }

        for (VirtualFile virtualFile: files) {
            PsiFile twigFile = PsiManager.getInstance(parameter.getProject()).findFile(virtualFile);
            if (!(twigFile instanceof TwigFile)) {
                continue;
            }

            twigFile.acceptChildren(new MyPsiRecursiveElementWalkingVisitor(psiFile, variables, parameter));
        }
    }

    /**
     * Adds parent scope and explicit include parameters for the matched include target.
     */
    private void collectIncludeContextVars(IElementType iElementType, PsiElement tag, PsiElement templatePsiName, Map<String, PsiVariable> variables, Set<VirtualFile> visitedFiles) {

        TwigIncludeContextParser.IncludeContext includeContext = resolveIncludeContext(iElementType, tag, templatePsiName);

        // we dont need to collect foreign file variables
        if (!includeContext.withParentContext() && includeContext.arguments().isEmpty()) {
           return;
        }

        Map<String, PsiVariable> parentScope = TwigTypeResolveUtil.collectScopeVariables(templatePsiName, visitedFiles);

        // add context vars
        if (includeContext.withParentContext()) {
            variables.putAll(parentScope);
        }

        // add explicit include parameters; literal values are still visible as untyped variables
        for (TwigIncludeContextParser.IncludeArgument argument: includeContext.arguments()) {
            PsiVariable variable = resolveIncludeArgumentVariable(templatePsiName.getProject(), parentScope, getSourcePath(argument.valueElements()));
            variables.put(argument.name(), variable != null ? variable : new PsiVariable());
        }
    }

    /**
     * Adds native-shaped with/only context for extension-provided custom include/embed tags.
     */
    private void collectExternalIncludeContextVars(PsiElement tag, Map<String, PsiVariable> variables, Set<VirtualFile> visitedFiles) {
        TwigIncludeContextParser.IncludeContext includeContext = TwigIncludeContextParser.resolveTagIncludeContext(tag);

        if (!includeContext.withParentContext() && includeContext.arguments().isEmpty()) {
            return;
        }

        Map<String, PsiVariable> parentScope = TwigTypeResolveUtil.collectScopeVariables(tag, visitedFiles);

        if (includeContext.withParentContext()) {
            variables.putAll(parentScope);
        }

        for (TwigIncludeContextParser.IncludeArgument argument: includeContext.arguments()) {
            PsiVariable variable = resolveIncludeArgumentVariable(tag.getProject(), parentScope, getSourcePath(argument.valueElements()));
            variables.put(argument.name(), variable != null ? variable : new PsiVariable());
        }
    }

    /**
     * Resolves include context rules, e.g. parent context plus {foo: bar}, or isolated with only/with_context.
     */
    @NotNull
    private static TwigIncludeContextParser.IncludeContext resolveIncludeContext(@NotNull IElementType iElementType, @NotNull PsiElement tag, @NotNull PsiElement templatePsiName) {
        if (iElementType == TwigElementTypes.INCLUDE_TAG || iElementType == TwigElementTypes.EMBED_TAG) {
            return TwigIncludeContextParser.resolveTagIncludeContext(tag);
        }

        if (iElementType == TwigTokenTypes.IDENTIFIER) {
            return TwigIncludeContextParser.resolveFunctionIncludeContext(templatePsiName);
        }

        return new TwigIncludeContextParser.IncludeContext(Collections.emptyList(), true);
    }

    /**
     * Extracts simple type source paths, e.g. item or item.product.
     */
    @NotNull
    private static List<String> getSourcePath(@NotNull List<PsiElement> valueElements) {
        if (valueElements.size() != 1) {
            return Collections.emptyList();
        }

        PsiElement value = valueElements.getFirst();
        if (isSimpleVariableReference(value)) {
            return Collections.singletonList(value.getText());
        }

        if (isTwigElementType(value, TwigElementTypes.FIELD_REFERENCE)) {
            List<String> path = new ArrayList<>();
            collectReferencePath(value, path);
            return path.size() > 1 ? path : Collections.emptyList();
        }

        return Collections.emptyList();
    }

    /**
     * Checks for a direct variable value, e.g. item in {product: item}.
     */
    private static boolean isSimpleVariableReference(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigElementTypes.VARIABLE_REFERENCE) && StringUtils.isNotBlank(element.getText());
    }

    /**
     * Flattens Twig reference PSI into a path, e.g. item.product into [item, product].
     */
    private static void collectReferencePath(@NotNull PsiElement element, @NotNull List<String> path) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigElementTypes.VARIABLE_REFERENCE)) {
                if (StringUtils.isNotBlank(child.getText())) {
                    path.add(child.getText());
                }
                continue;
            }

            if (isTwigElementType(child, TwigElementTypes.FIELD_REFERENCE)) {
                collectReferencePath(child, path);
                continue;
            }

            if (isTwigElementType(child, TwigTokenTypes.IDENTIFIER) || isTwigElementType(child, TwigTokenTypes.STRING_TEXT)) {
                if (StringUtils.isNotBlank(child.getText())) {
                    path.add(child.getText());
                }
            }
        }
    }

    /**
     * Resolves an include argument against parent scope, e.g. product: item.product.
     */
    @Nullable
    private static PsiVariable resolveIncludeArgumentVariable(@NotNull Project project, @NotNull Map<String, PsiVariable> parentScope, @NotNull List<String> sourcePath) {
        if (sourcePath.isEmpty()) {
            return null;
        }

        PsiVariable rootVariable = parentScope.get(sourcePath.getFirst());
        if (rootVariable == null) {
            return null;
        }

        if (sourcePath.size() == 1) {
            return rootVariable;
        }

        Set<String> types = new HashSet<>(rootVariable.getTypes());
        for (int i = 1; i < sourcePath.size(); i++) {
            Set<String> resolvedTypes = new HashSet<>();
            // Walk the property path using the same public getter/field shortcuts as Twig completion.
            for (PhpClass phpClass: PhpElementsUtil.getClassFromPhpTypeSet(project, types)) {
                for (PhpNamedElement target: TwigTypeResolveUtil.getTwigPhpNameTargets(phpClass, sourcePath.get(i))) {
                    resolvedTypes.addAll(target.getType().filterPrimitives().getTypes());
                }
            }

            if (resolvedTypes.isEmpty()) {
                return null;
            }

            types = resolvedTypes;
        }

        return new PsiVariable(types);
    }

    /**
     * Small guard around direct Twig element type comparisons.
     */
    private static boolean isTwigElementType(@NotNull PsiElement element, @NotNull IElementType elementType) {
        return element.getNode().getElementType() == elementType;
    }

    private Collection<VirtualFile> getImplements(TwigFile twigFile) {
        final Set<VirtualFile> targets = new HashSet<>();

        for (String templateName: TwigUtil.getTemplateNamesForFile(twigFile)) {
            FileBasedIndex.getInstance().getFilesWithKey(TwigIncludeStubIndex.KEY, new HashSet<>(Collections.singletonList(templateName)), virtualFile -> {
                targets.add(virtualFile);
                return true;
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(twigFile.getProject()), TwigFileType.INSTANCE));
        }

        return targets;
    }

    private class MyPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        @NotNull
        private final PsiFile psiFile;

        @NotNull
        private final Map<String, PsiVariable> variables;

        @NotNull
        private final TwigFileVariableCollectorParameter parameter;

        private ElementPattern<PsiElement> includeFunctionPattern;

        private ElementPattern<PsiElement> getIncludeFunctionPattern() {
            return includeFunctionPattern != null ? includeFunctionPattern : (includeFunctionPattern = TwigPattern.getPrintBlockOrTagFunctionPattern("include"));
        }

        private MyPsiRecursiveElementWalkingVisitor(@NotNull PsiFile psiFile, @NotNull Map<String, PsiVariable> variables, @NotNull TwigFileVariableCollectorParameter parameter) {
            this.psiFile = psiFile;
            this.variables = variables;
            this.parameter = parameter;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {

            // {% include 'template.html' %}
            if (element instanceof TwigTagWithFileReference && element.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                for (String templateName: TwigUtil.getIncludeTagStrings((TwigTagWithFileReference) element)) {
                    collectContextVars(TwigElementTypes.INCLUDE_TAG, element, element, templateName);
                }
            }

            if (element instanceof TwigCompositeElement) {
                // {{ include('template.html') }}
                PsiElement includeTag = PsiElementUtils.getChildrenOfType(element, getIncludeFunctionPattern());
                if (includeTag != null) {
                    collectContextVars(TwigTokenTypes.IDENTIFIER, element, includeTag);
                }

                // {% embed "foo.html.twig"
                PsiElement embedTag = PsiElementUtils.getChildrenOfType(element, TwigPattern.getEmbedPattern());
                if (embedTag != null) {
                    collectContextVars(TwigElementTypes.EMBED_TAG, element, embedTag);
                }

                collectExternalContextVars(element);
            }

            super.visitElement(element);
        }

        private void collectContextVars(IElementType iElementType, @NotNull PsiElement element, @NotNull PsiElement includeTag) {
            collectContextVars(iElementType, element, includeTag, includeTag.getText());
        }

        private void collectContextVars(IElementType iElementType, @NotNull PsiElement element, @NotNull PsiElement contextElement, @NotNull String templateName) {
            if (StringUtils.isNotBlank(templateName)) {
                for (PsiFile templateFile: TwigUtil.getTemplatePsiElements(element.getProject(), templateName)) {
                    if (templateFile.equals(psiFile)) {
                        collectIncludeContextVars(iElementType, element, contextElement, variables, parameter.getVisitedFiles());
                    }
                }
            }
        }

        private void collectExternalContextVars(@NotNull PsiElement element) {
            for (TwigFileUsage extension : TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
                if (extension.isIncludeTemplate(element)) {
                    collectExternalContextVars(element, extension.getIncludeTemplate(element));
                }

                if (extension.isEmbedTemplate(element)) {
                    collectExternalContextVars(element, extension.getEmbedTemplate(element));
                }
            }
        }

        private void collectExternalContextVars(@NotNull PsiElement element, @NotNull Collection<String> templateNames) {
            for (String templateName : templateNames) {
                if (StringUtils.isBlank(templateName)) {
                    continue;
                }

                for (PsiFile templateFile : TwigUtil.getTemplatePsiElements(element.getProject(), templateName)) {
                    if (templateFile.equals(psiFile)) {
                        collectExternalIncludeContextVars(element, variables, parameter.getVisitedFiles());
                    }
                }
            }
        }
    }
}
