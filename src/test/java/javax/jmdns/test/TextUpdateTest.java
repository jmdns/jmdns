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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.tasks.state.DNSStateTask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextUpdateTest {

    private ServiceInfo service;
    private ServiceInfo printer;
    private ServiceInfo caseSensitivePrinter;
    private MockListener serviceListenerMock;

    private static final String SERVICE_KEY = "srvname"; // Max 9 chars

    private static class MockListener implements ServiceListener {

        private final List<ServiceEvent> serviceAdded = Collections.synchronizedList(new ArrayList<>(2));
        private final List<ServiceEvent> serviceRemoved = Collections.synchronizedList(new ArrayList<>(2));
        private final List<ServiceEvent> serviceResolved = Collections.synchronizedList(new ArrayList<>(2));

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceAdded(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceAdded(ServiceEvent event) {
            serviceAdded.add(event.clone());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceRemoved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceRemoved(ServiceEvent event) {
            serviceRemoved.add(event.clone());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceResolved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceResolved(ServiceEvent event) {
            serviceResolved.add(event.clone());
        }

        public List<ServiceEvent> servicesAdded() {
            return serviceAdded;
        }

        public List<ServiceEvent> servicesRemoved() {
            return serviceRemoved;
        }

        public List<ServiceEvent> servicesResolved() {
            return serviceResolved;
        }

        public synchronized void reset() {
            serviceAdded.clear();
            serviceRemoved.clear();
            serviceResolved.clear();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Services Added: " + serviceAdded.size());
            for (ServiceEvent event : serviceAdded) {
                sb.append("\n\tevent name: '");
                sb.append(event.getName());
                sb.append("' type: '");
                sb.append(event.getType());
                sb.append("' info: '");
                sb.append(event.getInfo());
            }
            sb.append("\nServices Removed: " + serviceRemoved.size());
            for (ServiceEvent event : serviceRemoved) {
                sb.append("\n\tevent name: '");
                sb.append(event.getName());
                sb.append("' type: '");
                sb.append(event.getType());
                sb.append("' info: '");
                sb.append(event.getInfo());
            }
            sb.append("\nServices Resolved: " + serviceResolved.size());
            for (ServiceEvent event : serviceResolved) {
                sb.append("\n\tevent name: '");
                sb.append(event.getName());
                sb.append("' type: '");
                sb.append(event.getType());
                sb.append("' info: '");
                sb.append(event.getInfo());
            }
            return sb.toString();
        }

    }

    @BeforeEach
    public void setup() {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(SERVICE_KEY, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        text = "Test hypothetical print server";
        properties.clear();
        properties.put(SERVICE_KEY, text.getBytes());
        printer = ServiceInfo.create("_html._tcp.local.", "printer-someuniqueid", "_printer", 80, 0, 0, true, properties);
        text = "Test hypothetical print server with case-sensitive sub-type";
        properties.clear();
        properties.put(SERVICE_KEY, text.getBytes());
        caseSensitivePrinter = ServiceInfo.create("_html._tcp.local.", "cs-printer-someuniqueid", "_Printer", 80, 0, 0, true, properties);

        serviceListenerMock = new MockListener();
    }

    @Test
    void testListenForTextUpdateOnOtherRegistry() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(service);
            Thread.sleep(6000);

            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            Optional<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service resolved event.");
            ServiceInfo result = servicesResolved.get().getInfo();
            assertNotNull(result, "Did not get the expected service info: ");
            assertEquals(service, result, "Did not get the expected service info: ");
            assertEquals(service.getPropertyString(SERVICE_KEY), result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            serviceListenerMock.reset();
            String text = "Test improbable web server";
            Map<String, byte[]> properties = new HashMap<>();
            properties.put(SERVICE_KEY, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service text updated event.");
            result = servicesResolved.get().getInfo();
            assertEquals(text, result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            serviceListenerMock.reset();
            text = "Test more improbable web server";
            properties = new HashMap<>();
            properties.put(SERVICE_KEY, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue( servicesResolved.isPresent(), "We did not get the service text updated event.");
            result = servicesResolved.get().getInfo();
            assertEquals(text, result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            serviceListenerMock.reset();
            text = "Test even more improbable web server";
            properties = new HashMap<>();
            properties.put(SERVICE_KEY, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service text updated event.");
            result = servicesResolved.get().getInfo();
            assertEquals(text, result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");
        }
    }

    @Test
    void testRegisterEmptyTXTField() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(service);
            Thread.sleep(6000);

            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            Optional<ServiceEvent> serviceResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(serviceResolved.isPresent(), "We did not get the service resolved event.");
            ServiceInfo result = serviceResolved.get().getInfo();
            assertNotNull(result, "Did not get the expected service info: ");
            assertEquals(service, result, "Did not get the expected service info: ");
            assertEquals(service.getPropertyString(SERVICE_KEY), result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            serviceListenerMock.reset();
            Map<String, byte[]> properties = new HashMap<>();
            service.setText(properties);
            Thread.sleep(4000);
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals(1, servicesResolved.size(), "We did not get the service text updated event.");
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNull(result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");
        }
    }

    @Test
    void testRegisterCaseSensitiveField() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            String text = "Test hypothetical Web Server";
            Map<String, byte[]> properties = new HashMap<>();
            properties.put(SERVICE_KEY, text.getBytes());
            service = ServiceInfo.create("_Html._Tcp.local.", "Apache-SomeUniqueId", 80, 0, 0, true, properties);

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(service);
            Thread.sleep(6000);

            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            Optional<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service resolved event.");
            ServiceInfo result = servicesResolved.get().getInfo();
            assertNotNull(result, "Did not get the expected service info: ");
            assertEquals(service, result, "Did not get the expected service info: ");
            assertEquals(service.getPropertyString(SERVICE_KEY), result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            ServiceInfo[] infos = registry.list(service.getType());
            assertEquals(1, infos.length, "We did not get the right list of service info.");
            assertEquals(service, infos[0], "Did not get the expected service info: ");
            assertEquals(service.getPropertyString(SERVICE_KEY), infos[0].getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");

            infos = registry.list(service.getType().toLowerCase());
            assertEquals(1, infos.length, "We did not get the right list of service info.");
            assertEquals(service, infos[0], "Did not get the expected service info: ");
            assertEquals(service.getPropertyString(SERVICE_KEY), infos[0].getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");
        }
    }

    @Test
    void testRenewExpiringRequests() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            // To test for expiring TTL
            DNSStateTask.setDefaultTTL(60);

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(service);
            Thread.sleep(6000);

            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);

            // wait for the TTL
            Thread.sleep(2 * 60 * 1000);

            services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service after the renewal: ");
            assertEquals(service, services[0]);

        } finally {
            DNSStateTask.setDefaultTTL(DNSConstants.DNS_TTL);
        }
    }

    @Test
    void testSubtype() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(printer);
            Thread.sleep(6000);

            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals(printer.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(printer.getType(), info.getType(), "We did not get the right type for the resolved service:");
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            Optional<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service resolved event.");
            ServiceInfo result = servicesResolved.get().getInfo();
            assertNotNull(result, "Did not get the expected service info: ");
            assertEquals(printer, result, "Did not get the expected service info: ");
            assertEquals(printer.getSubtype(), result.getSubtype(), "Did not get the expected service info subtype: ");
            assertEquals(printer.getPropertyString(SERVICE_KEY), result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");
            serviceListenerMock.reset();
        }
    }

    @Test
    void testCaseSensitiveSubtype() throws IOException, InterruptedException {

        try (JmDNS registry = JmDNS.create("Listener");
             JmDNS newServiceRegistry = JmDNS.create("Registry")) {

            registry.addServiceListener(service.getType(), serviceListenerMock);

            newServiceRegistry.registerService(caseSensitivePrinter);
            Thread.sleep(6000);

            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals(1, servicesAdded.size(), "We did not get the service added event.");
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals(caseSensitivePrinter.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(caseSensitivePrinter.getType(), info.getType(), "We did not get the right type for the resolved service:");
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            Optional<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved().stream()
                    .filter(e -> new String(e.getInfo().getTextBytes()).contains(SERVICE_KEY)).findFirst();
            assertTrue(servicesResolved.isPresent(), "We did not get the service resolved event.");
            ServiceInfo result = servicesResolved.get().getInfo();
            assertNotNull(result, "Did not get the expected service info: ");
            assertEquals(caseSensitivePrinter, result, "Did not get the expected service info: ");
            assertEquals(caseSensitivePrinter.getSubtype(), result.getSubtype(), "Did not get the expected service info subtype: ");
            assertEquals(caseSensitivePrinter.getPropertyString(SERVICE_KEY), result.getPropertyString(SERVICE_KEY), "Did not get the expected service info text: ");
            serviceListenerMock.reset();
        }
    }

}
