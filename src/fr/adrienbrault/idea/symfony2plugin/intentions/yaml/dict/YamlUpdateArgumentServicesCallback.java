package fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

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

        PsiElement yamlCompoundValue = argumentsKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return;
        }


        int appendEndOffset = -1;
        String insertString = null;

        PsiElement firstChild1 = yamlCompoundValue.getFirstChild();


        if(firstChild1 instanceof YAMLSequence) {
            // - @foo

            // search indent and EOL value
            String indent = argumentsKeyValue.getValueIndent();
            String eol = TranslationInsertUtil.findEol(yamlKeyValue);

            List<String> yamlSequences = new ArrayList<String>();
            for (String item : items) {
                // should be faster then YamlPsiElementFactory.createFromText
                yamlSequences.add(indent + String.format("- @%s", StringUtils.isNotBlank(item) ? item : "?"));
            }

            appendEndOffset = yamlCompoundValue.getTextRange().getEndOffset();
            insertString = eol + StringUtils.join(yamlSequences, eol);

        } else if(firstChild1 instanceof YAMLArray) {

            // we wound array
            List<PsiElement> yamlArguments = YamlHelper.getYamlArrayOnSequenceOrArrayElements((YAMLCompoundValue) yamlCompoundValue);
            if(yamlArguments != null && yamlArguments.size() > 0) {
                appendEndOffset = yamlArguments.get(yamlArguments.size() - 1).getTextRange().getEndOffset();

                List<String> arrayList = new ArrayList<String>();
                for (String item : items) {
                    arrayList.add("@" + (StringUtils.isNotBlank(item) ? item : "?"));
                }

                insertString = ", " + StringUtils.join(arrayList, ", ");
            }

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
