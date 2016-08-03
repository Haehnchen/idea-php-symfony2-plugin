package fr.adrienbrault.idea.symfony2plugin.completion.xml;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class XmlGotoCompletionRegistrar implements GotoCompletionRegistrar  {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // <import resource="config_foo.xml"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            ImportResourceGotoCompletionProvider::new
        );

        // <service id="<caret>" class="MyFoo\Foo\Apple"/>
        registrar.register(
            XmlPatterns.psiElement().withParent(XmlHelper.getServiceIdNamePattern()),
            ServiceIdCompletionProvider::new
        );
    }

    private static class ImportResourceGotoCompletionProvider extends GotoCompletionProvider {

        public ImportResourceGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String xmlAttributeValue = GotoCompletionUtil.getXmlAttributeValue(element);
            if(xmlAttributeValue == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<>();

            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(element.getProject(), xmlAttributeValue));
            targets.addAll(FileResourceUtil.getFileResourceTargetsInBundleDirectory(element.getProject(), xmlAttributeValue));

            PsiFile containingFile = element.getContainingFile();
            if(containingFile != null) {
                targets.addAll(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, xmlAttributeValue));
            }

            return targets;
        }
    }

    private static class ServiceIdCompletionProvider extends GotoCompletionProvider {
        private ServiceIdCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            // find class name of service tag
            PsiElement xmlToken = this.getElement();
            if(xmlToken instanceof XmlToken) {
                PsiElement xmlAttrValue = xmlToken.getParent();
                if(xmlAttrValue instanceof XmlAttributeValue) {
                    PsiElement xmlAttribute = xmlAttrValue.getParent();
                    if(xmlAttribute instanceof XmlAttribute) {
                        PsiElement xmlTag = xmlAttribute.getParent();
                        if(xmlTag instanceof XmlTag) {
                            String aClass = ((XmlTag) xmlTag).getAttributeValue("class");
                            if(aClass != null && StringUtils.isNotBlank(aClass)) {
                                lookupElements.add(LookupElementBuilder.create(
                                    ServiceUtil.getServiceNameForClass(getProject(), aClass)).withIcon(Symfony2Icons.SERVICE)
                                );
                            }
                        }
                    }
                }
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Collections.emptyList();
        }
    }
}
