package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableTemplatePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {
                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
                        resultSet.addElement(
                            new TemplateLookupElement(entry.getKey(), entry.getValue())
                        );
                    }
                }
            }
        );

        // provides support for 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTranslationPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    Symfony2ProjectComponent symfony2ProjectComponent = parameters.getPosition().getProject().getComponent(Symfony2ProjectComponent.class);
                    TranslationStringMap map = symfony2ProjectComponent.getTranslationMap();
                    if(map == null) {
                        return;
                    }

                    PsiElement psi = parameters.getPosition();

                    // we only get a PRINT_BLOCK with a huge flat list of psi elements
                    // parsing this would be harder than use regex
                    // {{ 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain') }}
                    String str = psi.getParent().getText();

                    // @TODO: some more conditions needed here
                    // search in twig project for regex
                    String regex = "\\|\\s?trans\\s?\\(\\{.*?\\},\\s?['\"](\\w+)['\"]\\s?\\)";
                    Matcher matcher = Pattern.compile(regex).matcher(str);

                    String domainName = "messages";
                    while (matcher.find()) {
                        domainName = matcher.group(1);
                    }

                    ArrayList<String> domainMap = map.getDomainMap(domainName);
                    if(domainMap == null) {
                        return;
                    }

                    for(String stringId : domainMap) {
                        resultSet.addElement(
                            new TranslatorLookupElement(stringId, domainName)
                        );
                    }

                }
            }

        );

    }

}
