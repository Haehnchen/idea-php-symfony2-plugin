package fr.adrienbrault.idea.symfony2plugin.codeInsight.utils;

import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.FormOptionGotoCompletionRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class GotoCompletionUtil {

    private static GotoCompletionRegistrar[] CONTRIBUTORS = new FormOptionGotoCompletionRegistrar[] {
        new FormOptionGotoCompletionRegistrar()
    };

    public static Collection<GotoCompletionContributor> getContributors(final PsiElement psiElement) {

        final Collection<GotoCompletionContributor> contributors = new ArrayList<GotoCompletionContributor>();

        GotoCompletionRegistrarParameter registrar = new GotoCompletionRegistrarParameter() {
            @Override
            public void register(@NotNull ElementPattern<? extends PsiElement> pattern, GotoCompletionContributor contributor) {
                if(pattern.accepts(psiElement)) {
                    contributors.add(contributor);
                }
            }
        };

        for(GotoCompletionRegistrar register: CONTRIBUTORS) {
            register.register(registrar);
        }

        return contributors;
    }

}
