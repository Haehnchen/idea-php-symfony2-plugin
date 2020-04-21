package fr.adrienbrault.idea.symfony2plugin.translation.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProviderDict;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.collector.YamlTranslationVisitor;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.DomainMappings;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationUtil {
    public static final ExtensionPointName<TranslatorProvider> TRANSLATION_PROVIDER = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.TranslatorProvider");

    public static MethodMatcher.CallToSignature[] PHP_TRANSLATION_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Translation\\TranslatorInterface", "trans"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Translation\\TranslatorInterface", "transChoice"),
        new MethodMatcher.CallToSignature("\\Symfony\\Contracts\\Translation\\TranslatorInterface", "trans"),
        new MethodMatcher.CallToSignature("\\Symfony\\Contracts\\Translation\\TranslatorInterface", "transChoice"),
        new MethodMatcher.CallToSignature("\\Symfony\Bundle\\FrameworkBundle\\Templating\\Helper\\TranslatorHelper", "trans"),
        new MethodMatcher.CallToSignature("\\Symfony\Bundle\\FrameworkBundle\\Templating\\Helper\\TranslatorHelper", "transChoice")
    };

    private static final String[] XLIFF_XPATH = {
        "//xliff/file/body/trans-unit/source",
        "//xliff/file/group/unit/segment/source",
        "//xliff/file/unit/segment/source"
    };

    public static Collection<VirtualFile> getDomainFilesFromCompiledContainer(@NotNull Project project, @NotNull String domainName) {
        DomainMappings domainMappings = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);
        Collection<VirtualFile> virtualFiles = new ArrayList<>();

        for(DomainFileMap domain: domainMappings.getDomainFileMaps()) {
            if(domain.getDomain().equals(domainName)) {
                VirtualFile virtualFile = domain.getFile();
                if(virtualFile != null) {
                    virtualFiles.add(virtualFile);
                }
            }
        }

        return virtualFiles;
    }

    /**
     * Get targets for translation based on the domain path inside the compiled container
     *
     * TODO: completely remove this? support translation paths from service compiler
     */
    public static Collection<PsiElement> getTranslationKeyFromCompiledContainerDomain(@NotNull Project project, @NotNull String domain, @NotNull String translationKey) {
        Collection<PsiElement> psiFoundElements = new ArrayList<>();

        // search for available domain files
        for(PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, TranslationUtil.getDomainFilesFromCompiledContainer(project, domain))) {
            psiFoundElements.addAll(getTranslationKeyTargetInsideFile(psiFile, domain, translationKey));
        }

        return psiFoundElements;
    }

    public static boolean hasDomainInsideCompiledContainer(@NotNull Project project, @NotNull String domainName) {
        DomainMappings domainMappings = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);
        for(DomainFileMap domain: domainMappings.getDomainFileMaps()) {
            if(domain.getDomain().equals(domainName)) {
                return true;
            }
        }

        return false;
    }

    static public Collection<String> getDomainsFromContainer(@NotNull Project project) {
        DomainMappings domainMappings = ServiceXmlParserFactory.getInstance(project, DomainMappings.class);

        return domainMappings.getDomainFileMaps().stream()
            .map(DomainFileMap::getDomain)
            .collect(Collectors.toSet());
    }

    /**
     * Find a target translation key based on all supported formats
     */
    public static Collection<PsiElement> getTranslationKeyTargetInsideFile(@NotNull PsiFile psiFile, @NotNull String domain, @NotNull String translationKey) {
        Set<PsiElement> elements = new HashSet<>();

        if(psiFile instanceof YAMLFile) {
            // collect on yaml keys
            YamlTranslationVisitor.collectFileTranslations((YAMLFile) psiFile, (keyName, yamlKeyValue) -> {
                if (keyName.equals(translationKey)) {
                    // multiline "line values" are not resolve properly on psiElements use key as fallback target
                    PsiElement valuePsiElement = yamlKeyValue.getValue();
                    elements.add(valuePsiElement != null ? valuePsiElement : yamlKeyValue);

                    return false;
                }

                return true;
            });
        } else if(TranslationUtil.isSupportedXlfFile(psiFile)) {
            // fine: xlf registered as XML file. try to find source value
            elements.addAll(TranslationUtil.getTargetForXlfAsXmlFile((XmlFile) psiFile, translationKey));
        } else if(("xlf".equalsIgnoreCase(psiFile.getVirtualFile().getExtension()) || "xliff".equalsIgnoreCase(psiFile.getVirtualFile().getExtension()))) {
            // xlf are plain text because not supported by jetbrains
            // for now we can only set file target
            Project project = psiFile.getProject();
            elements.addAll(FileBasedIndex.getInstance()
                .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.filesScope(project, Collections.singletonList(psiFile.getVirtualFile()))).stream()
                .filter(string -> string.contains(translationKey)).map(string -> psiFile)
                .collect(Collectors.toList())
            );
        }

        return elements;
    }

    public static PsiElement[] getTranslationPsiElements(@NotNull Project project, @NotNull String translationKey, @NotNull String domain) {
        Collection<PsiElement> targets = new HashSet<>();

        Arrays.stream(getTranslationProviders())
            .map(translationProvider -> translationProvider.getTranslationTargets(project, translationKey, domain))
            .forEach(targets::addAll);

        return targets.toArray(new PsiElement[0]);
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

    public static boolean hasDomain(@NotNull Project project, @NotNull String domainName) {
        return Arrays.stream(getTranslationProviders())
            .anyMatch(translatorProvider -> translatorProvider.hasDomain(project, domainName));
    }

    public static boolean hasTranslationKey(@NotNull Project project, String keyName, String domainName) {
        if(!hasDomain(project, domainName)) {
            return false;
        }

        return Arrays.stream(getTranslationProviders())
            .anyMatch(translatorProvider -> translatorProvider.hasTranslationKey(project, keyName, domainName));
    }

    @NotNull
    public static List<LookupElement> getTranslationLookupElementsOnDomain(@NotNull Project project, @NotNull String domainName) {
        List<LookupElement> lookupElements = new ArrayList<>();

        Map<String, Boolean> keys = new HashMap<>();

        for (TranslatorProvider translationProvider : getTranslationProviders()) {
            for (TranslatorProviderDict.TranslationKey translationKey : translationProvider.getTranslationsForDomain(project, domainName)) {
                String domain = translationKey.getDomain();
                if (keys.containsKey(domain)) {
                    // weak to full
                    if(!keys.get(domain) && !translationKey.isWeak()) {
                        keys.put(domain, translationKey.isWeak());
                    }
                } else {
                    keys.put(domain, translationKey.isWeak());
                }
            }
        }

        // fallback on index
        for(Map.Entry<String, Boolean> entry : keys.entrySet()) {
            lookupElements.add(new TranslatorLookupElement(entry.getKey(), domainName, entry.getValue()));
        }

        return lookupElements;
    }

    @NotNull
    public static List<LookupElement> getTranslationDomainLookupElements(@NotNull Project project) {
        Map<String, Boolean> domains = new HashMap<>();

        for (TranslatorProvider translationProvider : getTranslationProviders()) {
            for (TranslatorProviderDict.TranslationDomain translationDomain : translationProvider.getTranslationDomains(project)) {
                String domain = translationDomain.getDomain();
                if (domains.containsKey(domain)) {
                    // weak to full
                    if(!domains.get(domain) && !translationDomain.isWeak()) {
                        domains.put(domain, translationDomain.isWeak());
                    }
                } else {
                    domains.put(domain, translationDomain.isWeak());
                }
            }
        }

        return domains.entrySet().stream()
            .map((Function<Map.Entry<String, Boolean>, LookupElement>) entry ->
                new TranslatorLookupElement(entry.getKey(), entry.getKey(), entry.getValue())
            ).collect(Collectors.toList());
    }

    public static List<PsiFile> getDomainPsiFiles(@NotNull Project project, @NotNull String domainName) {
        Set<VirtualFile> files = new HashSet<>();

        for (TranslatorProvider translationProvider : getTranslationProviders()) {
            files.addAll(translationProvider.getDomainPsiFiles(project, domainName));
        }

        return new ArrayList<>(PsiElementUtils.convertVirtualFilesToPsiFiles(project, files));
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

        // Simple parser for MessageFormat by the ICU project
        matcher = Pattern.compile("(\\{\\s*[^{]*\\s*})").matcher(text);
        while(matcher.find()){
            // Keep the whole placeholder for consistency with other formats, but also add just the placeholder name
            // as this is allowed with the ICU format.
            placeholder.add(matcher.group(1));
            placeholder.add(matcher.group(1).replace("{", "").replace("}", "").trim());
        }

        return placeholder;
    }

    private static TranslatorProvider[] getTranslationProviders() {
        return TRANSLATION_PROVIDER.getExtensions();
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
