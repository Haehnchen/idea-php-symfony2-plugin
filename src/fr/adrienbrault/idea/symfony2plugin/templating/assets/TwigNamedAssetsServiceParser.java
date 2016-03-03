package fr.adrienbrault.idea.symfony2plugin.templating.assets;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TwigNamedAssetsServiceParser extends AbstractServiceParser {

    protected Map<String, String[]> namedAssets = new HashMap<String, String[]>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='assetic.asset_manager']//call[@method='addResource']//service[@class='Symfony\\Bundle\\AsseticBundle\\Factory\\Resource\\ConfigurationResource']//argument/argument[@key]";
    }

    public void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String key = node.getAttribute("key");
            if(key != null && StringUtils.isNotBlank(key)) {


                Set<String> files = new HashSet<String>();
                NodeList argument1 = node.getElementsByTagName("argument");
                if(argument1.getLength() > 1) {

                    Element argument = (Element) argument1.item(0);

                    NodeList firstChild = argument.getElementsByTagName("argument");
                    for (int x = 0; x < firstChild.getLength(); x++) {
                        String textContent = firstChild.item(x).getTextContent();
                        if(StringUtils.isNotBlank(textContent)) {
                            files.add(textContent);
                        }
                    }
                }

                namedAssets.put(key, files.toArray(new String[files.size()]));

            }
        }
    }

    public Map<String, String[]> getNamedAssets() {
        return namedAssets;
    }

}