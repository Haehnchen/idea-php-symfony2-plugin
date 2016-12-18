package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTypeSignatureTypes {

    public static PhpTypeSignatureInterface[] DEFAULT_PROVIDER = new PhpTypeSignatureInterface[] {
        new ServiceType(),
        new ClassType(),
        new FormTypesType(),
        new InterfaceType(),
        new ClassInterfaceType(),
    };

    private static class ServiceType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {

            ContainerService containerService = ContainerCollectionResolver.getService(project, parameter);
            if(containerService != null) {
                String serviceClass = containerService.getClassName();
                if(serviceClass != null) {
                    return PhpIndex.getInstance(project).getAnyByFQN(serviceClass);
                }
            }

            return null;
        }

        @NotNull
        public String getName() {
            return "Service";
        }

    }

    private static class ClassType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            return PhpIndex.getInstance(project).getClassesByFQN(parameter.startsWith("\\") ? parameter : "\\" + parameter);
        }

        @NotNull
        public String getName() {
            return "Class";
        }

    }

    private static class ClassInterfaceType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            return Arrays.asList(PhpElementsUtil.getClassInterface(project, parameter));
        }

        @NotNull
        public String getName() {
            return "ClassInterface";
        }

    }

    private static class InterfaceType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            return PhpIndex.getInstance(project).getInterfacesByFQN(parameter.startsWith("\\") ? parameter : "\\" + parameter);
        }

        @NotNull
        public String getName() {
            return "Interface";
        }

    }

    private static class FormTypesType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            return Arrays.asList(FormUtil.getFormTypeToClass(project, parameter));
        }

        @NotNull
        public String getName() {
            return "FormType";
        }

    }

}
