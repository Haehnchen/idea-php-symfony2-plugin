package fr.adrienbrault.idea.symfony2plugin.stubs.cache;

import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import java.util.HashMap;
import java.util.Map;

public class ServiceBuilderTimeCache {

    private boolean isInit = false;
    private Boolean inWrite = false;
    private Map<String, ContainerService> containerMap = new HashMap<String, ContainerService>();
    private final Object lock = new Object();

    synchronized public boolean isInitAndStart() {

        if(this.isInit) {
            return true;
        }

        this.isInit = true;

        return false;
    }

    public void startWrite() {
        this.inWrite = true;
    }

    public void endWrite(Map<String, ContainerService> foo) {
        synchronized(lock){
            containerMap.putAll(foo);
            inWrite = false;
            lock.notify();
        }
    }

    public Map<String, ContainerService> getContainerMap() {
        synchronized(lock){
            while (inWrite) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    return this.containerMap;
                }
            }
        }
        return this.containerMap;
    }

}
