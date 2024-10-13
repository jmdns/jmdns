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
package javax.jmdns.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jmdns.ServiceEvent;

public class EventContainer {

    private static final Logger logger = LoggerFactory.getLogger(EventContainer.class);

    private final Map<String, ServiceEvent> events = new LinkedHashMap<>();

    private boolean eventReceived;

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
                    logger.error("InterruptedException during waiting", exception);
                }
            }
        }
    }

}
