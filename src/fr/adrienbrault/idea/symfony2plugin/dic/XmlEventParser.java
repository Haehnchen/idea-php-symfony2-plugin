package fr.adrienbrault.idea.symfony2plugin.dic;

import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class XmlEventParser extends AbstractServiceParser {

    protected HashMap<String, String> list = new HashMap<String, String>();
    protected ArrayList<EventDispatcherSubscribedEvent> events = new ArrayList<EventDispatcherSubscribedEvent>();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id]/tag[@event]";
    }

    public void parser(File file) {
        NodeList nodeList = this.parserer(file);

        if(nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            this.list.put(node.getAttribute("event"), node.getAttribute("name"));
            if(((Element) node.getParentNode()).hasAttribute("class")) {
                this.events.add(new EventDispatcherSubscribedEvent(node.getAttribute("event"), ((Element) node.getParentNode()).getAttribute("class")).setType(node.getAttribute("name")));
            }

        }

    }

    public HashMap<String, String> get() {
        return list;
    }

    public ArrayList<EventDispatcherSubscribedEvent> getEvents() {
        return events;
    }

    public ArrayList<EventDispatcherSubscribedEvent> getEventSubscribers(String name) {

        ArrayList<EventDispatcherSubscribedEvent> subscribedEvents = new ArrayList<EventDispatcherSubscribedEvent>();
        for(EventDispatcherSubscribedEvent subscribedEvent: this.getEvents()) {
            if(subscribedEvent.getStringValue().equals(name)) {
                subscribedEvents.add(subscribedEvent);
            }
        }

        return subscribedEvents;
    }

}