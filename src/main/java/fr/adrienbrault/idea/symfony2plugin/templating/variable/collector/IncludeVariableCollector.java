package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
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
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
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

    /**
     * Child variable plus optional parent source path, e.g. product from item.product.
     */
    private record IncludeArgument(@NotNull String name, @NotNull List<String> sourcePath) {}

    /**
     * Parsed include context: explicit arguments and whether parent variables are inherited.
     */
    private record IncludeContext(@NotNull Collection<IncludeArgument> arguments, boolean withParentContext) {}

    @Override
    public void collectPsiVariables(@NotNull final TwigFileVariableCollectorParameter parameter, @NotNull final Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getElement().getContainingFile();
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

        IncludeContext includeContext = resolveIncludeContext(iElementType, tag, templatePsiName);

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
        for (IncludeArgument argument: includeContext.arguments()) {
            PsiVariable variable = resolveIncludeArgumentVariable(templatePsiName.getProject(), parentScope, argument.sourcePath());
            variables.put(argument.name(), variable != null ? variable : new PsiVariable());
        }
    }

    /**
     * Resolves include context rules, e.g. parent context plus {foo: bar}, or isolated with only/with_context.
     */
    @NotNull
    private static IncludeContext resolveIncludeContext(@NotNull IElementType iElementType, @NotNull PsiElement tag, @NotNull PsiElement templatePsiName) {
        if (iElementType == TwigElementTypes.INCLUDE_TAG || iElementType == TwigElementTypes.EMBED_TAG) {
            return new IncludeContext(collectTagIncludeArguments(tag), !hasOnlyKeyword(tag));
        }

        if (iElementType == TwigTokenTypes.IDENTIFIER) {
            return collectFunctionIncludeContext(templatePsiName);
        }

        return new IncludeContext(Collections.emptyList(), true);
    }

    /**
     * Detects tag-style isolation, e.g. {% include 'card.html.twig' only %}.
     */
    private static boolean hasOnlyKeyword(@NotNull PsiElement tag) {
        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigTokenTypes.ONLY_KEYWORD) || "only".equals(child.getText())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads arguments from tag-style includes, e.g.
     * {% include 'card.html.twig' with {product: item} only %}
     */
    @NotNull
    private static Collection<IncludeArgument> collectTagIncludeArguments(@NotNull PsiElement tag) {
        PsiElement withElement = null;
        for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isTwigElementType(child, TwigTokenTypes.WITH_KEYWORD) || "with".equals(child.getText())) {
                withElement = child;
                break;
            }
        }

        if (withElement == null) {
            return Collections.emptyList();
        }

        PsiElement hashLiteral = nextHashLiteral(withElement);
        return hashLiteral != null ? collectHashArguments(hashLiteral) : Collections.emptyList();
    }

    /**
     * Reads arguments from function-style includes, e.g.
     * {{ include('card.html.twig', {product: item}, with_context: false) }}
     */
    @NotNull
    private static IncludeContext collectFunctionIncludeContext(@NotNull PsiElement templatePsiName) {
        PsiElement functionCall = PsiElementUtils.getParentOfType(templatePsiName, TwigElementTypes.FUNCTION_CALL);
        if (functionCall == null) {
            return new IncludeContext(Collections.emptyList(), true);
        }

        Collection<IncludeArgument> includeArguments = new ArrayList<>();
        boolean withParentContext = true;
        List<List<PsiElement>> arguments = splitFunctionArguments(functionCall);

        for (int i = 1; i < arguments.size(); i++) {
            List<PsiElement> argument = arguments.get(i);

            if (isWithContextFalseArgument(argument)) {
                withParentContext = false;
                continue;
            }

            PsiElement hashLiteral = findHashLiteral(argument);
            if (hashLiteral != null) {
                includeArguments.addAll(collectHashArguments(hashLiteral));
            }
        }

        return new IncludeContext(includeArguments, withParentContext);
    }

    /**
     * Splits only direct include() arguments; hash literals stay whole PSI nodes.
     *
     * Example: include('card.html.twig', {title: 'Hi ' ~ item.name, options: {active: true}}, with_context: false)
     * becomes [template], [hash literal], [with_context, :, false].
     */
    @NotNull
    private static List<List<PsiElement>> splitFunctionArguments(@NotNull PsiElement functionCall) {
        List<List<PsiElement>> arguments = new ArrayList<>();
        List<PsiElement> current = new ArrayList<>();
        boolean insideArguments = false;

        for (PsiElement child = functionCall.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isWhitespace(child)) {
                continue;
            }

            if (!insideArguments) {
                // Ignore the function name and start collecting after the opening parenthesis.
                if (isTwigElementType(child, TwigTokenTypes.LBRACE)) {
                    insideArguments = true;
                }

                continue;
            }

            if (isTwigElementType(child, TwigTokenTypes.RBRACE)) {
                if (!current.isEmpty()) {
                    arguments.add(current);
                }

                break;
            }

            // Twig hash arguments are single LITERAL nodes, so this comma is a function-level separator.
            if (isTwigElementType(child, TwigTokenTypes.COMMA)) {
                arguments.add(current);
                current = new ArrayList<>();
                continue;
            }

            current.add(child);
        }

        return arguments;
    }

    /**
     * Matches include() context isolation, e.g.
     * {{ include('card.html.twig', {product: item}, with_context: false) }}
     *
     * Example: with_context: false as [name, separator, value].
     */
    private static boolean isWithContextFalseArgument(@NotNull List<PsiElement> argument) {
        if (argument.size() < 3) {
            return false;
        }

        PsiElement name = argument.get(0);
        if (!"with_context".equals(name.getText())) {
            return false;
        }

        PsiElement separator = argument.get(1);
        if (!isTwigElementType(separator, TwigTokenTypes.COLON) && !isTwigElementType(separator, TwigTokenTypes.EQ)) {
            return false;
        }

        return "false".equals(argument.get(2).getText());
    }

    /**
     * Finds the next direct Twig hash literal after a token, e.g. after the with keyword.
     */
    @Nullable
    private static PsiElement nextHashLiteral(@NotNull PsiElement element) {
        PsiElement child = nextMeaningfulSibling(element);
        return child != null && isHashLiteral(child) ? child : null;
    }

    /**
     * Finds a Twig hash literal inside one already split function argument.
     */
    @Nullable
    private static PsiElement findHashLiteral(@NotNull List<PsiElement> argument) {
        for (PsiElement element: argument) {
            if (isHashLiteral(element)) {
                return element;
            }
        }

        return null;
    }

    /**
     * Checks for Twig hash literals represented as LITERAL nodes, e.g. {title: 'Example'}.
     */
    private static boolean isHashLiteral(@NotNull PsiElement element) {
        return isTwigElementType(element, TwigElementTypes.LITERAL) &&
            element.getFirstChild() != null &&
            isTwigElementType(element.getFirstChild(), TwigTokenTypes.LBRACE_CURL);
    }

    /**
     * Reads Twig hash parameters, e.g. {product: item, title: 'Example'}.
     */
    @NotNull
    private static Collection<IncludeArgument> collectHashArguments(@NotNull PsiElement hashLiteral) {
        List<PsiElement> children = getMeaningfulChildren(hashLiteral);
        Collection<IncludeArgument> arguments = new ArrayList<>();

        for (int i = 0; i < children.size(); i++) {
            PsiElement child = children.get(i);

            if (isTwigElementType(child, TwigTokenTypes.LBRACE_CURL) || isTwigElementType(child, TwigTokenTypes.COMMA)) {
                continue;
            }

            KeyResult keyResult = readHashKey(children, i);
            if (keyResult == null || keyResult.nextIndex() >= children.size() || !isTwigElementType(children.get(keyResult.nextIndex()), TwigTokenTypes.COLON)) {
                continue;
            }

            int valueStart = keyResult.nextIndex() + 1;
            int valueEnd = valueStart;
            // Keep the full value PSI slice so complex/literal values still create an untyped variable.
            while (valueEnd < children.size() && !isTwigElementType(children.get(valueEnd), TwigTokenTypes.COMMA) && !isTwigElementType(children.get(valueEnd), TwigTokenTypes.RBRACE_CURL)) {
                valueEnd++;
            }

            arguments.add(new IncludeArgument(keyResult.name(), getSourcePath(children.subList(valueStart, valueEnd))));
            i = valueEnd;
        }

        return arguments;
    }

    /**
     * Reads unquoted and quoted hash keys, e.g. product, 'product', or "product".
     */
    @Nullable
    private static KeyResult readHashKey(@NotNull List<PsiElement> children, int index) {
        PsiElement child = children.get(index);

        if (isTwigElementType(child, TwigTokenTypes.IDENTIFIER) || isTwigElementType(child, TwigTokenTypes.STRING_TEXT)) {
            return new KeyResult(child.getText(), index + 1);
        }

        if (isSimpleVariableReference(child)) {
            return new KeyResult(child.getText(), index + 1);
        }

        if (isTwigElementType(child, TwigTokenTypes.SINGLE_QUOTE) || isTwigElementType(child, TwigTokenTypes.DOUBLE_QUOTE)) {
            if (index + 2 < children.size() && isTwigElementType(children.get(index + 1), TwigTokenTypes.STRING_TEXT)) {
                PsiElement closingQuote = children.get(index + 2);
                if (closingQuote.getNode().getElementType() == child.getNode().getElementType()) {
                    return new KeyResult(children.get(index + 1).getText(), index + 3);
                }
            }
        }

        return null;
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
     * Returns direct non-whitespace children for predictable PSI token scanning.
     */
    @NotNull
    private static List<PsiElement> getMeaningfulChildren(@NotNull PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        for (PsiElement child = element.getFirstChild(); child != null; child = nextMeaningfulSibling(child)) {
            if (!isWhitespace(child)) {
                children.add(child);
            }
        }

        return children;
    }

    /**
     * Gets the next sibling while skipping normal and Twig-specific whitespace.
     */
    @Nullable
    private static PsiElement nextMeaningfulSibling(@NotNull PsiElement element) {
        PsiElement next = PhpPsiUtil.getNextSiblingIgnoreWhitespace(element, true);
        while (next != null && isWhitespace(next)) {
            next = PhpPsiUtil.getNextSiblingIgnoreWhitespace(next, true);
        }

        return next;
    }

    /**
     * Handles both IntelliJ whitespace PSI and Twig's own whitespace token.
     */
    private static boolean isWhitespace(@NotNull PsiElement element) {
        return element instanceof PsiWhiteSpace || isTwigElementType(element, TwigTokenTypes.WHITE_SPACE);
    }

    /**
     * Small guard around direct Twig element type comparisons.
     */
    private static boolean isTwigElementType(@NotNull PsiElement element, @NotNull IElementType elementType) {
        return element.getNode().getElementType() == elementType;
    }

    /**
     * Parsed hash key and index of the next PSI token after it.
     */
    private record KeyResult(@NotNull String name, int nextIndex) {}

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
    }
}
