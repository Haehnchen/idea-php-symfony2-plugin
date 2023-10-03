package fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlCreateServiceArgumentsCallback implements ServiceActionUtil.InsertServicesCallback {

    @NotNull
    private final YAMLKeyValue serviceKeyValue;

    public YamlCreateServiceArgumentsCallback(@NotNull final YAMLKeyValue serviceKeyValue) {
        this.serviceKeyValue = serviceKeyValue;
    }

    @Override
    public void insert(List<String> items) {
        PsiDocumentManager manager = PsiDocumentManager.getInstance(serviceKeyValue.getProject());
        Document document = manager.getDocument(serviceKeyValue.getContainingFile());
        if (document == null) {
            return;
        }

        List<String> arrayList = new ArrayList<>();
        for (String item : items) {
            arrayList.add("'@" + (StringUtils.isNotBlank(item) ? item : "?") + "'");
        }

        YamlHelper.putKeyValue(serviceKeyValue, "arguments", "[" + StringUtils.join(arrayList, ", ") + "]");

        manager.doPostponedOperationsAndUnblockDocument(document);
        manager.commitDocument(document);
    }
}