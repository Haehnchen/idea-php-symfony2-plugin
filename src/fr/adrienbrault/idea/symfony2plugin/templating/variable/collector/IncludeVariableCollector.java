package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;


public class IncludeVariableCollector implements TwigFileVariableCollector, TwigFileVariableCollector.TwigFileVariableCollectorExt {

    @Override
    public void collectVars(TwigFileVariableCollectorParameter parameter, final HashMap<String, PsiVariable> variables) {

        final PsiFile psiFile = parameter.getElement().getContainingFile();
        if(!(psiFile instanceof TwigFile) || PsiTreeUtil.getChildOfType(psiFile, TwigExtendsTag.class) != null) {
            return;
        }

        Collection<PsiFile> files = getImplements((TwigFile) psiFile);
        if(files.size() == 0) {
            return;
        }

        for(PsiFile twigFile: files) {

            twigFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if(element instanceof TwigTagWithFileReference && element.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                        PsiElement includeTag = PsiElementUtils.getChildrenOfType(element, TwigHelper.getTemplateFileReferenceTagPattern("include"));
                        if(includeTag != null) {
                            String templateName = includeTag.getText();
                            if(StringUtils.isNotBlank(templateName)) {
                                for(PsiFile templateFile: TwigHelper.getTemplateFilesByName(element.getProject(), templateName)) {
                                    if(templateFile.equals(psiFile)) {
                                        collectIncludeContextVars(includeTag, variables);
                                    }
                                }

                            }
                        }
                    }

                    super.visitElement(element);
                }
            });
        }

    }

    private void collectIncludeContextVars(PsiElement includeTag, HashMap<String, PsiVariable> variables) {

        // @TODO: support "only" and variable alias
        for(Map.Entry<String, PsiVariable> entry: TwigTypeResolveUtil.collectScopeVariables(includeTag).entrySet()) {
            variables.put(entry.getKey(), entry.getValue());
        }

    }

    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, HashMap<String, Set<String>> variables) {

    }

    private Collection<PsiFile> getImplements(TwigFile twigFile) {

        final Collection<PsiFile> targets = new ArrayList<PsiFile>();

        for(Map.Entry<String, PsiFile> entry: TwigUtil.getTemplateName(twigFile).entrySet()) {

            final Project project = twigFile.getProject();
            FileBasedIndexImpl.getInstance().getFilesWithKey(TwigIncludeStubIndex.KEY, new HashSet<String>(Arrays.asList(entry.getKey())), new Processor<VirtualFile>() {
                @Override
                public boolean process(VirtualFile virtualFile) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

                    if(psiFile != null) {
                        targets.add(psiFile);
                    }

                    return true;
                }
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        }

        return targets;
    }

}
