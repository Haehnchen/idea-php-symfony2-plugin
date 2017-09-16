package fr.adrienbrault.idea.symfony2plugin.translation.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationCollector;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVistor;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.w3c.dom.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationUtil {
    private static final String[] XLIFF_XPATH = {
        "//xliff/file/body/trans-unit/source",
        "//xliff/file/group/unit/segment/source",
        "//xliff/file/unit/segment/source"
    };

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
        List<PsiElement> psiFoundElements = new ArrayList<>();
        List<VirtualFile> virtualFilesFound = new ArrayList<>();

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
                    YAMLKeyValue goToPsi = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, translationKey.split("\\."));
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

        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domain)), virtualFile -> {
            // prevent duplicate targets and dont walk same file twice
            if(virtualFilesFound.contains(virtualFile)) {
                return true;
            }

            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if(psiFile == null) {
                return true;
            }

            if(psiFile instanceof YAMLFile) {
                YamlTranslationVistor.collectFileTranslations((YAMLFile) psiFile, translationCollector);
            } else if(isSupportedXlfFile(psiFile)) {
                // fine: xlf registered as XML file. try to find source value
                psiFoundElements.addAll(getTargetForXlfAsXmlFile((XmlFile) psiFile, translationKey));
            } else if(("xlf".equalsIgnoreCase(virtualFile.getExtension()) || "xliff".equalsIgnoreCase(virtualFile.getExtension()))) {
                // xlf are plain text because not supported by jetbrains
                // for now we can only set file target
                psiFoundElements.addAll(FileBasedIndex.getInstance()
                    .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.filesScope(project, Collections.singletonList(virtualFile))).stream()
                    .filter(string -> string.contains(translationKey)).map(string -> psiFile)
                    .collect(Collectors.toList())
                );
            }

            return true;
        }, GlobalSearchScope.allScope(project));

        return psiFoundElements.toArray(new PsiElement[psiFoundElements.size()]);
    }

    /**
     * Find targets for xlf files if registered as XML
     *
     * 1.2 xliff -> file -> body -> trans-unit -> source
     * 2.0 xliff -> file -> group -> unit -> segment -> source
     * 2.0 xliff -> file -> unit -> segment -> source
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

                    // <trans-unit id="1" resname="title.test">
                    String resname = transUnit.getAttributeValue("resname");
                    if(resname != null && key.equals(resname)) {
                        psiElements.add(transUnit);
                    }
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

            // version="2.0" shortcut
            for (XmlTag unit : file.findSubTags("unit")) {
                for (XmlTag segment : unit.findSubTags("segment")) {
                    consumer.consume(segment);
                }
            }
        }

        return psiElements;
    }

    public static boolean hasDomain(Project project, String domainName) {
        return TranslationIndex.getInstance(project).getTranslationMap().getDomainList().contains(domainName) ||
            FileBasedIndex.getInstance().getValues(
                TranslationStubIndex.KEY,
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

        for(Set<String> keys: FileBasedIndex.getInstance().getValues(TranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project))){
            if(keys.contains(keyName)) {
                return true;
            }
        }

        return false;
    }


    public static List<LookupElement> getTranslationLookupElementsOnDomain(Project project, String domainName) {

        Set<String> keySet = new HashSet<>();
        List<Set<String>> test = FileBasedIndex.getInstance().getValues(TranslationStubIndex.KEY, domainName, GlobalSearchScope.allScope(project));
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

        // attach index domains as weak one
        for(String domainKey: SymfonyProcessors.createResult(project, TranslationStubIndex.KEY, domainList)) {
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

        FileBasedIndex.getInstance().getFilesWithKey(TranslationStubIndex.KEY, new HashSet<>(Collections.singletonList(domainName)), virtualFile -> {
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
    public static Set<String> getXliffTranslations(@NotNull InputStream content) {
        Set<String> set = new HashSet<>();

        visitXliffTranslations(content, pair -> set.add(pair.getFirst()));

        return set;
    }

    private static void visitXliffTranslations(@NotNull InputStream content, @NotNull Consumer<Pair<String, Node>> consumer) {
        Document document;

        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.parse(content);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return;
        }

        if(document == null) {
            return;
        }

        for (String s : XLIFF_XPATH) {
            visitNodes(s, document, consumer);
        }
    }

    public static boolean isSupportedXlfFile(@NotNull PsiFile psiFile) {
        if(!(psiFile instanceof XmlFile)) {
            return false;
        }

        String extension = psiFile.getVirtualFile().getExtension();
        return "xlf".equalsIgnoreCase(extension) || "xliff".equalsIgnoreCase(extension);
    }

    /**
     * Translation placeholder extraction:
     * "%limit%", "{{ limit }}", "{{limit}}",
     * "@username", "!username", "%username"
     */
    @NotNull
    public static Set<String> getPlaceholderFromTranslation(@NotNull String text) {
        Set<String> placeholder = new HashSet<>();

        // best practise
        Matcher matcher = Pattern.compile("(%[^%^\\s]*%)").matcher(text);
        while(matcher.find()){
            placeholder.add(matcher.group(1));
        }

        // validator
        matcher = Pattern.compile("(\\{\\{\\s*[^{]*\\s*}})").matcher(text);
        while(matcher.find()){
            placeholder.add(matcher.group(1));
        }

        // Drupal
        matcher = Pattern.compile("([@|!|%][^\\s][\\w-]*)[\\s]*").matcher(text);
        while(matcher.find()){
            placeholder.add(matcher.group(1));
        }

        return placeholder;
    }

    /**
     * Extract common placeholder pattern from translation content
     */
    @NotNull
    public static Set<String> getPlaceholderFromTranslation(@NotNull Project project, @NotNull String key, @NotNull String domain) {
        Set<String> placeholder = new HashSet<>();
        Set<VirtualFile> visitedXlf = new HashSet<>();

        for (PsiElement element : TranslationUtil.getTranslationPsiElements(project, key, domain)) {
            if (element instanceof YAMLScalar) {
                String textValue = ((YAMLScalar) element).getTextValue();
                if(StringUtils.isBlank(textValue)) {
                    continue;
                }

                placeholder.addAll(
                    TranslationUtil.getPlaceholderFromTranslation(textValue)
                );
            } else if("xlf".equalsIgnoreCase(element.getContainingFile().getVirtualFile().getExtension()) || "xliff".equalsIgnoreCase(element.getContainingFile().getVirtualFile().getExtension())) {
                VirtualFile virtualFile = element.getContainingFile().getVirtualFile();

                // visiting on file scope because we dont rely on xlf and xliff registered as XML file
                // dont visit file twice
                if(!visitedXlf.contains(virtualFile)) {
                    try {
                        visitXliffTranslations(
                            element.getContainingFile().getVirtualFile().getInputStream(),
                            new MyXlfTranslationConsumer(placeholder, key)
                        );
                    } catch (IOException ignored) {
                    }
                }

                visitedXlf.add(virtualFile);
            }
        }

        return placeholder;
    }

    private static void visitNodes(@NotNull String xpath, @NotNull Document document, @NotNull Consumer<Pair<String, Node>> consumer) {
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
                consumer.consume(Pair.create(textContent, node));
            }

            // <trans-unit id="1" resname="title.test">
            Node transUnitNode = node.getParentNode();
            if(transUnitNode != null) {
                NamedNodeMap attributes = transUnitNode.getAttributes();
                if(attributes != null) {
                    Node resname = attributes.getNamedItem("resname");
                    if(resname != null) {
                        String textContentResname = resname.getTextContent();
                        if(textContentResname != null && StringUtils.isNotBlank(textContentResname)) {
                            consumer.consume(Pair.create(textContentResname, node));
                        }
                    }
                }
            }
        }
    }

    /**
     * <trans-unit id="29">
     *  <source>foo</source>
     *  <target>foo</target>
     * </trans-unit>
     */
    private static class MyXlfTranslationConsumer implements Consumer<Pair<String, Node>> {
        @NotNull
        private final Set<String> placeholder;

        @NotNull
        private final String key;

        MyXlfTranslationConsumer(@NotNull Set<String> placeholder, @NotNull String key) {
            this.placeholder = placeholder;
            this.key = key;
        }

        @Override
        public void consume(Pair<String, Node> pair) {
            if(!(pair.getSecond() instanceof Element) || !"source".equalsIgnoreCase(pair.getSecond().getNodeName())) {
                return;
            }

            Element source = (Element) pair.getSecond();
            if(!key.equalsIgnoreCase(source.getTextContent())) {
                return;
            }

            visitNodeText(source);

            Node transUnit = source.getParentNode();
            if(transUnit instanceof Element) {
                NodeList target = ((Element) transUnit).getElementsByTagName("target");
                if(target.getLength() > 0) {
                    visitNodeText(target.item(0));
                }
            }
        }

        private void visitNodeText(@NotNull Node target) {
            String nodeValue = target.getTextContent();
            if(StringUtils.isNotBlank(nodeValue)) {
                placeholder.addAll(
                    TranslationUtil.getPlaceholderFromTranslation(nodeValue)
                );
            }
        }
    }
}
