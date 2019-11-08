package fr.adrienbrault.idea.symfonyplugin.assistant.signature;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import fr.adrienbrault.idea.symfonyplugin.assistant.AssistantPsiReferenceContributor;
import fr.adrienbrault.idea.symfonyplugin.assistant.reference.DefaultReferenceProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("method_signature_settings")
public class MethodSignatureSetting {

    private String callTo;
    private String methodName;
    private String ReferenceProviderName;
    private int indexParameter;

    private AssistantPsiReferenceContributor assistantPsiReferenceContributor = null;

    public MethodSignatureSetting() {
    }

    public MethodSignatureSetting(String callTo, String methodName, int indexParameter, String provider) {
        this.callTo = callTo;
        this.methodName = methodName;
        this.indexParameter = indexParameter;
        this.ReferenceProviderName = provider;
    }

    public MethodSignatureSetting(String callTo, String methodName, int indexParameter, DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM provider) {
        this(callTo, methodName, indexParameter, provider.toString());
    }

    public void setCallTo(String callTo) {
        this.callTo = callTo;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setReferenceProviderName(String providerName) {
        this.ReferenceProviderName = providerName;
    }

    public void setIndexParameter(int indexParameter) {
        this.indexParameter = indexParameter;
    }

    @Attribute("CallTo")
    public String getCallTo() {
        return callTo;
    }

    @Attribute("MethodName")
    public String getMethodName() {
        return methodName;
    }

    @Attribute("IndexParameter")
    public int getIndexParameter() {
        return indexParameter;
    }

    @Attribute("ReferenceProviderName")
    public String getReferenceProviderName() {
        return ReferenceProviderName;
    }

}
