package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import com.jetbrains.php.lang.psi.elements.impl.MethodReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
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

    @Nullable
    @Override
    public PhpType getType(PsiElement e) {
        if (!isContainerGetCall(e)) {
            return null;
        }

        String serviceId = getServiceId((MethodReferenceImpl) e);
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
        if (!(e instanceof  MethodReferenceImpl)) {
            return false;
        }

        MethodReferenceImpl methodRefImpl = (MethodReferenceImpl) e;
        if (null == e.getReference()) {
            return false;
        }

        PsiElement resolvedReference = methodRefImpl.getReference().resolve();
        if (!(resolvedReference instanceof MethodImpl)) {
            return false;
        }

        MethodImpl methodImpl = (MethodImpl) resolvedReference;
        String methodFQN = methodImpl.getFQN(); // Something like "\Symfony\Bundle\FrameworkBundle\Controller\Controller.get"
        if (null == methodFQN) {
            return false;
        }

        if (methodFQN.equals("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller.get")
            || methodFQN.equals("\\Symfony\\Component\\DependencyInjection\\ContainerInterface.get")) {
            return true;
        }

        return false;
    }

    private String getServiceId(MethodReferenceImpl e) {
        String serviceId = null;

        PsiElement[] parameters = e.getParameters();
        if (parameters.length > 0 && parameters[0] instanceof StringLiteralExpressionImpl) {
            serviceId = parameters[0].getText(); // quoted string
            serviceId = serviceId.substring(1, serviceId.length() - 1);
        }

        return serviceId;
    }

    private Map<String, String> cachedServiceMap;
    private long cachedServiceMapLastModified;

    private Map<String, String>getServicesMap(Project project) {
        Map<String, String> map = new HashMap<String, String>();

        String defaultServiceMapFilePath = project.getBasePath() + Settings.getInstance(project).pathToProjectContainer;
        File xmlFile = new File(defaultServiceMapFilePath);
        if (!xmlFile.exists()) {
            return map;
        }

        long xmlFileLastModified = xmlFile.lastModified();
        if (xmlFileLastModified == cachedServiceMapLastModified) {
            return cachedServiceMap;
        }

        Document doc = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(xmlFile);
        } catch (SAXException e) {
            e.printStackTrace();

            return map;
        } catch (IOException e) {
            e.printStackTrace();

            return map;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();

            return map;
        }

        NodeList servicesNodes = doc.getElementsByTagName("service");
        for (int i = 0; i < servicesNodes.getLength(); i++) {
            Element node = (Element) servicesNodes.item(i);
            map.put(node.getAttribute("id"), "\\" + node.getAttribute("class"));
        }

        cachedServiceMap = map;
        cachedServiceMapLastModified = xmlFileLastModified;

        return map;
    }

}
