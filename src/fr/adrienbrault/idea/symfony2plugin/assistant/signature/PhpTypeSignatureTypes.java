package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.intellij.openapi.project.Project;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class PhpTypeSignatureTypes {

    public static PhpTypeSignatureInterface[] DEFAULT_PROVIDER = new PhpTypeSignatureInterface[] {
        new ServiceType(),
        new ClassType(),
        new FormTypesType()
    };

    private static class ServiceType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            String serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().get(parameter);
            if (null == serviceClass) {
                return null;
            }

            return PhpIndex.getInstance(project).getAnyByFQN(serviceClass);
        }

        @NotNull
        public String getName() {
            return "Service";
        }

    }

    private static class ClassType implements PhpTypeSignatureInterface {

        @Nullable
        public Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter) {
            return PhpIndex.getInstance(project).getAnyByFQN(parameter);
        }

        @NotNull
        public String getName() {
            return "Class";
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
