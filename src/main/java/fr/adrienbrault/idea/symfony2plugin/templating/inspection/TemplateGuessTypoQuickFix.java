package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateGuessTypoQuickFix extends AbstractGuessTypoQuickFix {
    @NotNull
    private final String missingTemplateName;

    public TemplateGuessTypoQuickFix(@NotNull String missingTemplateName) {
        this.missingTemplateName = missingTemplateName;
    }

    @Override
    protected @NotNull String getSuggestionLabel() {
        return "Template";
    }

    @Override
    protected @NotNull List<String> getSimilarItems(@NotNull Project project) {
        String strippedInput = stripTemplateFormatAndExtensionLowered(missingTemplateName);

        Map<String, String> strippedToOriginal = new HashMap<>();
        Set<String> strippedNames = new HashSet<>();

        for (String template : TwigUtil.getTemplateMap(project, true).keySet()) {
            String stripped = stripTemplateFormatAndExtensionLowered(template);
            strippedToOriginal.put(stripped, template);
            strippedNames.add(stripped);
        }

        return SimilarSuggestionUtil.findSimilarString(strippedInput, strippedNames).stream()
            .map(strippedToOriginal::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @NotNull
    private static String stripTemplateFormatAndExtensionLowered(@NotNull String templateNameIfMissing) {
        return templateNameIfMissing
            .toLowerCase()
            .replaceAll("\\.[^.]*$", "").replaceAll("\\.[^.]*$", "");
    }
}
