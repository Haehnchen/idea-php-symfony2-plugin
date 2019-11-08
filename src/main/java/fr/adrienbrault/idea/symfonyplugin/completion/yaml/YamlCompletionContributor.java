package fr.adrienbrault.idea.symfonyplugin.completion.yaml;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.util.completion.YamlKeywordsCompletionProvider;
import fr.adrienbrault.idea.symfonyplugin.util.completion.YamlTagCompletionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

/**
 * @author Thomas Schulz <mail@king2500.net>
 */
public class YamlCompletionContributor extends CompletionContributor {
    public YamlCompletionContributor() {
        // config:
        //   key: !<caret>
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSingleLineTextOrTag(),
            new YamlTagCompletionProvider()
        );

        // config:
        //   key: <caret>
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSingleLineText(),
            new YamlKeywordsCompletionProvider()
        );
    }

    @Override
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        // Only for Yaml tag places (scalar values)
        //   key: !<caret>
        if (!YamlElementPatternHelper.getSingleLineTextOrTag().accepts(position)
            && !(position.getPrevSibling() instanceof YAMLKeyValue)
            && !(position.getParent() instanceof YAMLSequenceItem)
            && !(position.getParent() instanceof YAMLSequence)
        ) {
            return super.invokeAutoPopup(position, typeChar);
        }

        return typeChar == '!';
    }
}
