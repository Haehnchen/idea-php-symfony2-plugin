package fr.adrienbrault.idea.symfony2plugin.util.controller;


import com.jetbrains.php.lang.psi.elements.Method;

public class ControllerAction {

    String shortcutName;
    Method method;

    public ControllerAction(String shortcutName, Method method) {
       this.shortcutName = shortcutName;
       this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public String getShortcutName() {
        return shortcutName;
    }

}

