package fr.adrienbrault.idea.symfony2plugin.templating.webpack;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.*;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * - encore_entry_link_tags()
 * - encore_entry_script_tags()
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class WebpackEncoreGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(TwigPattern.getPrintBlockOrTagFunctionPattern("encore_entry_link_tags", "encore_entry_script_tags"), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new WebpackEncoreGotoCompletionRegistrar.EncoreEntry(psiElement);
        });
    }

    private static class EncoreEntry extends GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
        private EncoreEntry(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = element.getText();

            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            HashSet<PsiElement> targets = new HashSet<>();
            SymfonyWebpackUtil.visitAllEntryFileTypes(element.getProject(), triple -> {
                if (contents.equalsIgnoreCase(triple.getEntry())) {
                    String entryTarget = triple.getEntryTarget();
                    if (entryTarget != null) {
                        // "./foobar" => "foobar"
                        String replace = entryTarget.replace("\\", "/");
                        if (replace.startsWith("./")) {
                            replace = replace.substring(2);
                        }

                        VirtualFile relativeFile = VfsUtil.findRelativeFile(triple.getVirtualFile().getParent(), replace.split("/"));
                        if (relativeFile != null) {
                            PsiFile file = PsiManager.getInstance(getProject()).findFile(relativeFile);
                            if (file != null) {
                                targets.add(file);
                            }
                        }
                    }

                    PsiElement target = triple.getPsiElement();
                    if (target != null) {
                        targets.add(target);
                    } else {
                        PsiFile file = PsiManager.getInstance(getProject()).findFile(triple.getVirtualFile());
                        if (file != null) {
                            targets.add(file);
                        }
                    }
                }
            });

            return targets;
        }

        @Override
        public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
            SymfonyWebpackUtil.visitAllEntryFileTypes(getProject(), pair ->
                {
                    LookupElementBuilder lookupElement = LookupElementBuilder.create(pair.getEntry())
                        .withIcon(Symfony2Icons.SYMFONY)
                        .withTypeText(pair.getVirtualFile().getName());

                    arguments.getResultSet().addElement(lookupElement);
                }
            );
        }
    }
}
