package fr.adrienbrault.idea.symfony2plugin.translation.dict;


import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationCollector;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVistor;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlKeyFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TranslationUtil {

    static public VirtualFile[] getDomainFilePsiElements(Project project, String domainName) {

        DomainMappings domainMappings = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);
        List<VirtualFile> virtualFiles = new ArrayList<>();

        for(DomainFileMap domain: domainMappings.getDomainFileMaps()) {
            if(domain.getDomain().equals(domainName)) {
                VirtualFile virtualFile = domain.getFile();
                if(virtualFile != null) {
                    virtualFiles.add(virtualFile);
                }
            }
        }

        return virtualFiles.toArray(new VirtualFile[virtualFiles.size()]);
    }

    public static PsiElement[] getTranslationPsiElements(final Project project, final String translationKey, final String domain) {


        final List<PsiElement> psiFoundElements = new ArrayList<>();
        final List<VirtualFile> virtualFilesFound = new ArrayList<>();

        // @TODO: completely remove this? support translation paths from service compiler
        // search for available domain files
        for(VirtualFile translationVirtualFile : getDomainFilePsiElements(project, domain)) {

            if(translationVirtualFile.getFileType() != YAMLFileType.YML) {
                continue;
            }

            PsiFile psiFile = PsiElementUtils.virtualFileToPsiFile(project, translationVirtualFile);
            if(psiFile instanceof YAMLFile) {
                PsiElement yamlDocu = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocu != null) {
                    YAMLKeyValue goToPsi = YamlKeyFinder.findKeyValueElement(yamlDocu, translationKey);
                    if(goToPsi != null) {
                        // multiline are line values are not resolve properly on psiElements use key as fallback target
                        PsiElement valuePsiElement = goToPsi.getValue();
                        psiFoundElements.add(valuePsiElement != null ? valuePsiElement : goToPsi);
                        virtualFilesFound.add(translationVirtualFile);
                    }
                }

            }

        }

        // collect on index
        final YamlTranslationCollector translationCollector = (keyName, yamlKeyValue) -> {
            if (keyName.equals(translationKey)) {

                // multiline "line values" are not resolve properly on psiElements use key as fallback target
                PsiElement valuePsiElement = yamlKeyValue.getValue();
                psiFoundElements.add(valuePsiElement != null ? valuePsiElement : yamlKeyValue);

                return false;
            }

            return true;
        };

        FileBasedIndexImpl.getInstance().getFilesWithKey(YamlTranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domain)), virtualFile -> {
            // prevent duplicate targets and dont walk same file twice
            if(virtualFilesFound.contains(virtualFile)) {
                return true;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile instanceof YAMLFile) {
                YamlTranslationVistor.collectFileTranslations((YAMLFile) psiFile, translationCollector);
            } else if(("xlf".equalsIgnoreCase(virtualFile.getExtension()) || "xliff".equalsIgnoreCase(virtualFile.getExtension())) && psiFile instanceof XmlFile) {
                // fine: xlf registered as XML file. try to find source value
                psiFoundElements.addAll(getTargetForXlfAsXmlFile((XmlFile) psiFile, translationKey));
            } else if(("xlf".equalsIgnoreCase(virtualFile.getExtension()) || "xliff".equalsIgnoreCase(virtualFile.getExtension()) && psiFile != null)) {
                // xlf are plain text because not supported by jetbrains
                // for now we can only set file target
                psiFoundElements.addAll(
                    FileBasedIndexImpl.getInstance().getValues(YamlTranslationStubIndex.KEY, domain, GlobalSearchScope.filesScope(project, Collections.singletonList(virtualFile)))
                    .stream().filter(string -> string.contains(translationKey)).map(string -> psiFile).collect(Collectors.toList())
                );
            }

            return true;
        }, GlobalSearchScope.allScope(project));

        return psiFoundElements.toArray(new PsiElement[psiFoundElements.size()]);
    }

    /**
     * Find targets for xlf files if registered as XML

     * 1.2 xliff -> file -> body -> trans-unit -> source
     * 2.0 xliff -> file -> group -> unit -> segment -> source
     */
    @NotNull
    public static Collection<PsiElement> getTargetForXlfAsXmlFile(@NotNull XmlFile xmlFile, @NotNull String key) {
        XmlTag rootTag = xmlFile.getRootTag();
        if(rootTag == null) {
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        // find source key
        Consumer<XmlTag> consumer = xmlTag -> {
            XmlTag source = xmlTag.findFirstSubTag("source");
            if (source != null) {
                String text = source.getValue().getText();
                if (key.equalsIgnoreCase(text)) {
                    psiElements.add(source);
                }
            }
        };

        for (XmlTag file : rootTag.findSubTags("file")) {
            // version="1.2"
            for (XmlTag body : file.findSubTags("body")) {
                for (XmlTag transUnit : body.findSubTags("trans-unit")) {
                    consumer.consume(transUnit);
                  }
            }

            // version="2.0"
            for (XmlTag group : file.findSubTags("group")) {
                for (XmlTag unit : group.findSubTags("unit")) {
                    for (XmlTag segment : unit.findSubTags("segment")) {
                        consumer.consume(segment);
                    }
                }
            }
        }

        return psiElements;
    }

    public static boolean hasDomain(Project project, String domainName) {
        return TranslationIndex.getInstance(project).getTranslationMap().getDomainList().contains(domainName) ||
            FileBasedIndexImpl.getInstance().getValues(
                YamlTranslationStubIndex.KEY,
                domainName,
                GlobalSearchScope.allScope(project)
            ).size() > 0;
    }

    public static boolean hasTranslationKey(@NotNull Project project, String keyName, String domainName) {

        if(!hasDomain(project, domainName)) {
            return false;
        }

        Set<String> domainMap = TranslationIndex.getInstance(project).getTranslationMap().getDomainMap(domainName);
        if(domainMap != null && domainMap.contains(keyName)) {
            return true;
        }

        for(Set<String> keys: FileBasedIndexImpl.getInstance().getValues(YamlTranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project))){
            if(keys.contains(keyName)) {
                return true;
            }
        }

        return false;
    }


    public static List<LookupElement> getTranslationLookupElementsOnDomain(Project project, String domainName) {

        Set<String> keySet = new HashSet<>();
        List<Set<String>> test = FileBasedIndexImpl.getInstance().getValues(YamlTranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project));
        for(Set<String> keys: test ){
            keySet.addAll(keys);
        }

        List<LookupElement> lookupElements = new ArrayList<>();

        TranslationStringMap map = TranslationIndex.getInstance(project).getTranslationMap();
        Collection<String> domainMap = map.getDomainMap(domainName);

        if(domainMap != null) {

            // php translation parser; are not weak and valid keys
            for(String stringId : domainMap) {
                lookupElements.add(new TranslatorLookupElement(stringId, domainName));
            }

            // attach weak translations keys on file index
            for(String stringId : keySet) {
                if(!domainMap.contains(stringId)) {
                    lookupElements.add(new TranslatorLookupElement(stringId, domainName, true));
                }
            }

            return lookupElements;
        }

        // fallback on index
        for(String stringId : keySet) {
            lookupElements.add(new TranslatorLookupElement(stringId, domainName, true));
        }

        return lookupElements;
    }

    @NotNull
    public static List<LookupElement> getTranslationDomainLookupElements(Project project) {

        List<LookupElement> lookupElements = new ArrayList<>();

        // domains on complied file
        TranslationStringMap map = TranslationIndex.getInstance(project).getTranslationMap();
        Set<String> domainList = map.getDomainList();
        for(String domainKey : domainList) {
            lookupElements.add(new TranslatorLookupElement(domainKey, domainKey));
        }

        SymfonyProcessors.CollectProjectUniqueKeysStrong projectUniqueKeysStrong = new SymfonyProcessors.CollectProjectUniqueKeysStrong(project, YamlTranslationStubIndex.KEY, domainList);
        FileBasedIndexImpl.getInstance().processAllKeys(YamlTranslationStubIndex.KEY, projectUniqueKeysStrong, project);

        // attach index domains as weak one
        for(String domainKey: projectUniqueKeysStrong.getResult()) {
            if(!domainList.contains(domainKey)) {
                lookupElements.add(new TranslatorLookupElement(domainKey, domainKey, true));
            }
        }

        return lookupElements;
    }

    public static List<PsiFile> getDomainPsiFiles(final Project project, String domainName) {

        final List<PsiFile> results = new ArrayList<>();
        final List<VirtualFile> uniqueFileList = new ArrayList<>();

        // get translation files from compiler
        for(VirtualFile virtualFile : TranslationUtil.getDomainFilePsiElements(project, domainName)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                uniqueFileList.add(virtualFile);
                results.add(psiFile);
            }
        }

        FileBasedIndexImpl.getInstance().getFilesWithKey(YamlTranslationStubIndex.KEY, new HashSet<>(Arrays.asList(domainName)), virtualFile -> {
            if(uniqueFileList.contains(virtualFile)) {
                return true;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile != null) {
                uniqueFileList.add(virtualFile);
                results.add(psiFile);
            }

            return true;
        }, PhpIndex.getInstance(project).getSearchScope());

        return results;
    }

    @NotNull
    public static Set<String> getXliffTranslations(InputStream content) {

        Set<String> set = new HashSet<>();

        Document document;

        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(content);
        } catch (ParserConfigurationException e) {
            return set;
        } catch (SAXException e) {
            return set;
        } catch (IOException e) {
            return set;
        }

        if(document == null) {
            return set;
        }

        for (String s : new String[]{"//xliff/file/body/trans-unit/source", "//xliff/file/group/unit/segment/source", "//xliff/file/unit/segment/source"}) {
            visitNodes(s, set, document);
        }

        return set;
    }

    private static void visitNodes(@NotNull String xpath, @NotNull Set<String> set, @NotNull Document document) {
        Object result;
        try {
            // @TODO: xpath should not use "file/body"
            XPathExpression xPathExpr = XPathFactory.newInstance().newXPath().compile(xpath);
            result = xPathExpr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return;
        }

        if(!(result instanceof NodeList)) {
            return;
        }

        NodeList nodeList = (NodeList) result;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String textContent = node.getTextContent();
            if(org.apache.commons.lang.StringUtils.isNotBlank(textContent)) {
                set.add(textContent);
            }
        }
    }
}
