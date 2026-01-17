package fr.adrienbrault.idea.symfony2plugin.mcp.service;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.action.ui.MethodParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Generates Symfony service definitions in YAML or XML format based on a class name.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceDefinitionGenerator {

    private final Project project;

    public ServiceDefinitionGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * Generates a service definition for the given class.
     * Validation should be handled by the caller (e.g., MCP tool).
     *
     * @param className The fully qualified class name (FQN) - should be validated by caller
     * @param outputType The output format (YAML or XML)
     * @param useClassNameAsId If true, use class name as service ID; if false, use generated snake_case service name
     * @return The generated service definition as a string, or null if generation fails
     */
    @Nullable
    public String generate(@NotNull String className, @NotNull OutputType outputType, boolean useClassNameAsId) {
        // Normalize class name (strip leading backslash)
        className = StringUtils.stripStart(className, "\\");

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if (phpClass == null) {
            return null;
        }

        // Get default service name (snake_case generated from class name)
        String serviceName = ServiceUtil.getServiceNameForClass(project, className);

        // Collect all services for type resolution
        Map<String, ContainerService> serviceClass = ContainerCollectionResolver.getServices(project);
        Set<String> serviceSetComplete = new TreeSet<>();
        serviceSetComplete.add("");
        serviceSetComplete.addAll(serviceClass.keySet());

        // Build method parameters
        List<MethodParameter.MethodModelParameter> modelParameters = collectMethodParameters(phpClass, serviceClass, serviceSetComplete);

        // Sort parameters
        modelParameters.sort(
            Comparator
                .comparing(MethodParameter.MethodModelParameter::getName)
                .thenComparingInt(MethodParameter.MethodModelParameter::getIndex)
        );

        // Build the service definition
        ServiceBuilder.OutputType builderOutputType = outputType == OutputType.YAML
            ? ServiceBuilder.OutputType.Yaml
            : ServiceBuilder.OutputType.XML;

        // Pass useClassNameAsId to ServiceBuilder (inverted logic: isClassAsIdAttribute)
        ServiceBuilder builder = new ServiceBuilder(modelParameters, project, useClassNameAsId);
        return builder.build(builderOutputType, className, serviceName);
    }

    /**
     * Generates a service definition for the given class using class name as ID (default).
     * Validation should be handled by the caller (e.g., MCP tool).
     *
     * @param className The fully qualified class name (FQN) - should be validated by caller
     * @param outputType The output format (YAML or XML)
     * @return The generated service definition as a string, or null if generation fails
     */
    @Nullable
    public String generate(@NotNull String className, @NotNull OutputType outputType) {
        return generate(className, outputType, true);
    }

    /**
     * Collects method parameters from the given class that are potential service dependencies.
     */
    @NotNull
    private List<MethodParameter.MethodModelParameter> collectMethodParameters(
            @NotNull PhpClass phpClass,
            @NotNull Map<String, ContainerService> serviceClass,
            @NotNull Set<String> serviceSetComplete) {

        List<MethodParameter.MethodModelParameter> modelParameters = new ArrayList<>();

        for (Method method : phpClass.getMethods()) {
            if (method.getModifier().isPublic()) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Set<String> possibleServices = getPossibleServices(parameters[i], serviceClass);
                    if (!possibleServices.isEmpty()) {
                        String serviceName = getServiceName(possibleServices);
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, possibleServices, serviceName));
                    } else {
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, serviceSetComplete));
                    }
                }
            }
        }

        return modelParameters;
    }

    /**
     * Gets possible services for a given parameter type.
     */
    @NotNull
    private Set<String> getPossibleServices(@NotNull Parameter parameter, @NotNull Map<String, ContainerService> serviceClass) {
        PhpPsiElement phpPsiElement = parameter.getFirstPsiChild();
        if (!(phpPsiElement instanceof ClassReference classReference)) {
            return Collections.emptySet();
        }

        String type = classReference.getFQN();
        if (type == null) {
            return Collections.emptySet();
        }

        return ServiceActionUtil.getPossibleServices(project, type, serviceClass);
    }

    /**
     * Gets the best service name from a set of possible services.
     * Returns the first one (services are sorted by priority).
     */
    @Nullable
    private String getServiceName(@NotNull Set<String> services) {
        return services.isEmpty() ? null : services.iterator().next();
    }

    /**
     * Output format for service definitions.
     */
    public enum OutputType {
        YAML,
        XML
    }
}
