package fr.adrienbrault.idea.symfonyplugin.action.ui;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodParameter implements Serializable {
    public static class MethodModelParameter {

        private Method method;
        private Parameter parameter;
        private int index;
        private Set<String> possibleServices;
        private boolean isPossibleService = false;
        private String currentService;

        public MethodModelParameter(Method method, Parameter parameter, int index, Set<String> possibleServices, String currentService) {
            this(method, parameter, index, possibleServices);
            this.isPossibleService = true;
            this.currentService = currentService;
        }

        public MethodModelParameter(Method method, Parameter parameter, int index, Set<String> possibleServices) {
            this.method = method;
            this.index = index;
            this.parameter = parameter;
            this.possibleServices = possibleServices;
        }

        public Method getMethod() {
            return this.method;
        }

        public String getName() {
            return this.method.getName();
        }

        public int getIndex() {
            return this.index;
        }

        public Parameter getParameter() {
            return this.parameter;
        }

        public Set<String> getPossibleServices() {
            return this.possibleServices;
        }

        public boolean isPossibleService() {
            return this.isPossibleService;
        }

        public void setPossibleService(boolean value) {
            this.isPossibleService = value;
        }

        public String getCurrentService() {
            return currentService;
        }

        public void setCurrentService(String currentService) {

            if (StringUtils.isBlank(currentService)) {
                this.currentService = null;
                this.isPossibleService = false;
                return;
            }

            this.currentService = currentService;
            this.isPossibleService = true;
        }

    }
}