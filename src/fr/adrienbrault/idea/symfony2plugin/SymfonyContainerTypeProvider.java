package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class SymfonyContainerTypeProvider implements PhpTypeProvider {

    private Map<String, Map<String, String>> servicesMapsPerProject;
    private Map<String, Long> servicesMapsLastModifiedPerProject;

    public SymfonyContainerTypeProvider() {
        servicesMapsPerProject = new HashMap<String, Map<String, String>>();
        servicesMapsLastModifiedPerProject = new HashMap<String, Long>();
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }

        if (!isContainerGetCall(e)) {
            return null;
        }

        String serviceId = getServiceId((MethodReference) e);
        if (null == serviceId) {
            return null;
        }

        Map<String, String> serviceMap = getServicesMap(e.getProject());
        String serviceClass = serviceMap.get(serviceId);

        if (null == serviceClass) {
            return null;
        }

        return new PhpType().add(serviceClass);
    }

    private boolean isContainerGetCall(PsiElement e) {
        return isContainerGetCall(e, 1);
    }

    private boolean isContainerGetCall(PsiElement e, int deepness) {
        // ((Method)((MethodReference)((PhpReturnInstruction)((Method)((MethodReference)e).getReference().resolve()).getControlFlow().getInstructions()[6]).getArgument()).getReference().resolve()).getFQN()
        if (!(e instanceof  MethodReference)) {
            return false;
        }

        MethodReference methodRef = (MethodReference) e;
        if (null == e.getReference()) {
            return false;
        }

        PsiElement resolvedReference = methodRef.getReference().resolve();
        if (!(resolvedReference instanceof Method)) {
            return false;
        }

        Method method = (Method) resolvedReference;
        String methodFQN = method.getFQN(); // Something like "\Symfony\Bundle\FrameworkBundle\Controller\Controller.get"
        if (null == methodFQN) {
            return false;
        }

        String expectedMethodFQN = "\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get";
        if (methodFQN.equals(expectedMethodFQN)) {
            return true;
        }

        if (deepness > 3) {
            return false;
        }

        // Try to see if this method return expression is a method call to a ContainerInterface::get ... recursive!

        PhpInstruction[] instructions = method.getControlFlow().getInstructions();
        for (int i = 0; i < instructions.length; i++) {
            PhpInstruction instruction = instructions[i];

            if (instruction instanceof PhpReturnInstruction) {
                PhpReturnInstruction returnInstruction = (PhpReturnInstruction) instruction;

                PsiElement returnInstructionElement = returnInstruction.getArgument();
                if (null != returnInstructionElement &&
                    null != returnInstructionElement.getReference() &&
                    returnInstructionElement.getReference().resolve() != resolvedReference) { // Avoid stackoverflow with method calling itself
                    return isContainerGetCall(returnInstructionElement, deepness + 1);
                }
            }
        }

        return false;
    }

    private String getServiceId(MethodReference e) {
        String serviceId = null;

        PsiElement[] parameters = e.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
            serviceId = parameters[0].getText(); // quoted string
            serviceId = serviceId.substring(1, serviceId.length() - 1);
        }

        return serviceId;
    }

    private Map<String, String>getServicesMap(Project project) {
        Map<String, String> map = new HashMap<String, String>();

        String defaultServiceMapFilePath = project.getBasePath() + "/" + Settings.getInstance(project).pathToProjectContainer;
        File xmlFile = new File(defaultServiceMapFilePath);
        if (!xmlFile.exists()) {
            return map;
        }

        Long xmlFileLastModified = xmlFile.lastModified();
        if (xmlFileLastModified.equals(servicesMapsLastModifiedPerProject.get(project.getBasePath()))) {
            return servicesMapsPerProject.get(project.getBasePath());
        }

        try {
            ServiceMapParser serviceMapParser = new ServiceMapParser();
            map = serviceMapParser.parse(xmlFile);
            servicesMapsPerProject.put(project.getBasePath(), map);
            servicesMapsLastModifiedPerProject.put(project.getBasePath(), xmlFileLastModified);
        } catch (SAXException e) {
            return map;
        } catch (IOException e) {
            return map;
        } catch (ParserConfigurationException e) {
            return map;
        }

        return map;
    }

}
