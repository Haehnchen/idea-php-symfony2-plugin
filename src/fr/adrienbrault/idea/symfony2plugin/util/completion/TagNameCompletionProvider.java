package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndexImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.ContainerTagLookupElement;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlTagParser;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesTagStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;

public class TagNameCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        completionResultSet.addAllElements(getTagLookupElements(completionParameters.getPosition().getProject()));
    }

    public static Collection<LookupElement> getTagLookupElements(Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        Set<String> uniqueTags = new HashSet<String>();

        XmlTagParser xmlEventParser = ServiceXmlParserFactory.getInstance(project, XmlTagParser.class);
        for(String tag: xmlEventParser.get()) {
            uniqueTags.add(tag);
            lookupElements.add(new ContainerTagLookupElement(tag));
        }

        SymfonyProcessors.CollectProjectUniqueKeys projectUniqueKeysStrong = new SymfonyProcessors.CollectProjectUniqueKeys(project, ServicesTagStubIndex.KEY);
        FileBasedIndexImpl.getInstance().processAllKeys(ServicesTagStubIndex.KEY, projectUniqueKeysStrong, project);

        for(String serviceName: projectUniqueKeysStrong.getResult()) {
            List<Set<String>> tags = FileBasedIndexImpl.getInstance().getValues(ServicesTagStubIndex.KEY, serviceName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), XmlFileType.INSTANCE, YAMLFileType.YML));
            for(Set<String> tagDef: tags) {
                for(String tag: tagDef) {
                    if(!uniqueTags.contains(tag)) {
                        uniqueTags.add(tag);
                        lookupElements.add(new ContainerTagLookupElement(tag, true));
                    }
                }
            }
        }
        return lookupElements;
    }


}
