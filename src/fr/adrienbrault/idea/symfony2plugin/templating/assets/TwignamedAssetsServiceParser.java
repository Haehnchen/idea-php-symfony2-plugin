package fr.adrienbrault.idea.symfony2plugin.templating.assets;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TwigNamedAssetsServiceParser extends AbstractServiceParser {

    protected Map<String, String[]> namedAssets = new HashMap<String, String[]>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id='assetic.asset_manager']//call[@method='addResource']//service[@class='Symfony\\Bundle\\AsseticBundle\\Factory\\Resource\\ConfigurationResource']//argument/argument[@key]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String key = node.getAttribute("key");
            if(key != null && StringUtils.isNotBlank(key)) {
                namedAssets.put(key, new String[0]);
            }
        }
    }

    public Map<String, String[]> getNamedAssets() {
        return namedAssets;
    }

}