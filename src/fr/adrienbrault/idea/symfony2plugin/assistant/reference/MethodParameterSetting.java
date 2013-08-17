package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantPsiReferenceContributor;

@Tag("method_parameter_settings")
public class MethodParameterSetting {

    private String callTo;
    private String methodName;
    private String ReferenceProviderName;
    private int indexParameter;
    private String contributorName;
    private String contributorData;

    private AssistantPsiReferenceContributor assistantPsiReferenceContributor = null;

    public MethodParameterSetting() {
    }

    public MethodParameterSetting(String callTo, String methodName, int indexParameter, String provider) {
        this.callTo = callTo;
        this.methodName = methodName;
        this.indexParameter = indexParameter;
        this.ReferenceProviderName = provider;
    }

    public MethodParameterSetting(String callTo, String methodName, int indexParameter, String provider, String contributorName, String contributorTypeData) {
        this(callTo, methodName, indexParameter, provider);
        this.contributorName = contributorName;
        this.contributorData = contributorTypeData;
    }

    public MethodParameterSetting(String callTo, String methodName, int indexParameter, DefaultReferenceProvider.DEFAULT_PROVIDER_ENUM provider, DefaultReferenceContributor.DEFAULT_CONTRIBUTORS_ENUM contributorName, String contributorTypeData) {
        this(callTo, methodName, indexParameter, provider.toString());
        this.contributorName = contributorName.toString();
        this.contributorData = contributorTypeData;
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

    public void setContributorName(String contributorType) {
        this.contributorName = contributorType;
    }

    public void setContributorData(String contributorTypeData) {
        this.contributorData = contributorTypeData;
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

    @Attribute("contributorName")
    public String getContributorName() {
        return contributorName;
    }

    @Attribute("ContributorData")
    public String getContributorData() {
        return contributorData;
    }

    public MethodParameterSetting withPsiReference(AssistantPsiReferenceContributor assistantPsiReferenceContributor) {
        this.assistantPsiReferenceContributor = assistantPsiReferenceContributor;
        return this;
    }

    public AssistantPsiReferenceContributor getAssistantPsiReferenceContributor() {
        return assistantPsiReferenceContributor;
    }

    public boolean hasAssistantPsiReferenceContributor() {
        return this.assistantPsiReferenceContributor != null;
    }

}
