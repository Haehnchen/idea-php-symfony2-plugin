package fr.adrienbrault.idea.symfony2plugin.action;


import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceArgumentSelectionDialog;
import fr.adrienbrault.idea.symfony2plugin.action.ui.SymfonyCreateService;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceArgumentInspection;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlCreateServiceArgumentsCallback;
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.dict.YamlUpdateArgumentServicesCallback;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceActionUtil {

    /**
     * Attributes which we should not support in missing arguments constructors for server definition
     */
    public static final String[] INVALID_ARGUMENT_ATTRIBUTES = getInvalidArgumentAttributes();

    private static String[] getInvalidArgumentAttributes() {
        return Arrays.stream(YamlServiceArgumentInspection.INVALID_KEYS)
            .map(s -> s.replace("_", "-"))
            .toArray(String[]::new);
    }

    public static void buildFile(@NotNull AnActionEvent event, @NotNull Project project, @NotNull String templatePath) {
        String extension;

        if ((templatePath.endsWith(".yml") || templatePath.endsWith(".yaml"))) {
            extension = "yaml";
        } else if (templatePath.endsWith(".xml")) {
            extension = "xml";
        } else if (templatePath.endsWith(".php")) {
            extension = "php";
        } else {
            throw new RuntimeException("no valid extension for: " + templatePath);
        }

        String fileName = Messages.showInputDialog(project, "File name (without extension)", String.format("Create %s Service", extension), Symfony2Icons.SYMFONY);
        if(fileName == null || StringUtils.isBlank(fileName)) {
            return;
        }

        final PsiDirectory parentDirectory = NewFileActionUtil.getSelectedDirectoryFromAction(event);
        if (parentDirectory == null) {
            return;
        }

        if(parentDirectory.findFile(fileName) != null) {
            Messages.showInfoMessage("File exists", "Error");
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            PsiFile fileFromText = PsiFileFactory.getInstance(project).createFileFromText("services." + extension, PhpFileType.INSTANCE, NewFileActionUtil.getFileTemplateContent(templatePath));

            if (extension.equals("php")) {
                CodeStyleManager.getInstance(project).reformat(fileFromText);
            }

            PsiElement newFile = PsiDirectoryFactory.getInstance(project).createDirectory(parentDirectory.getVirtualFile()).add(fileFromText);
            new OpenFileDescriptor(project, newFile.getContainingFile().getVirtualFile(), 0).navigate(true);
        });
    }

    @NotNull
    public static Set<String> getPossibleServices(@NotNull Project project, @NotNull String type, @NotNull Map<String, ContainerService> serviceClasses) {
        PhpClass typeClass = PhpElementsUtil.getClassInterface(project, type);
        if(typeClass == null) {
            return Collections.emptySet();
        }

        return getPossibleServices(typeClass, serviceClasses);
    }

    @NotNull
    public static Set<String> getPossibleServices(@NotNull PhpClass phpClass, @NotNull Map<String, ContainerService> serviceClasses) {
        List<ContainerService> matchedContainer = new ArrayList<>(ServiceUtil.getServiceSuggestionForPhpClass(phpClass, serviceClasses));
        if(matchedContainer.isEmpty()) {
            return Collections.emptySet();
        }

        // weak service have lower priority
        matchedContainer.sort(new SymfonyCreateService.ContainerServicePriorityWeakComparator());

        // lower priority of services like "doctrine.orm.default_entity_manager"
        matchedContainer.sort(new SymfonyCreateService.ContainerServicePriorityNameComparator());

        matchedContainer.sort((o1, o2) ->
            Integer.compare(ServiceContainerUtil.getServiceUsage(phpClass.getProject(), o2.getName()), ServiceContainerUtil.getServiceUsage(phpClass.getProject(), o1.getName()))
        );

        return matchedContainer.stream()
            .map(ContainerService::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static class ServiceYamlContainer {

        @NotNull
        private final YAMLKeyValue serviceKey;
        @Nullable
        private final YAMLKeyValue argument;

        @NotNull
        private final String className;

        public ServiceYamlContainer(@NotNull YAMLKeyValue serviceKey, @Nullable YAMLKeyValue argument, @NotNull String className) {
            this.serviceKey = serviceKey;
            this.argument = argument;
            this.className = className;
        }

        @Nullable
        public YAMLKeyValue getArgument() {
            return argument;
        }

        @NotNull
        public String getClassName() {
            return className;
        }

        @NotNull
        public YAMLKeyValue getServiceKey() {
            return serviceKey;
        }

        /**
         * fo<caret>o:
         *   class: foo
         *   arguments: []
         */
        @Nullable
        public static ServiceYamlContainer create(@NotNull YAMLKeyValue yamlServiceKeyValue) {
            YAMLMapping childOfType = PsiTreeUtil.getChildOfType(yamlServiceKeyValue, YAMLMapping.class);
            if(childOfType == null) {
                return null;
            }

            String serviceClass = null;

            YAMLKeyValue aClass = childOfType.getKeyValueByKey("class");
            if(aClass != null) {
                // service:
                //  class: Foobar
                YAMLValue value = aClass.getValue();
                if(value instanceof YAMLScalar) {
                    serviceClass =  ((YAMLScalar) value).getTextValue();
                }
            } else {
                // Foobar:
                //  argument: ~
                String keyText = yamlServiceKeyValue.getKeyText();

                if(StringUtils.isNotBlank(keyText) && YamlHelper.isClassServiceId(keyText)) {
                    serviceClass = keyText;
                }
            }

            if (StringUtils.isBlank(serviceClass)) {
                return null;
            }

            return new ServiceYamlContainer(yamlServiceKeyValue, childOfType.getKeyValueByKey("arguments"), serviceClass);
        }

    }

    /**
     * Gets all services inside yaml file with "arguments" key context
     */
    @NotNull
    public static Collection<ServiceYamlContainer> getYamlContainerServiceArguments(@NotNull YAMLFile yamlFile) {

        Collection<ServiceYamlContainer> services = new ArrayList<>();

        for(YAMLKeyValue yamlKeyValue : YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            ServiceYamlContainer serviceYamlContainer = ServiceYamlContainer.create(yamlKeyValue);
            if(serviceYamlContainer != null) {
                services.add(serviceYamlContainer);
            }
        }

        return services;
    }

    @NotNull
    public static List<String> getXmlMissingArgumentTypes(@NotNull XmlTag xmlTag, boolean collectOptionalParameter, @NotNull ContainerCollectionResolver.LazyServiceCollector collector) {
        PhpClass resolvedClassDefinition = getPhpClassFromXmlTag(xmlTag, collector);
        if (resolvedClassDefinition == null) {
            return Collections.emptyList();
        }

        Method constructor = resolvedClassDefinition.getConstructor();
        if(constructor == null) {
            return Collections.emptyList();
        }

        int serviceArguments = 0;

        for (XmlTag tag : xmlTag.getSubTags()) {
            if("argument".equals(tag.getName())) {
                serviceArguments++;
            }
        }

        Parameter[] parameters = collectOptionalParameter ? constructor.getParameters() : PhpElementsUtil.getFunctionRequiredParameter(constructor);
        if(parameters.length <= serviceArguments) {
            return Collections.emptyList();
        }

        final List<String> args = new ArrayList<>();

        for (int i = serviceArguments; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String s = parameter.getDeclaredType().toString();
            args.add(s);
        }

        return args;
    }

    @Nullable
    public static PhpClass getPhpClassFromXmlTag(@NotNull XmlTag xmlTag, @NotNull ContainerCollectionResolver.LazyServiceCollector collector) {
        String className = xmlTag.getAttributeValue("class");
        if(className == null) {
            String id = xmlTag.getAttributeValue("id");
            if(id == null || !YamlHelper.isClassServiceId(id)) {
                return null;
            }

            className = id;
        }

        // @TODO: cache defs
        PhpClass resolvedClassDefinition = ServiceUtil.getResolvedClassDefinition(xmlTag.getProject(), className, collector);
        if(resolvedClassDefinition == null) {
            return null;
        }

        return resolvedClassDefinition;
    }

    @NotNull
    public static List<String> getYamlMissingArgumentTypes(Project project, ServiceActionUtil.ServiceYamlContainer container, boolean collectOptionalParameter, @NotNull ContainerCollectionResolver.LazyServiceCollector collector) {
        PhpClass resolvedClassDefinition = ServiceUtil.getResolvedClassDefinition(project, container.getClassName(), collector);
        if(resolvedClassDefinition == null) {
            return Collections.emptyList();
        }

        Method constructor = resolvedClassDefinition.getConstructor();
        if(constructor == null) {
            return Collections.emptyList();
        }

        int serviceArguments = -1;
        if(container.getArgument() != null) {
            PsiElement yamlCompoundValue = container.getArgument().getValue();

            if(yamlCompoundValue instanceof YAMLCompoundValue) {
                List<PsiElement> yamlArrayOnSequenceOrArrayElements = YamlHelper.getYamlArrayOnSequenceOrArrayElements((YAMLCompoundValue) yamlCompoundValue);
                if(yamlArrayOnSequenceOrArrayElements != null) {
                    serviceArguments = yamlArrayOnSequenceOrArrayElements.size();
                }
            }

        } else {
            serviceArguments = 0;
        }

        if(serviceArguments == -1) {
            return Collections.emptyList();
        }

        Parameter[] parameters = collectOptionalParameter ? constructor.getParameters() : PhpElementsUtil.getFunctionRequiredParameter(constructor);
        if(parameters.length <= serviceArguments) {
            return Collections.emptyList();
        }

        final List<String> args = new ArrayList<>();

        for (int i = serviceArguments; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String s = parameter.getDeclaredType().toString();
            args.add(s);
        }

        return args;
    }

    public static boolean isValidXmlParameterInspectionService(@NotNull XmlTag xmlTag) {
        if (!"service".equals(xmlTag.getName())) {
            return false;
        }

        // we dont support some attributes right now
        for(String s : INVALID_ARGUMENT_ATTRIBUTES) {
            if(xmlTag.getAttribute(s) != null) {
                return false;
            }
        }

        // <service autowire="[false|true]"/>
        String autowire = xmlTag.getAttributeValue("autowire");
        if("true".equalsIgnoreCase(autowire)) {
            return false;
        } else if("false".equalsIgnoreCase(autowire)) {
            return true;
        }

        // <service><factory/></service>
        // symfony2 >= 2.6
        if(xmlTag.findSubTags("factory").length > 0) {
            return false;
        }

        // <services autowire="true"><defaults/></services>
        PsiElement servicesTag = xmlTag.getParent();
        if(servicesTag instanceof XmlTag && "services".equals(((XmlTag) servicesTag).getName())) {
            // <defaults autowire="true" />
            for (XmlTag defaults : ((XmlTag) servicesTag).findSubTags("defaults")) {
                if("true".equalsIgnoreCase(defaults.getAttributeValue("autowire"))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void fixServiceArgument(@NotNull List<String> args, final @NotNull XmlTag xmlTag) {
        fixServiceArgument(xmlTag.getProject(), args, new XmlInsertServicesCallback(xmlTag));
    }

    public static void fixServiceArgument(@NotNull YAMLKeyValue yamlKeyValue) {
        YAMLKeyValue argumentsKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, "arguments");

        List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(
            yamlKeyValue.getProject(),
            ServiceActionUtil.ServiceYamlContainer.create(yamlKeyValue),
            false,
            new ContainerCollectionResolver.LazyServiceCollector(yamlKeyValue.getProject())
        );

        if(yamlMissingArgumentTypes.isEmpty()) {
            return;
        }

        InsertServicesCallback insertServicesCallback;

        if(argumentsKeyValue == null) {
            // there is no "arguments" key so provide one
            insertServicesCallback = new YamlCreateServiceArgumentsCallback(yamlKeyValue);
        } else {
            insertServicesCallback = new YamlUpdateArgumentServicesCallback(
                yamlKeyValue.getProject(),
                argumentsKeyValue,
                yamlKeyValue
            );
        }

        ServiceActionUtil.fixServiceArgument(yamlKeyValue.getProject(), yamlMissingArgumentTypes, insertServicesCallback);
    }

    public static void fixServiceArgument(@NotNull Project project, @NotNull List<String> args, final @NotNull InsertServicesCallback callback) {

        Map<String, ContainerService> services = ContainerCollectionResolver.getServices(project);

        Map<String, Set<String>> resolved = new LinkedHashMap<>();
        for (String arg : args) {
            resolved.put(arg, ServiceActionUtil.getPossibleServices(project, arg, services));
        }

        // we got an unique service list, not need to provide ui
        if(isUniqueServiceMap(resolved)) {
            List<String> items = new ArrayList<>();
            for (Map.Entry<String, Set<String>> stringSetEntry : resolved.entrySet()) {
                Set<String> value = stringSetEntry.getValue();
                if(!value.isEmpty()) {
                    items.add(value.iterator().next());
                } else {
                    items.add("?");
                }
            }

            ApplicationManager.getApplication().runWriteAction(() -> callback.insert(items));

            return;
        }

        ServiceArgumentSelectionDialog.createDialog(project, resolved, callback::insert);
    }

    public static boolean isUniqueServiceMap(Map<String, Set<String>> resolvedServices) {

        for (Map.Entry<String, Set<String>> stringSetEntry : resolvedServices.entrySet()) {
            if(stringSetEntry.getValue().size() > 1) {
                return false;
            }
        }

        return true;

    }

    public static void addServices(List<String> items, XmlTag xmlTag) {
        for (String item : items) {

            if(StringUtils.isBlank(item)) {
                item = "?";
            }

            XmlTag tag = XmlElementFactory.getInstance(xmlTag.getProject()).createTagFromText(String.format("<argument type=\"service\" id=\"%s\"/>", item), xmlTag.getLanguage());
            xmlTag.addSubTag(tag, false);
        }
    }

    public interface InsertServicesCallback {
        void insert(List<String> items);
    }

    public static class XmlInsertServicesCallback implements InsertServicesCallback {

        @NotNull
        private final XmlTag xmlTag;

        public XmlInsertServicesCallback(final @NotNull XmlTag xmlTag) {
            this.xmlTag = xmlTag;
        }

        @Override
        public void insert(List<String> items) {
            addServices(items, this.xmlTag);
        }

    }

}
