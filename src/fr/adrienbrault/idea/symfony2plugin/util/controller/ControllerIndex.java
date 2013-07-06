package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ControllerIndex {

    Project project;
    PhpIndex phpIndex;

    public ControllerIndex(Project project) {
       this.project = project;
       this.phpIndex = PhpIndex.getInstance(project);
    }

    public ArrayList<ControllerAction> getAction() {

        ArrayList<ControllerAction> actions = new ArrayList<ControllerAction>();
        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(phpIndex);

        Collection<PhpClass> controllerClasses = phpIndex.getAllSubclasses("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller");
        for(PhpClass controllerClass : controllerClasses) {
            actions.addAll(this.getActionMethods(symfonyBundleUtil, controllerClass));
        }

        return actions;

    }

    @Nullable
    public ControllerAction getControllerActionOnService(String shortcutName) {

        // only foo_bar:Method is valid
        if(shortcutName.contains("::") || !shortcutName.contains(":") || shortcutName.contains("\\")) {
            return null;
        }

        String serviceId = shortcutName.substring(0, shortcutName.lastIndexOf(":"));
        String methodName = shortcutName.substring(shortcutName.lastIndexOf(":") + 1);

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(this.project, XmlServiceParser.class).getServiceMap();

        if(serviceMap.getMap().containsKey(serviceId))  {
            Collection<? extends PhpNamedElement> methodCalls = this.phpIndex.getBySignature("#M#C" + serviceMap.getMap().get(serviceId) + "." + methodName, null, 0);

            for(PhpNamedElement phpNamedElement : methodCalls) {
                if(phpNamedElement instanceof Method) {
                    return new ControllerAction(serviceId, (Method) phpNamedElement);
                }
            }

        }

        return null;
    }

    @Nullable
    public ControllerAction getControllerAction(String shortcutName) {
        for(ControllerAction controllerAction: this.getAction()) {
            if(controllerAction.getShortcutName().equals(shortcutName)) {
                return controllerAction;
            }
        }

        return null;
    }

    private ArrayList<ControllerAction> getActionMethods(SymfonyBundleUtil symfonyBundleUtil, PhpClass controllerClass) {

        ArrayList<ControllerAction> actions = new ArrayList<ControllerAction>();

        Collection<Method> methods = controllerClass.getMethods();
        SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(controllerClass);

        if(symfonyBundle == null) {
            return actions;
        }

        String path = symfonyBundle.getRelative(controllerClass.getContainingFile().getVirtualFile(), true);
        if(path == null || !path.startsWith("Controller/") || !path.endsWith("Controller")) {
            return actions;
        }

        for(Method method : methods) {
            String methodName = method.getName();
            if(methodName.endsWith("Action") && method.getAccess().isPublic()) {
                String shortcutName = symfonyBundle.getName() + ":" + path.substring("Controller/".length(), path.length() - 10) + ':' + methodName.substring(0, methodName.length() - 6);
                actions.add(new ControllerAction(shortcutName, method));
            }

        }

        return actions;
    }

    public ArrayList<ControllerAction> getServiceActionMethods(Project project) {

        ArrayList<ControllerAction> actions = new ArrayList<ControllerAction>();

        Symfony2ProjectComponent symfony2ProjectComponent = project.getComponent(Symfony2ProjectComponent.class);
        Map<String,Route> routes = symfony2ProjectComponent.getRoutes();
        if(routes.size() == 0) {
            return actions;
        }

        ServiceMap serviceMap = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap();
        if(serviceMap.getMap().size() == 0) {
            return actions;
        }

        HashMap<String, String> controllerClassNames = new HashMap<String, String>();

        // there is now way to find service controllers directly,
        // so we search for predefined service controller and use the public methods
        for (Map.Entry<String,Route> entrySet: routes.entrySet()) {
            String controllerName = entrySet.getValue().getController();
            if(!controllerName.contains("::") && controllerName.contains(":")) {
                String serviceId = controllerName.substring(0, controllerName.lastIndexOf(":"));
                if(serviceMap.getMap().containsKey(serviceId)) {
                    String className =  serviceMap.getMap().get(serviceId);
                    controllerClassNames.put(serviceId, className);
                }
            }
        }

        // find public method of the service class which are possible Actions
        for(Map.Entry<String, String> classDefinition: controllerClassNames.entrySet()) {
            PhpClass phpClass = PhpElementsUtil.getClass(this.phpIndex, classDefinition.getValue());
            if(phpClass != null) {
                for(Method method : phpClass.getMethods()) {
                    if(method.getAccess().isPublic() && !method.getName().startsWith("__") && !method.getName().startsWith("set")) {
                        actions.add(new ControllerAction(classDefinition.getKey() + ":" + method.getName(), method));
                    }
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

    static public ArrayList<LookupElement> getControllerLookupElements(Project project) {
        ArrayList<LookupElement> lookupElements = new ArrayList<LookupElement>();

        ControllerIndex controllerIndex = new ControllerIndex(project);
        for(ControllerAction controllerAction: controllerIndex.getAction()) {
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
