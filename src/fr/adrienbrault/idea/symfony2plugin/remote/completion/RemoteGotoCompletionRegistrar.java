package fr.adrienbrault.idea.symfony2plugin.remote.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ApplicationSettings;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.remote.RemoteStorage;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.json.JsonLookupElement;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.json.JsonRawLookupElement;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.json.JsonRegistrar;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.util.RemoteJsonUtil;
import fr.adrienbrault.idea.symfony2plugin.remote.provider.JsonLookupProvider;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RemoteGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {

        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement parent = psiElement.getParent();
                if(!(parent instanceof StringLiteralExpression)) {
                    return null;
                }

                // local file
                Collection<JsonRegistrar> registrars = new ArrayList<JsonRegistrar>(
                    RemoteJsonUtil.getRegistrarJsonFromFile(psiElement.getProject())
                );

                // on http server
                if(Symfony2ApplicationSettings.getInstance().serverEnabled && RemoteStorage.getInstance(psiElement.getProject()).has(JsonLookupProvider.class)) {
                    JsonLookupProvider jsonLookupProvider = RemoteStorage.getInstance(psiElement.getProject()).get(JsonLookupProvider.class);
                    if(jsonLookupProvider != null) {
                        registrars.addAll(RemoteJsonUtil.getJsonRegistrars(jsonLookupProvider.getJsonObject()));
                    }
                }

                Set<String> provider = new HashSet<String>();
                for(JsonRegistrar jsonRegistrar: registrars) {

                    if(jsonRegistrar.getSignature() == null) {
                        continue;
                    }

                    if(jsonRegistrar.getSignature() != null) {
                        String[] split = jsonRegistrar.getSignature().replaceAll("(:)\\1", "$1").split(":");
                        if(split.length == 2) {
                            if(MethodMatcher.getMatchedSignatureWithDepth(psiElement.getParent(), new MethodMatcher.CallToSignature[] { new MethodMatcher.CallToSignature(split[0], split[1])}) != null) {
                                provider.add(jsonRegistrar.getProvider());
                            }
                        }
                    }
                }

                if(provider.size() == 0) {
                    return null;
                }

                return new JsonGotoCompletionProvider(psiElement, provider);
            }

        });

    }

    private static class JsonGotoCompletionProvider extends GotoCompletionProvider {

        private final Set<String> providers;

        public JsonGotoCompletionProvider(PsiElement element, Set<String> providers) {
            super(element);
            this.providers = providers;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();
            for(JsonRawLookupElement element: getProvider(getProject())) {
                lookupElements.add(new JsonLookupElement(element));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {


            PsiElement parent = element.getParent();
            if(!(parent instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String contents = ((StringLiteralExpression) parent).getContents();
            if(StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> psiElements = new HashSet<PsiElement>();

            for(JsonRawLookupElement jsonElement: getProvider(element.getProject())) {
                if(jsonElement.getTarget() != null && contents.equals(jsonElement.getLookupString())) {
                    psiElements.addAll(PhpElementsUtil.getClassMethodGoTo(getProject(), jsonElement.getTarget()));
                }
            }

            return psiElements;
        }

        private Collection<JsonRawLookupElement> getProvider(Project project) {

            Collection<JsonRawLookupElement> providers = new ArrayList<JsonRawLookupElement>(
                RemoteJsonUtil.getProviderJsonFromFile(project, this.providers)
            );

            if(Symfony2ApplicationSettings.getInstance().serverEnabled && RemoteStorage.getInstance(project).has(JsonLookupProvider.class)) {
                JsonLookupProvider jsonLookupProvider = RemoteStorage.getInstance(project).get(JsonLookupProvider.class);
                if(jsonLookupProvider != null) {
                    providers.addAll(RemoteJsonUtil.getProviderJsonRawLookupElements(this.providers, jsonLookupProvider.getJsonObject()));
                }
            }

            return providers;
        }

    }

}
