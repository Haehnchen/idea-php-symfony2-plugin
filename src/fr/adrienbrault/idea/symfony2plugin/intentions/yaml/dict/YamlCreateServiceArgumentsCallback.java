package fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCreateServiceArgumentsCallback implements ServiceActionUtil.InsertServicesCallback {

    @NotNull
    private final YAMLKeyValue yamlKeyValue;

    public YamlCreateServiceArgumentsCallback(@NotNull final YAMLKeyValue yamlKeyValue) {
        this.yamlKeyValue = yamlKeyValue;
    }

    @Override
    public void insert(List<String> items) {
        String indent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentToThisElement(yamlKeyValue));
        String eol = TranslationInsertUtil.findEol(yamlKeyValue);

        PsiDocumentManager manager = PsiDocumentManager.getInstance(yamlKeyValue.getProject());
        Document document = manager.getDocument(yamlKeyValue.getContainingFile());
        if (document == null) {
            return;
        }

        List<String> arrayList = new ArrayList<String>();
        for (String item : items) {
            arrayList.add("@" + (StringUtils.isNotBlank(item) ? item : "?"));
        }

        document.insertString(yamlKeyValue.getTextRange().getEndOffset(), eol + indent + "arguments: [" + StringUtils.join(arrayList, ", ") + "]");
        manager.doPostponedOperationsAndUnblockDocument(document);
        manager.commitDocument(document);
    }

}