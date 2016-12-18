package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssistantReferenceUtil {


    public static String[] getReferenceProvider(Project project) {

        ArrayList<String> contributorAliases = new ArrayList<>();
        for(AssistantReferenceProvider referenceProvider: DefaultReferenceProvider.DEFAULT_PROVIDERS) {
            contributorAliases.add(referenceProvider.getAlias());
        }

        return contributorAliases.toArray(new String[contributorAliases.size()]);
    }

    public static String[] getContributorProvider(Project project) {

        ArrayList<String> contributorAliases = new ArrayList<>();
        for(AssistantReferenceContributor assistantReferenceContributor : DefaultReferenceContributor.DEFAULT_CONTRIBUTORS) {
            contributorAliases.add(assistantReferenceContributor.getAlias());
        }

        return contributorAliases.toArray(new String[contributorAliases.size()]);
    }

    @Nullable
    public static AssistantReferenceContributor getContributorProviderByName(Project project, String name) {

        for(AssistantReferenceContributor assistantReferenceContributor : DefaultReferenceContributor.DEFAULT_CONTRIBUTORS) {
            if(assistantReferenceContributor.getAlias().equals(name)) {
                return assistantReferenceContributor;
            }
        }

        return null;
    }

    @NotNull
    public static ArrayList<MethodParameterSetting> getMethodsParameterSettings(Project project) {

        List<MethodParameterSetting> methodParameterSettings = Settings.getInstance(project).methodParameterSettings;

        if(methodParameterSettings == null) {
            return new ArrayList<>();
        }

        return (ArrayList<MethodParameterSetting>) methodParameterSettings;
    }

    @Nullable
    public static AssistantReferenceContributor getContributor(MethodParameterSetting methodParameterSetting) {

        for(AssistantReferenceContributor assistantReferenceContributor : DefaultReferenceContributor.DEFAULT_CONTRIBUTORS) {
            if(assistantReferenceContributor.getAlias().equals(methodParameterSetting.getContributorName())) {
                return assistantReferenceContributor;
            }
        }

        return null;
    }

    @NotNull
    public static PsiReference[] getPsiReference(MethodParameterSetting methodParameterSetting, StringLiteralExpression psiElement, List<MethodParameterSetting> configsMethodScope, MethodReference method) {

        // custom references
        if(methodParameterSetting.hasAssistantPsiReferenceContributor()) {
            return methodParameterSetting.getAssistantPsiReferenceContributor().getPsiReferences(psiElement);
        }

        // build provider parameter
        AssistantReferenceProvider.AssistantReferenceProviderParameter assistantReferenceProviderParameter = new AssistantReferenceProvider.AssistantReferenceProviderParameter(
            psiElement,
            methodParameterSetting,
            configsMethodScope,
            method
        );

        String ReferenceProvider = methodParameterSetting.getReferenceProviderName();
        for(AssistantReferenceProvider referenceProvider: DefaultReferenceProvider.DEFAULT_PROVIDERS) {
            if(referenceProvider.getAlias().equals(ReferenceProvider)) {
                return new PsiReference[] { referenceProvider.getPsiReference(assistantReferenceProviderParameter) };
            }
        }

        return new PsiReference[0];
    }

}
