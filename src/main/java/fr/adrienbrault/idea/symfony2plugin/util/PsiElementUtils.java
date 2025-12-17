package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementUtils {
    public static String getMethodParameter(PsiElement parameter) {

        if (!(parameter instanceof StringLiteralExpression stringLiteralExpression)) {
            return null;
        }

        String stringValue = stringLiteralExpression.getText();
        String value = stringValue.substring(stringLiteralExpression.getValueRange().getStartOffset(), stringLiteralExpression.getValueRange().getEndOffset());

        return removeIdeaRuleHack(value);
    }

    public static String removeIdeaRuleHack(String value) {
        // wtf: ???
        // looks like current cursor position is marked :)
        return value.replace("IntellijIdeaRulezzz", "").replace("IntellijIdeaRulezzz ", "").trim();
    }

    @Nullable
    public static String getMethodParameterAt(@NotNull MethodReference methodReference, int index) {

        ParameterList parameterList = methodReference.getParameterList();
        if(parameterList == null) {
            return null;
        }

        return getMethodParameterAt(parameterList, index);
    }

    @Nullable
    public static String getMethodParameterAt(@NotNull ParameterList parameterList, int index) {
        PsiElement[] parameters = parameterList.getParameters();

        if(parameters.length < index + 1) {
            return null;
        }

        return getMethodParameter(parameters[index]);
    }

    @Nullable
    public static PsiElement getMethodParameterPsiElementAt(@Nullable MethodReference methodReference, int index) {

        if(methodReference == null) {
            return null;
        }

        return getMethodParameterPsiElementAt(methodReference.getParameterList(), index);
    }

    @Nullable
    public static PsiElement getMethodParameterPsiElementAt(@Nullable ParameterList parameterList, int index) {

        if(parameterList == null) {
            return null;
        }

        PsiElement[] parameters = parameterList.getParameters();

        if(parameters.length < index + 1) {
            return null;
        }

        return parameters[index];
    }

    @Nullable
    public static ParameterBag getCurrentParameterIndex(Parameter parameter) {

        PsiElement parameterList = parameter.getContext();
        if(!(parameterList instanceof ParameterList)) {
            return null;
        }

        return getCurrentParameterIndex(((ParameterList) parameterList).getParameters(), parameter);
    }

    @Nullable
    public static ParameterBag getCurrentParameterIndex(PsiElement[] parameters, PsiElement parameter) {
        int i;
        for(i = 0; i < parameters.length; i = i + 1) {
            if(parameters[i].equals(parameter)) {
                return new ParameterBag(i, parameters[i]);
            }
        }

        return null;
    }

    @Nullable
    public static ParameterBag getCurrentParameterIndex(PsiElement psiElement) {
        PsiElement parameterList = psiElement.getContext();
        if (!(parameterList instanceof ParameterList)) {
            return null;
        }

        return getCurrentParameterIndex(((ParameterList) parameterList).getParameters(), psiElement);
    }

    public static boolean isCurrentParameter(@NotNull PsiElement psiElement, @NotNull String namedParameter, int index) {
        PsiElement parameterList = psiElement.getContext();
        if (!(parameterList instanceof ParameterList)) {
            return false;
        }

        ASTNode previousNonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(psiElement.getNode());
        if (previousNonWhitespaceSibling != null && previousNonWhitespaceSibling.getElementType() == PhpTokenTypes.opCOLON) {
            ASTNode previousNonWhitespaceSibling1 = FormatterUtil.getPreviousNonWhitespaceSibling(previousNonWhitespaceSibling);
            if (previousNonWhitespaceSibling1 != null && previousNonWhitespaceSibling1.getElementType() == PhpTokenTypes.IDENTIFIER && namedParameter.equals(previousNonWhitespaceSibling1.getText())) {
                return true;
            }
        }

        ParameterBag currentParameterIndex = getCurrentParameterIndex(((ParameterList) parameterList).getParameters(), psiElement);
        return currentParameterIndex != null && currentParameterIndex.getIndex() == index;
    }

    public static int getParameterIndexValue(@Nullable PsiElement parameterListChild) {

        if(parameterListChild == null) {
            return -1;
        }

        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(parameterListChild);
        if(parameterBag == null) {
            return -1;
        }

        return parameterBag.getIndex();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getNextSiblingOfType(@Nullable PsiElement sibling, ElementPattern<PsiElement> pattern) {
        if (sibling == null) return null;
        for (PsiElement child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return (T)child;
            }
        }
        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> Collection<T> getNextSiblingOfTypes(@Nullable PsiElement sibling, ElementPattern<PsiElement> pattern) {
        if (sibling == null) return Collections.emptySet();

        Collection<T> foo = new ArrayList<>();
        for (PsiElement child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                foo.add((T)child);
            }
        }
        return foo;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> Collection<T> getNextSiblingOfTypes(@Nullable PsiElement sibling, Class<T> pattern) {
        if (sibling == null) return Collections.emptySet();

        Collection<T> results = new ArrayList<>();
        for (PsiElement child = sibling.getNextSibling(); child != null; child = child.getNextSibling()) {
            if (pattern.isInstance(child)) {
                //noinspection unchecked
                results.add((T)child);
            }
        }

        return results;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getPrevSiblingOfType(@Nullable PsiElement sibling, ElementPattern<T> pattern) {
        if (sibling == null) return null;
        for (PsiElement child = sibling.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return (T)child;
            }
        }
        return null;
    }

    public static void getPrevSiblingOnCallback(@Nullable PsiElement sibling, Processor<PsiElement> processor) {
        if (sibling == null) return;

        for (PsiElement child = sibling.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if (!processor.process(child)) {
                return;
            }
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T getChildrenOfType(@Nullable PsiElement element, ElementPattern<T> pattern) {
        if (element == null) return null;

        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return (T)child;
            }
        }

        return null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> Collection<T> getChildrenOfTypeAsList(@Nullable PsiElement element, ElementPattern<T> pattern) {

        Collection<T> collection = new ArrayList<>();

        if (element == null) {
            return collection;
        }

        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (pattern.accepts(child)) {
                //noinspection unchecked
                collection.add((T)child);
            }
        }

        return collection;
    }

    @Nullable
    @Deprecated
    public static MethodReference getMethodReferenceWithFirstStringParameter(PsiElement psiElement) {
        if(!PlatformPatterns.psiElement()
            .withParent(StringLiteralExpression.class).inside(ParameterList.class)
            .withLanguage(PhpLanguage.INSTANCE).accepts(psiElement)) {

            return null;
        }

        ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
        if (parameterList == null) {
            return null;
        }

        if (!(parameterList.getContext() instanceof MethodReference)) {
            return null;
        }

        return (MethodReference) parameterList.getContext();
    }

    @Nullable
    public static MethodReference getMethodReferenceWithFirstStringParameter(@NotNull StringLiteralExpression stringLiteralExpression) {
        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(stringLiteralExpression);
        if(parameterBag == null || parameterBag.getIndex() != 0) {
            return null;
        }

        PsiElement parameterList = stringLiteralExpression.getParent();
        if (!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement methodReference = parameterList.getParent();
        if (!(methodReference instanceof MethodReference)) {
            return null;
        }

        return (MethodReference) parameterList.getParent();
    }

    public static String trimQuote(String text) {
        return text.replaceAll("^\"|\"$|\'|\'$", "");
    }

    public static String getText(@NotNull PsiElement psiElement) {
        return trimQuote(psiElement.getText());
    }

    @Nullable
    public static PsiFile virtualFileToPsiFile(Project project, VirtualFile virtualFile) {
        if(virtualFile == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    @Nullable
    public static PsiElement getParentOfTypeFirstChild(@Nullable PsiElement element, @NotNull Class aClass) {
        if (element == null) return null;

        PsiElement lastElement = null;
        while (element != null) {

            if (aClass.isInstance(element)) {
                return lastElement;
            }

            if (element instanceof PsiFile) {
                return null;
            }

            lastElement = element;
            element = element.getParent();
        }

        return null;
    }

    @Nullable
    public static PsiElement getParentOfType(@Nullable PsiElement element, @NotNull IElementType iElementType) {
        if (element == null) return null;

        while (element != null) {
            IElementType elementType = element.getNode().getElementType();
            if (elementType == iElementType) {
                return element;
            }

            if (element instanceof PsiFile) {
                return null;
            }

            element = element.getParent();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> List<T> getPrevSiblingsOfType(@Nullable PsiElement sibling, ElementPattern<T> pattern) {

        List<T> elements = new ArrayList<>();

        if (sibling == null) return null;
        for (PsiElement child = sibling.getPrevSibling(); child != null; child = child.getPrevSibling()) {
            if (pattern.accepts(child)) {
                // noinspection unchecked
                elements.add((T)child);
            }
        }
        return elements;
    }

    @Nullable
    public static PsiElement getNextSiblingAndSkip(@NotNull PsiElement psiElement, @NotNull IElementType find, @NotNull IElementType... skip) {
        for (PsiElement child = psiElement.getNextSibling(); child != null; child = child.getNextSibling()) {
            if(child.getNode().getElementType() == find) {
                return child;
            }

            if(child instanceof PsiWhiteSpace) {
                continue;
            }

            if(!Arrays.asList(skip).contains(child.getNode().getElementType())) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    public static String getStringBeforeCursor(StringLiteralExpression literal, int cursorOffset) {
        int cursorOffsetClean = cursorOffset - literal.getTextOffset() - 1;

        // stop here; we dont have a string before current position
        if(cursorOffsetClean < 1) {
            return null;
        }

        String content = literal.getContents();
        return content.length() >= cursorOffsetClean ? content.substring(0, cursorOffsetClean) : null;
    }

    @Nullable
    public static PsiElement getTextLeafElementFromStringLiteralExpression(@NotNull StringLiteralExpression psiElement) {
        return Arrays.stream(YamlHelper.getChildrenFix(psiElement))
            .filter(p -> p.getNode().getElementType() == PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE || p.getNode().getElementType() == PhpTokenTypes.STRING_LITERAL)
            .findFirst()
            .orElse(null);
    }

    @NotNull
    public static Collection<PsiFile> convertVirtualFilesToPsiFiles(@NotNull Project project, @NotNull Collection<VirtualFile> files) {

        Collection<PsiFile> psiFiles = new HashSet<>();

        PsiManager psiManager = null;
        for (VirtualFile file : files) {

            if(psiManager == null) {
                psiManager = PsiManager.getInstance(project);
            }

            PsiFile psiFile = psiManager.findFile(file);
            if(psiFile != null) {
                psiFiles.add(psiFile);
            }
        }

        return psiFiles;
    }

}
