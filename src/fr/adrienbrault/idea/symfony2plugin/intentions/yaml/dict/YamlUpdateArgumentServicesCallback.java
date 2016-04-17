package fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;
import org.jetbrains.yaml.psi.YAMLValue;
import org.jetbrains.yaml.psi.impl.YAMLArrayImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlUpdateArgumentServicesCallback implements ServiceActionUtil.InsertServicesCallback {

    private final Project project;
    private final YAMLKeyValue argumentsKeyValue;
    private final YAMLKeyValue yamlKeyValue;

    public YamlUpdateArgumentServicesCallback(Project project, YAMLKeyValue argumentsKeyValue, YAMLKeyValue serviceKeyValue) {
        this.project = project;
        this.argumentsKeyValue = argumentsKeyValue;
        this.yamlKeyValue = serviceKeyValue;
    }

    @Override
    public void insert(List<String> items) {

        YAMLValue yamlCompoundValue = argumentsKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return;
        }

        int appendEndOffset = -1;
        String insertString = null;

        if(yamlCompoundValue instanceof YAMLArrayImpl) {
            // [ @foo ]

            // we wound array
            List<PsiElement> yamlArguments = YamlHelper.getYamlArrayOnSequenceOrArrayElements((YAMLCompoundValue) yamlCompoundValue);
            if(yamlArguments != null && yamlArguments.size() > 0) {
                appendEndOffset = yamlArguments.get(yamlArguments.size() - 1).getTextRange().getEndOffset();

                List<String> arrayList = new ArrayList<String>();
                for (String item : items) {
                    arrayList.add(String.format("'@%s'", StringUtils.isNotBlank(item) ? item : "?"));
                }

                insertString = ", " + StringUtils.join(arrayList, ", ");
            }

        } else if(yamlCompoundValue instanceof YAMLSequence) {
            // - @foo

            // search indent and EOL value
            String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(yamlCompoundValue));
            String eol = TranslationInsertUtil.findEol(yamlKeyValue);

            List<String> yamlSequences = new ArrayList<String>();
            for (String item : items) {
                // should be faster then YamlPsiElementFactory.createFromText
                yamlSequences.add(indent + String.format("- '@%s'", StringUtils.isNotBlank(item) ? item : "?"));
            }

            appendEndOffset = yamlCompoundValue.getTextRange().getEndOffset();
            insertString = eol + StringUtils.join(yamlSequences, eol);
        }

        if(appendEndOffset == -1) {
            return;
        }

        PsiDocumentManager manager = PsiDocumentManager.getInstance(project);
        Document document = manager.getDocument(yamlKeyValue.getContainingFile());
        if (document == null) {
            return;
        }

        document.insertString(appendEndOffset, insertString);
        manager.doPostponedOperationsAndUnblockDocument(document);
        manager.commitDocument(document);
    }

}
