package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ServiceRouteContainer;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ControllerIndex {

    private Project project;
    private PhpIndex phpIndex;

    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    public ControllerIndex(Project project) {
       this.project = project;
       this.phpIndex = PhpIndex.getInstance(project);
    }

    public List<ControllerAction> getActions() {

        List<ControllerAction> actions = new ArrayList<ControllerAction>();
        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(phpIndex);

        for (SymfonyBundle symfonyBundle : symfonyBundleUtil.getBundles()) {
            actions.addAll(this.getActionMethods(symfonyBundle));
        }

        return actions;
    }

    @Nullable
    public ControllerAction getControllerActionOnService(String shortcutName) {

        // only foo_bar:Method is valid
        if(!RouteHelper.isServiceController(shortcutName)) {
            return null;
        }

        String serviceId = shortcutName.substring(0, shortcutName.lastIndexOf(":"));
        String methodName = shortcutName.substring(shortcutName.lastIndexOf(":") + 1);

        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(this.project, serviceId, getLazyServiceCollector(this.project));
        if(phpClass == null) {
            return null;
        }

        Method method = phpClass.findMethodByName(methodName);
        if(method == null) {
            return null;
        }

        return new ControllerAction(serviceId, method);
    }

    @Nullable
    public ControllerAction getControllerAction(String shortcutName) {
        for(ControllerAction controllerAction: this.getActions()) {
            if(controllerAction.getShortcutName().equals(shortcutName)) {
                return controllerAction;
            }
        }

        return null;
    }

    private List<ControllerAction> getActionMethods(SymfonyBundle symfonyBundle) {

        String namespaceName = symfonyBundle.getNamespaceName();
        if(!namespaceName.startsWith("\\")) {
            namespaceName = "\\" + namespaceName;
        }

        if(!namespaceName.endsWith("\\")) {
            namespaceName += "\\";
        }

        namespaceName += "Controller";

        List<ControllerAction> actions = new ArrayList<ControllerAction>();

        for (PhpClass phpClass : PhpIndexUtil.getPhpClassInsideNamespace(this.project, namespaceName)) {

            if(!phpClass.getName().endsWith("Controller")) {
                continue;
            }

            String presentableFQN = phpClass.getPresentableFQN();
            if(presentableFQN == null) {
                continue;
            }

            if(!presentableFQN.startsWith("\\")) {
                presentableFQN = "\\" + presentableFQN;
            }

            presentableFQN = presentableFQN.substring(0, presentableFQN.length() - "Controller".length());
            if(presentableFQN.length() == 0) {
                continue;
            }

            String ns = presentableFQN.substring(namespaceName.length() + 1);

            for(Method method : phpClass.getMethods()) {
                String methodName = method.getName();
                if(methodName.endsWith("Action") && method.getAccess().isPublic()) {
                    String shortcutName = symfonyBundle.getName() + ":" + ns.replace("\\", "/") + ':' + methodName.substring(0, methodName.length() - 6);
                    actions.add(new ControllerAction(shortcutName, method));
                }

            }

        }

        return actions;
    }

    @NotNull
    public List<ControllerAction> getServiceActionMethods(@NotNull Project project) {

        Map<String,Route> routes = RouteHelper.getAllRoutes(project);
        if(routes.size() == 0) {
            return Collections.emptyList();
        }

        // there is now way to find service controllers directly,
        // so we search for predefined service controller and use the public methods
        ContainerCollectionResolver.LazyServiceCollector collector = new ContainerCollectionResolver.LazyServiceCollector(project);

        List<ControllerAction> actions = new ArrayList<ControllerAction>();
        for (String serviceName : ServiceRouteContainer.build(routes).getServiceNames()) {

            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, serviceName, collector);
            if(phpClass == null) {
                continue;
            }

            // find public method of the service class which are possible Actions
            for(Method method : phpClass.getMethods()) {
                if(method.getAccess().isPublic() && !method.getName().startsWith("__") && !method.getName().startsWith("set")) {
                    actions.add(new ControllerAction(serviceName + ":" + method.getName(), method));
                }
            }

        }

        return actions;
    }

    @Nullable
    public Method resolveShortcutName(String controllerName) {
        ControllerIndex controllerIndex = new ControllerIndex(project);
        ControllerAction controllerAction = controllerIndex.getControllerAction(controllerName);
        if(controllerAction != null) {
            return controllerAction.getMethod();
        }

        controllerAction = controllerIndex.getControllerActionOnService(controllerName);
        if(controllerAction != null) {
            return controllerAction.getMethod();
        }

        return null;
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

    static public List<LookupElement> getControllerLookupElements(Project project) {
        List<LookupElement> lookupElements = new ArrayList<LookupElement>();

        ControllerIndex controllerIndex = new ControllerIndex(project);
        for(ControllerAction controllerAction: controllerIndex.getActions()) {
            lookupElements.add(new ControllerActionLookupElement(controllerAction));
        }

        for(ControllerAction controllerAction: controllerIndex.getServiceActionMethods(project)) {
            lookupElements.add(new ControllerActionLookupElement(controllerAction));
        }

        return lookupElements;
    }

    @Nullable
    static public Method getControllerMethod(Project project, String controllerName) {
        return new ControllerIndex(project).resolveShortcutName(controllerName);
    }

}
