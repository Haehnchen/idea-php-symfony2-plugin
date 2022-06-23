package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlGoToDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        Collection<PsiElement> targets = new HashSet<>();

        IElementType elementType = psiElement.getNode().getElementType();

        // only string values like "foo", foo
        if (elementType == YAMLTokenTypes.TEXT || elementType == YAMLTokenTypes.SCALAR_DSTRING || elementType == YAMLTokenTypes.SCALAR_STRING) {
            String psiText = PsiElementUtils.getText(psiElement);
            if (psiText.length() > 0) {
                if (psiText.startsWith("@") && psiText.length() > 1) {
                    targets.addAll(serviceGoToDeclaration(psiElement, psiText.substring(1)));
                }

                // match: %annotations.reader.class%
                if(psiText.length() > 3 && psiText.startsWith("%") && psiText.endsWith("%")) {
                    targets.addAll(parameterGoToDeclaration(psiElement, psiText));
                }

                if(psiText.startsWith("!php/const:")) {
                    targets.addAll(constantGoto(psiElement, psiText));
                }

                // mind the whitespace
                if(hasNewConst(psiElement)) {
                    targets.addAll(newConstantGoto(psiElement, psiText));
                }

                if(psiText.contains("\\")) {
                    targets.addAll(classGoToDeclaration(psiElement, psiText)) ;
                }

                if(psiText.endsWith(".twig") || psiText.endsWith(".php")) {
                    targets.addAll(TwigUtil.getTemplatePsiElements(psiElement.getProject(), psiText));
                }

                if(psiText.matches("^[\\w_.]+") && getGlobalServiceStringPattern().accepts(psiElement)) {
                    targets.addAll(serviceGoToDeclaration(psiElement, psiText));
                }
            }
        }

        if (elementType == YAMLTokenTypes.SCALAR_KEY) {
            String psiText = PsiElementUtils.getText(psiElement);

            if (psiText.startsWith("$")) {
                targets.addAll(namedArgumentGoto(psiElement));

                // services:
                //      _defaults:
                //          bind:
                //              $fo<caret>obar: '@foobar'
                if (YamlElementPatternHelper.getBindArgumentPattern().accepts(psiElement)) {
                    targets.addAll(namedDefaultBindArgumentGoto(psiElement, psiText));
                }
            }
        }

        // yaml Plugin BC: "!php/const:" is a tag
        if (elementType == YAMLTokenTypes.TAG) {
            String psiText = PsiElementUtils.getText(psiElement);
            if (psiText.startsWith("!php/const:")) {
                targets.addAll(constantGoto(psiElement, psiText));
            }
        }


        if(YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller").accepts(psiElement)) {
            targets.addAll(getControllerGoto(psiElement));
        }

        if(YamlElementPatternHelper.getSingleLineScalarKey("class").accepts(psiElement)) {
            targets.addAll(getClassGoto(psiElement));
        }

        if(YamlElementPatternHelper.getSingleLineScalarKey("resource").accepts(psiElement)) {
            targets.addAll(attachResourceBundleGoto(psiElement));
            targets.addAll(attachResourceOnPathGoto(psiElement));
        }

        // tags: { name: foobar }
        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("name")
        ).accepts(psiElement)) {
            targets.addAll(getTagClassesGoto(psiElement));
        }

        // tags: [ name: foobar ]
        if(YamlElementPatternHelper.getSequenceValueWithArrayKeyPattern("tags").accepts(psiElement)) {
            targets.addAll(getTagClassesGoto(psiElement));
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("event")
        ).accepts(psiElement)) {
            targets.addAll(getEventGoto(psiElement));
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("calls")
        ).accepts(psiElement)) {
            targets.addAll(getMethodGoto(psiElement));
        }

        if(StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ).accepts(psiElement)) {
            targets.addAll(getTagMethodGoto(psiElement));
        }

        // ["@service", method]
        if(YamlElementPatternHelper.getAfterCommaPattern().accepts(psiElement)) {
            targets.addAll(getArrayMethodGoto(psiElement));
        }

        // config.yml: "as<caret>setic": ~
        if(PlatformPatterns.psiElement().inFile(YamlElementPatternHelper.getConfigFileNamePattern()).accepts(psiElement)) {
            targets.addAll(visitConfigKey(psiElement));
        }

        // factory: "service:method"
        if(YamlElementPatternHelper.getSingleLineScalarKey("factory").accepts(psiElement)) {
            targets.addAll(getFactoryStringGoto(psiElement));
        }

        // _instanceof:
        //   My<caret>Class: ~
        // services:
        //   My<caret>Class: ~
        if(YamlElementPatternHelper.getServicesKeyPattern().accepts(psiElement)) {
            targets.addAll(getClassesForServiceKey(psiElement));
        }

        // services:
        //  Foo\\:
        //   resource: '../src/'
        //   exclude:
        //      - '../src/DependencyInjection/'
        if (StandardPatterns.or(
            YamlElementPatternHelper.getSingleLineScalarKey("exclude", "resource"),
            YamlElementPatternHelper.getSequenceValueWithArrayKeyPattern("exclude", "resource")
        ).accepts(psiElement)) {
            targets.addAll(attachResourceOrExcludeGlobNamespaceElements(psiElement));
        }

        // !tagged_iterator
        if (PlatformPatterns.psiElement(YAMLTokenTypes.TAG).accepts(psiElement)) {
            targets.addAll(attachTaggedIteratorClasses(psiElement));
        }

        return targets.toArray(new PsiElement[0]);
    }

    private Collection<? extends PsiElement> namedDefaultBindArgumentGoto(@NotNull PsiElement psiElement, @NotNull String parameterName) {
        Collection<PsiElement> psiElements = new HashSet<>();

        String argumentWithoutDollar = parameterName.substring(1);
        ServiceContainerUtil.visitNamedArguments(psiElement.getContainingFile(), pair -> {
            Parameter parameter = pair.getFirst();
            if (parameter.getName().equals(argumentWithoutDollar)) {
                psiElements.add(parameter);
            }
        });

        return psiElements;
    }

    private Collection<? extends PsiElement> namedArgumentGoto(@NotNull PsiElement psiElement) {
        Collection<PsiElement> psiElements = new HashSet<>();

        Parameter yamlNamedArgument = ServiceContainerUtil.getYamlNamedArgument(psiElement, new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject()));
        if (yamlNamedArgument != null) {
            psiElements.add(yamlNamedArgument);
        }

        return psiElements;
    }

    private boolean hasNewConst(@NotNull PsiElement psiElement) {
        PsiElement prevSibling = psiElement.getPrevSibling();
        while (prevSibling != null) {
            IElementType elementType = prevSibling.getNode().getElementType();
            if (elementType == YAMLTokenTypes.TEXT || elementType == YAMLTokenTypes.SCALAR_DSTRING || elementType == YAMLTokenTypes.SCALAR_STRING || elementType == YAMLTokenTypes.TAG) {
                String psiText = PsiElementUtils.getText(prevSibling);

                return psiText.equals("!php/const");
            }

            prevSibling = prevSibling.getPrevSibling();
        }

        return false;
    }

    @NotNull
    private Collection<PsiElement> classGoToDeclaration(@NotNull PsiElement psiElement, @NotNull String className) {

        Collection<PsiElement> psiElements = new HashSet<>();

        // Class::method
        // Class::FooAction
        // Class:Foo
        if(className.contains(":")) {
            String[] split = className.replaceAll("(:)\\1", "$1").split(":");
            if(split.length == 2) {
                for(String append: new String[] {"", "Action"}) {
                    Method classMethod = PhpElementsUtil.getClassMethod(psiElement.getProject(), split[0], split[1] + append);
                    if(classMethod != null) {
                        psiElements.add(classMethod);
                    }
                }
            }

            return psiElements;
        }

        // ClassName
        psiElements.addAll(PhpElementsUtil.getClassesInterface(psiElement.getProject(), className));
        return psiElements;
    }

    @NotNull
    private Collection<PsiElement> serviceGoToDeclaration(@NotNull PsiElement psiElement, @NotNull String serviceId) {
        serviceId = YamlHelper.trimSpecialSyntaxServiceName(serviceId).toLowerCase();

        String serviceClass = ContainerCollectionResolver.resolveService(psiElement.getProject(), serviceId);

        if (serviceClass != null) {
            Collection<PhpClass> phpClasses = PhpIndex.getInstance(psiElement.getProject()).getAnyByFQN(serviceClass);
            if(phpClasses.size() > 0) {
                return new ArrayList<>(phpClasses);
            }
        }

        // get container target on indexes
        return ServiceIndexUtil.findServiceDefinitions(psiElement.getProject(), serviceId);

    }

    @NotNull
    private Collection<PsiElement> parameterGoToDeclaration(@NotNull PsiElement psiElement, @NotNull String psiParameterName) {
        Collection<PsiElement> targets = new ArrayList<>(
            DotEnvUtil.getEnvironmentVariableTargetsForParameter(psiElement.getProject(), psiParameterName)
        );

        if(!YamlHelper.isValidParameterName(psiParameterName)) {
            return targets;
        }

        targets.addAll(ServiceUtil.getServiceClassTargets(psiElement.getProject(), psiParameterName));

        return targets;
    }

    @NotNull
    private Collection<PsiElement> constantGoto(@NotNull PsiElement psiElement, @NotNull String tagName) {
        String constantName = tagName.substring(11);
        if(StringUtils.isBlank(constantName)) {
            return Collections.emptyList();
        }

        return ServiceContainerUtil.getTargetsForConstant(psiElement.getProject(), constantName);
    }

    @NotNull
    private Collection<PsiElement> newConstantGoto(@NotNull PsiElement psiElement, @NotNull String constantName) {
        if(StringUtils.isBlank(constantName)) {
            return Collections.emptyList();
        }

        return ServiceContainerUtil.getTargetsForConstant(psiElement.getProject(), constantName);
    }

    @NotNull
    private Collection<PsiElement> visitConfigKey(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return Collections.emptyList();
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();
        if(StringUtils.isBlank(keyText)) {
            return Collections.emptyList();
        }

        return ConfigUtil.getTreeSignatureTargets(psiElement.getProject(), keyText);
    }

    @NotNull
    private Collection<PsiElement> getArrayMethodGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        String service = YamlHelper.getPreviousSequenceItemAsText(psiElement);
        if (service == null) {
            return Collections.emptyList();
        }

        PhpClass phpClass = ServiceUtil.getServiceClass(psiElement.getProject(), service);
        if(phpClass == null) {
            return Collections.emptyList();
        }

        Collection<PsiElement> results = new ArrayList<>();

        for (Method method : phpClass.getMethods()) {
            if(text.equals(method.getName())) {
                results.add(method);
            }
        }

        return results;
    }

    /**
     * Factory goto: "factory: 'foo:bar'"
     */
    @NotNull
    private Collection<PsiElement> getFactoryStringGoto(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLScalar)) {
            return Collections.emptyList();
        }

        String textValue = ((YAMLScalar) parent).getTextValue();
        String[] split = textValue.split(":");
        if(split.length != 2) {
            return Collections.emptyList();
        }

        PhpClass phpClass = ServiceUtil.getServiceClass(psiElement.getProject(), split[0]);
        if(phpClass == null) {
            return Collections.emptyList();
        }

        Collection<PsiElement> results = new ArrayList<>();

        for (Method method : phpClass.getMethods()) {
            if(split[1].equals(method.getName())) {
                results.add(method);
            }
        }

        return results;
    }

    @NotNull
    private Collection<PsiElement> getClassGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        return new ArrayList<>(PhpElementsUtil.getClassesInterface(psiElement.getProject(), text));
    }

    @NotNull
    private Collection<PsiElement> getMethodGoto(@NotNull PsiElement psiElement) {
        Collection<PsiElement> results = new ArrayList<>();

        PsiElement parent = psiElement.getParent();

        if(parent instanceof YAMLScalar) {
            YamlHelper.visitServiceCall((YAMLScalar) parent, clazz -> {
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(),clazz);
                if(phpClass != null) {
                    for(Method method: PhpElementsUtil.getClassPublicMethod(phpClass)) {
                        if(method.getName().equals(PsiElementUtils.trimQuote(psiElement.getText()))) {
                            results.add(method);
                        }
                    }
                }
            });
        }

        return results;
    }

    @NotNull
    private Collection<PsiElement> getTagMethodGoto(@NotNull PsiElement psiElement) {
        String methodName = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(methodName)) {
            return Collections.emptyList();
        }

        String classValue = YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement);
        if(classValue == null) {
            return Collections.emptyList();
        }

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classValue);
        if(phpClass == null) {
            return Collections.emptyList();
        }

        Method method = phpClass.findMethodByName(methodName);
        if(method != null) {
            return Collections.singletonList(method);
        }

        return Collections.emptyList();
    }

    @NotNull
    private Collection<PsiElement> attachResourceBundleGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> results = new ArrayList<>();

        results.addAll(FileResourceUtil.getFileResourceTargetsInBundleScope(psiElement.getProject(), text));
        results.addAll(FileResourceUtil.getFileResourceTargetsInBundleDirectory(psiElement.getProject(), text));

        return results;

    }

    /**
     * services:
     *   Foo\\:
     *     resource: '../src/'
     *     exclude: ...
     */
    @NotNull
    private Collection<PsiElement> attachResourceOrExcludeGlobNamespaceElements(@NotNull PsiElement psiElement) {
        String resource = PsiElementUtils.trimQuote(psiElement.getText());
        if (StringUtils.isBlank(resource)) {
            return Collections.emptyList();
        }

        PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile == null) {
            return Collections.emptyList();
        }

        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return Collections.emptyList();
        }

        PsiElement quotedText = psiElement.getParent();
        if (!(quotedText instanceof YAMLQuotedText)) {
            return Collections.emptyList();
        }

        String name = YamlHelper.getServiceKeyFromResourceFromStringOrArray((YAMLQuotedText) quotedText);
        if (name == null) {
            return Collections.emptyList();
        }

        return new HashSet<>(
            ServiceContainerUtil.getPhpClassFromResources(psiElement.getProject(), name, virtualFile, List.of(resource), Collections.emptyList())
        );
    }

    @NotNull
    private Collection<PsiElement> attachResourceOnPathGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text) || text.startsWith("@")) {
            return Collections.emptyList();
        }

        PsiFile containingFile = psiElement.getContainingFile();
        if(containingFile == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(FileResourceUtil.getFileResourceTargetsInDirectoryScope(containingFile, text));
    }

    /**
     * Click on "tagged_iterator"
     *
     * "!tagged_iterator app.handler"
     */
    @NotNull
    private Collection<PsiElement> attachTaggedIteratorClasses(@NotNull PsiElement psiElement) {
        if (psiElement.getNode().getElementType() == YAMLTokenTypes.TAG) {
            // - !tagged_iterator ...

            if ("!tagged_iterator".equals(psiElement.getText())) {
                PsiElement nextLeaf = PsiTreeUtil.nextCodeLeaf(psiElement);

                if (nextLeaf != null && nextLeaf.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                    // - !tagged_iterator app.handler
                    String tag = nextLeaf.getText();
                    if (StringUtils.isNotBlank(tag)) {
                        return new ArrayList<>(ServiceUtil.getTaggedClassesWithCompiled(psiElement.getProject(), tag));
                    }
                } else {
                    // - !tagged_iterator { tag: app.handler, default_priority_method: getPriority }
                    YAMLKeyValue tagKeyValue = PsiElementUtils.getNextSiblingOfTypes(psiElement, YAMLKeyValue.class)
                        .stream()
                        .filter(yamlKeyValue -> "tag".equals(yamlKeyValue.getKeyText()))
                        .findFirst()
                        .orElse(null);

                    if (tagKeyValue != null) {
                        String tag = tagKeyValue.getValueText();
                        if (StringUtils.isNotBlank(tag)) {
                            return new ArrayList<>(ServiceUtil.getTaggedClassesWithCompiled(psiElement.getProject(), tag));
                        }
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    @NotNull
    private Collection<PsiElement> getControllerGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        return Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), text));
    }

    @NotNull
    private Collection<PsiElement> getTagClassesGoto(@NotNull PsiElement psiElement) {
        String tagName = PsiElementUtils.trimQuote(psiElement.getText());
        if(StringUtils.isBlank(tagName)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(ServiceUtil.getTaggedClassesWithCompiled(psiElement.getProject(), tagName));
    }

    private Collection<PsiElement> getEventGoto(@NotNull PsiElement psiElement) {
        return EventDispatcherSubscriberUtil.getEventPsiElements(psiElement.getProject(), PsiElementUtils.trimQuote(psiElement.getText()));
    }

    /**
     * services:
     *   My<caret>Class: ~
     */
    private static Collection<PsiElement> getClassesForServiceKey(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();
        if(parent instanceof YAMLKeyValue) {
            return getClassesForServiceKey((YAMLKeyValue) parent);
        }

        return Collections.emptyList();
    }

    @NotNull
    public static Collection<PsiElement> getClassesForServiceKey(@NotNull YAMLKeyValue yamlKeyValue) {
        String valueText = yamlKeyValue.getKeyText();
        if (StringUtils.isBlank(valueText)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new HashSet<>();

        // My<caret>Class\:
        //   resource: '....'
        //   exclude: '....'
        if (valueText.endsWith("\\")) {
            targets.addAll(YamlHelper.getNamespaceResourcesClasses(yamlKeyValue));
        }

        targets.addAll(PhpElementsUtil.getClassesInterface(yamlKeyValue.getProject(), valueText));

        return targets;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

    /**
     * foo: b<caret>ar
     * foo: [ b<caret>ar ]
     * foo: { b<caret>ar }
     * foo:
     *  - b<caret>ar
     */
    private PsiElementPattern.Capture<PsiElement> getGlobalServiceStringPattern() {
        return PlatformPatterns.psiElement().withParent(
                PlatformPatterns.psiElement(YAMLScalar.class).withParent(PlatformPatterns.or(
                        PlatformPatterns.psiElement(YAMLKeyValue.class),
                        PlatformPatterns.psiElement(YAMLSequenceItem.class),
                        PlatformPatterns.psiElement(YAMLMapping.class)
                )));
    }
}
