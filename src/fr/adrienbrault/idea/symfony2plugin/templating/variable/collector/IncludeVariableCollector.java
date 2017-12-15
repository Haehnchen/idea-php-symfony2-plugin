package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IncludeVariableCollector implements TwigFileVariableCollector, TwigFileVariableCollector.TwigFileVariableCollectorExt {

    @Override
    public void collectVars(final TwigFileVariableCollectorParameter parameter, final Map<String, PsiVariable> variables) {

        final PsiFile psiFile = parameter.getElement().getContainingFile();
        if(!(psiFile instanceof TwigFile) || PsiTreeUtil.getChildOfType(psiFile, TwigExtendsTag.class) != null) {
            return;
        }

        Collection<VirtualFile> files = getImplements((TwigFile) psiFile);
        if(files.size() == 0) {
            return;
        }

        for(VirtualFile virtualFile: files) {

            PsiFile twigFile = PsiManager.getInstance(parameter.getProject()).findFile(virtualFile);
            if(!(twigFile instanceof TwigFile)) {
                continue;
            }

            twigFile.acceptChildren(new MyPsiRecursiveElementWalkingVisitor(psiFile, variables, parameter));
        }

    }

    private void collectIncludeContextVars(IElementType iElementType, PsiElement tag, PsiElement templatePsiName, Map<String, PsiVariable> variables, Set<VirtualFile> visitedFiles) {

        boolean addContextVar = true;
        Map<String, String> varAliasMap = new HashMap<>();

        if(iElementType == TwigElementTypes.INCLUDE_TAG || iElementType == TwigElementTypes.EMBED_TAG) {

            // {% include 'template.html' with {'foo': 'bar'} only %}
            // {% embed "template.html.twig" with {'foo': 'bar'} only %}

            PsiElement onlyElement = PsiElementUtils.getChildrenOfType(tag, TwigHelper.getIncludeOnlyPattern());
            if(onlyElement != null) {
                addContextVar = false;
            }

            varAliasMap = getIncludeWithVarNames(tag.getText());

        } else if(iElementType == TwigTokenTypes.IDENTIFIER) {

            // {{ include('template.html.twig', {'foo2': foo}, with_context = false) }}
            // not nice but its working :)

            // strip all whitespace psi elements
            String text = tag.getText();
            text = text.replaceAll("\\r|\\n|\\s+", "");

            String regex = "include\\((['|\"].*['|\"],(.*))\\)";
            Matcher matcher = Pattern.compile(regex).matcher(text);

            if (matcher.find()) {
                String[] group = matcher.group(1).split(",");

                if(group.length > 1) {

                    // json alias map: {'foo2': foo}
                    if(group[1].startsWith("{")) {
                        varAliasMap = getVariableAliasMap(group[1]);
                    }

                    // try to find context in one of the parameter:
                    // include('template.html', with_context = false)
                    // include('template.html', {foo: 'bar'}, with_context = false)
                    for (int i = 1; i < group.length; i++) {
                        if(group[i].equals("with_context=false")) {
                            addContextVar = false;
                        }
                    }
                }
            }

        }

        // we dont need to collect foreign file variables
        if(!addContextVar && varAliasMap.size() == 0) {
           return;
        }

        Map<String, PsiVariable> stringPsiVariableHashMap = TwigTypeResolveUtil.collectScopeVariables(templatePsiName, visitedFiles);

        // add context vars
        if(addContextVar) {
            for(Map.Entry<String, PsiVariable> entry: stringPsiVariableHashMap.entrySet()) {
                variables.put(entry.getKey(), entry.getValue());
            }
        }

        // add alias vars
        if(varAliasMap.size() > 0) {
            for(Map.Entry<String, String> entry: varAliasMap.entrySet()) {
                if(stringPsiVariableHashMap.containsKey(entry.getValue())) {
                    variables.put(entry.getKey(), stringPsiVariableHashMap.get(entry.getValue()));
                }
            }
        }

    }

    public static Map<String, String> getIncludeWithVarNames(String includeText) {

        String regex = "with\\s*\\{\\s*(.*[^%])\\}\\s*";
        Matcher matcher = Pattern.compile(regex).matcher(includeText.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            String group = matcher.group(1);
            return getVariableAliasMap("{" + group + "}");
        }

        return new HashMap<>();
    }

    private static Map<String, String> getVariableAliasMap(String jsonLike) {
        Map<String, String> map = new HashMap<>();

        String[] parts = jsonLike.replaceAll("^\\{|\\}$","").split("\"?(:|,)(?![^\\{]*\\})\"?");

        for (int i = 0; i < parts.length -1; i+=2) {
            map.put(StringUtils.trim(parts[i]).replaceAll("^\"|\"$|\'|\'$", ""), StringUtils.trim(parts[i+1]).replaceAll("^\"|\"$|\'|\'$", ""));
        }

        return map;
    }


    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
    }

    private Collection<VirtualFile> getImplements(TwigFile twigFile) {
        final Set<VirtualFile> targets = new HashSet<>();

        for(String templateName: TwigHelper.getTemplateNamesForFile(twigFile)) {
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

        private MyPsiRecursiveElementWalkingVisitor(@NotNull PsiFile psiFile, Map<String, PsiVariable> variables, @NotNull TwigFileVariableCollectorParameter parameter) {
            this.psiFile = psiFile;
            this.variables = variables;
            this.parameter = parameter;
        }

        @Override
        public void visitElement(PsiElement element) {

            // {% include 'template.html' %}
            if(element instanceof TwigTagWithFileReference && element.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                PsiElement includeTag = PsiElementUtils.getChildrenOfType(element, TwigHelper.getTemplateFileReferenceTagPattern("include"));
                if(includeTag != null) {
                    collectContextVars(TwigElementTypes.INCLUDE_TAG, element, includeTag);
                }
            }

            if(element instanceof TwigCompositeElement) {
                // {{ include('template.html') }}
                PsiElement includeTag = PsiElementUtils.getChildrenOfType(element, TwigHelper.getPrintBlockFunctionPattern("include"));
                if(includeTag != null) {
                    collectContextVars(TwigTokenTypes.IDENTIFIER, element, includeTag);
                }

                // {% embed "foo.html.twig"
                PsiElement embedTag = PsiElementUtils.getChildrenOfType(element, TwigHelper.getEmbedPattern());
                if(embedTag != null) {
                    collectContextVars(TwigElementTypes.EMBED_TAG, element, embedTag);
                }
            }

            super.visitElement(element);
        }

        private void collectContextVars(IElementType iElementType, @NotNull PsiElement element, @NotNull PsiElement includeTag) {

            String templateName = includeTag.getText();
            if(StringUtils.isNotBlank(templateName)) {
                for(PsiFile templateFile: TwigHelper.getTemplatePsiElements(element.getProject(), templateName)) {
                    if(templateFile.equals(psiFile)) {
                        collectIncludeContextVars(iElementType, element, includeTag, variables, parameter.getVisitedFiles());
                    }
                }

            }

        }
    }
}
