package javax.jmdns.test;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class EventStoringServiceListener implements ServiceListener {

    private final EventContainer addedEvents = new EventContainer();
    private final EventContainer removedEvents = new EventContainer();
    private final EventContainer resolvedEvents = new EventContainer();

    private final String serviceType;

    public EventStoringServiceListener(String serviceType) {
        this.serviceType = serviceType;
    }

    @Override
    public void serviceAdded(ServiceEvent event) {
        addedEvents.storeEvent(event);
    }

    @Override
    public void serviceRemoved(ServiceEvent event) {
        removedEvents.storeEvent(event);
    }

    @Override
    public void serviceResolved(ServiceEvent event) {
        resolvedEvents.storeEvent(event);
    }

    public boolean isServiceAddedReceived() {
        return addedEvents.isEventReceived(serviceType);
    }

    public boolean isServiceRemovedReceived() {
        return removedEvents.isEventReceived(serviceType);
    }

    public boolean isServiceResolvedReceived() {
        return resolvedEvents.isEventReceived(serviceType);
    }

    public void reset() {
        addedEvents.reset();
        removedEvents.reset();
        resolvedEvents.reset();
    }

}
