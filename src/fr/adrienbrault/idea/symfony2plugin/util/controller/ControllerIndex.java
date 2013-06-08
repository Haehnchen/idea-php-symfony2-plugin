package fr.adrienbrault.idea.symfony2plugin.util.controller;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class ControllerIndex {

    PhpIndex phpIndex;

    public ControllerIndex(PhpIndex phpIndex) {
       this.phpIndex = phpIndex;
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

}
