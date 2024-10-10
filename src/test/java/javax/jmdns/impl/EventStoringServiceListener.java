/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.jmdns.impl;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class EventStoringServiceListener implements ServiceListener {

    private EventContainer addedEvents    = new EventContainer();
    private EventContainer removedEvents  = new EventContainer();
    private EventContainer resolvedEvents = new EventContainer();

    private final String   serviceType;

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
