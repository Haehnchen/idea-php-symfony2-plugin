package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiReferenceBase<PsiElement> implements PsiReference {

    private String templateName;

    public TemplateReference(@NotNull StringLiteralExpression element) {
        super(element);

        templateName = element.getText().substring(
            element.getValueRange().getStartOffset(),
            element.getValueRange().getEndOffset()
        ); // Remove quotes
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        Map<String, TwigFile> twigFilesByName = getTwigFilesByName();

        return twigFilesByName.get(templateName);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        List<LookupElement> results = new ArrayList<LookupElement>();

        Map<String, TwigFile> twigFilesByName = getTwigFilesByName();
        for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
            results.add(
                new TemplateLookupElement(entry.getKey(), entry.getValue())
            );
        }

        return results.toArray();
    }

    private Map<String, TwigFile> getTwigFilesByName() {
        PhpIndex phpIndex = PhpIndex.getInstance(getElement().getProject());
        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        Collection<PhpClass> phpClasses = phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");

        Map<String, PsiDirectory> bundlesDirectories = new HashMap<String, PsiDirectory>();
        for (PhpClass phpClass : phpClasses) {
            bundlesDirectories.put(phpClass.getName(), phpClass.getContainingFile().getContainingDirectory());
        }

        Collection<VirtualFile> twigVirtualFiles = fileBasedIndex.getContainingFiles(FileTypeIndex.NAME, TwigFileType.INSTANCE, GlobalSearchScope.projectScope(getElement().getProject()));
        Map<String, TwigFile> results = new HashMap<String, TwigFile>();
        for (VirtualFile twigVirtualFile : twigVirtualFiles) {
            // Find in which bundle it is
            for (Map.Entry<String, PsiDirectory> pair : bundlesDirectories.entrySet()) {
                if (!VfsUtil.isAncestor((pair.getValue()).getVirtualFile(), twigVirtualFile, false)) {
                    continue;
                }

                String bundleName = pair.getKey(); // XXX:xxx:xxx
                String templatePath = VfsUtil.getRelativePath(twigVirtualFile, (pair.getValue()).getVirtualFile(), '/'); // Resources/views/xxx.twig
                if (null == templatePath || !templatePath.startsWith("Resources/views")) {
                    continue;
                }

                templatePath = templatePath.substring("Resources/views/".length()); // xxx.twig
                String templateDirectory = null; // xxx:XXX:xxx
                String templateFile = null; // xxx:xxx:XXX
                if (templatePath.contains("/")) {
                    int lastDirectorySeparatorIndex = templatePath.lastIndexOf("/");
                    templateDirectory = templatePath.substring(0, lastDirectorySeparatorIndex);
                    templateFile = templatePath.substring(lastDirectorySeparatorIndex + 1);
                } else {
                    templateDirectory = "";
                    templateFile = templatePath;
                }

                String templateFinalName = bundleName + ":" + templateDirectory + ":" + templateFile;
                TwigFile twigFile = (TwigFile)PsiManager.getInstance(getElement().getProject()).findFile(twigVirtualFile);

                results.put(templateFinalName, twigFile);
            }
        }

        return results;
    }

}
