package fr.adrienbrault.idea.symfony2plugin.completion.yaml;

import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateGotoCompletionRegistrar;

public class YamlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // defaults:
        //   route: <caret>
        registrar.register(
            YamlElementPatternHelper.getSingleLineScalarKey("route"),
            RouteGotoCompletionProvider::new
        );

        // defaults:
        //   template: <caret>
        registrar.register(
            YamlElementPatternHelper.getSingleLineScalarKey("template"),
            TemplateGotoCompletionRegistrar::new
        );
    }
}
