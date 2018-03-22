package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlEventParser extends AbstractServiceParser {

    protected Map<String, String> list = new ConcurrentHashMap<>();
    protected List<EventDispatcherSubscribedEvent> events = new ArrayList<>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id]/tag[@event]";
    }

    public void parser(InputStream file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.list.put(node.getAttribute("event"), node.getAttribute("name"));
            if(((Element) node.getParentNode()).hasAttribute("class")) {
                this.events.add(new EventDispatcherSubscribedEvent(node.getAttribute("event"), ((Element) node.getParentNode()).getAttribute("class"), null).setType(node.getAttribute("name")));
            }

        }

    }

    public Map<String, String> get() {
        return list;
    }

    public List<EventDispatcherSubscribedEvent> getEvents() {
        return events;
    }

    public List<EventDispatcherSubscribedEvent> getEventSubscribers(String name) {

        ArrayList<EventDispatcherSubscribedEvent> subscribedEvents = new ArrayList<>();
        for(EventDispatcherSubscribedEvent subscribedEvent: this.getEvents()) {
            if(subscribedEvent.getStringValue().equals(name)) {
                subscribedEvents.add(subscribedEvent);
            }
        }

        return subscribedEvents;
    }

}