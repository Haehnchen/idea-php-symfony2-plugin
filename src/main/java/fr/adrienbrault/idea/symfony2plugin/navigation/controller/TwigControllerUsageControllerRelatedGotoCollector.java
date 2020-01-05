package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Attach "Foo\FoobarController::FooAction" to its controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigControllerUsageControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {
    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        Method method = parameter.getMethod();
        PhpClass containingClass = method.getContainingClass();

        if (containingClass == null) {
            return;
        }

        String controllerAction = StringUtils.stripStart(containingClass.getPresentableFQN(), "\\") + "::" + method.getName();

        Collection<VirtualFile> targets = new HashSet<>();
        FileBasedIndex.getInstance().getFilesWithKey(TwigControllerStubIndex.KEY, new HashSet<>(Collections.singletonList(controllerAction)), virtualFile -> {
            targets.add(virtualFile);
            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(parameter.getProject()), TwigFileType.INSTANCE));

        for (PsiFile psiFile: PsiElementUtils.convertVirtualFilesToPsiFiles(parameter.getProject(), targets)) {
            TwigUtil.visitControllerFunctions(psiFile, pair -> {
                if (pair.getFirst().equalsIgnoreCase(controllerAction)) {
                    parameter.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(pair.getSecond()).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                }
            });
        }
    }
}
