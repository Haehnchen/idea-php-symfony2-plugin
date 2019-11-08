package fr.adrienbrault.idea.symfonyplugin.intentions.yaml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfonyplugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ServiceTag;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlServiceTagIntention extends PsiElementBaseIntentionAction {

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {

        if(psiElement.getContainingFile().getFileType() != YAMLFileType.YML || !Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        return YamlHelper.findServiceInContext(psiElement) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {

        YAMLKeyValue serviceKeyValue = YamlHelper.findServiceInContext(psiElement);
        if(serviceKeyValue == null) {
            return;
        }

        Pair<PhpClass, Set<String>> invoke = invoke(project, serviceKeyValue);
        if(invoke == null) {
            return;
        }

        Set<String> phpClassServiceTags = invoke.getSecond();
        if(phpClassServiceTags.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "Ops, no possible Tag found");
            return;
        }

        PhpClass resolvedClassDefinition = invoke.getFirst();

        int appendEndOffset = -1;
        String insertString = null;

        YAMLKeyValue argumentsKeyValue = YamlHelper.getYamlKeyValue(serviceKeyValue, "tags");
        if(argumentsKeyValue == null) {

            // we dont an "tags" key so we need to create one

            String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(serviceKeyValue));

            List<String> yamlSequences = new ArrayList<>();
            for (String item : phpClassServiceTags) {
                ServiceTag serviceTag = new ServiceTag(resolvedClassDefinition, item);
                ServiceUtil.decorateServiceTag(serviceTag);
                yamlSequences.add(indent + " " + serviceTag.toYamlString());
            }

            appendEndOffset = serviceKeyValue.getTextRange().getEndOffset();

            String eol = TranslationInsertUtil.findEol(serviceKeyValue);
            insertString = eol + indent + "tags:" + eol + StringUtils.join(yamlSequences, eol);

        } else {

            // we found a "tags" key so update
            PsiElement value = argumentsKeyValue.getValue();
            if(!(value instanceof YAMLCompoundValue)) {
                HintManager.getInstance().showErrorHint(editor, "Sry, not supported tags definition");
                return;
            }

            PsiElement firstChild = value.getFirstChild();
            if(firstChild instanceof YAMLSequence) {

                String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(argumentsKeyValue));

                List<String> yamlSequences = new ArrayList<>();
                for (String item : phpClassServiceTags) {
                    ServiceTag serviceTag = new ServiceTag(resolvedClassDefinition, item);
                    ServiceUtil.decorateServiceTag(serviceTag);
                    yamlSequences.add(indent + serviceTag.toYamlString());
                }

                appendEndOffset = argumentsKeyValue.getTextRange().getEndOffset();

                String eol = TranslationInsertUtil.findEol(argumentsKeyValue);
                insertString = eol + StringUtils.join(yamlSequences, eol);
            }

        }

        if(appendEndOffset == -1) {
            HintManager.getInstance().showErrorHint(editor, "Sry, not supported service definition");
            return;
        }

        PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        Document document = manager.getDocument(serviceKeyValue.getContainingFile());
        if (document == null) {
            return;
        }

        document.insertString(appendEndOffset, insertString);
        manager.doPostponedOperationsAndUnblockDocument(document);
        manager.commitDocument(document);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Add Tags";
    }

    @Nullable
    private static Pair<PhpClass, Set<String>> invoke(@NotNull Project project, @NotNull YAMLKeyValue serviceKeyValue) {

        String aClass = YamlHelper.getYamlKeyValueAsString(serviceKeyValue, "class");
        if(aClass == null|| StringUtils.isBlank(aClass)) {
            return null;
        }

        PhpClass resolvedClassDefinition = ServiceUtil.getResolvedClassDefinition(project, aClass, new ContainerCollectionResolver.LazyServiceCollector(project));
        if(resolvedClassDefinition == null) {
            return null;
        }

        Set<String> phpClassServiceTags = ServiceUtil.getPhpClassServiceTags(resolvedClassDefinition);

        Set<String> strings = YamlHelper.collectServiceTags(serviceKeyValue);
        if(strings != null && strings.size() > 0) {
            for (String s : strings) {
                if(phpClassServiceTags.contains(s)) {
                    phpClassServiceTags.remove(s);
                }
            }
        }

        return Pair.create(resolvedClassDefinition, phpClassServiceTags);
    }

}
