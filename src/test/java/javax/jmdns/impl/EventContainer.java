package javax.jmdns.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jmdns.ServiceEvent;

public class EventContainer {

    private final Map<String, ServiceEvent> events = new LinkedHashMap<String, ServiceEvent>();

    private boolean                         eventReceived;

    public void storeEvent(ServiceEvent event) {
        synchronized (events) {
            events.put(event.getType(), event);
            events.notifyAll();
        }
    }

    public boolean isEventReceived(String serviceType) {
        synchronized (events) {
            if (!eventReceived) {
                waitForEvent(serviceType);
            }
            return eventReceived;
        }
    }

    public void reset() {
        synchronized (events) {
            eventReceived = false;
            events.clear();
        }
    }

    private void waitForEvent(String serviceType) {
        synchronized (events) {
            eventReceived = events.containsKey(serviceType);
            int eventVerificationCount = 0;

            while (!eventReceived && eventVerificationCount < 30) {
                try {
                    events.wait(1000);
                    eventReceived = events.containsKey(serviceType);
                    eventVerificationCount++;
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

}
