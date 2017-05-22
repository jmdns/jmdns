/**
 *
 */
package javax.jmdns.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.tasks.state.DNSStateTask;

import org.junit.Before;
import org.junit.Test;

public class TextUpdateTest {

    private ServiceInfo  service;
    private ServiceInfo  printer;
    private MockListener serviceListenerMock;

    private final static String serviceKey = "srvname"; // Max 9 chars

    public static class MockListener implements ServiceListener {

        private final List<ServiceEvent> _serviceAdded    = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));
        private final List<ServiceEvent> _serviceRemoved  = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));
        private final List<ServiceEvent> _serviceResolved = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceAdded(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceAdded(ServiceEvent event) {
            _serviceAdded.add(event.clone());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceRemoved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceRemoved(ServiceEvent event) {
            _serviceRemoved.add(event.clone());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.ServiceListener#serviceResolved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceResolved(ServiceEvent event) {
            _serviceResolved.add(event.clone());
        }

        public List<ServiceEvent> servicesAdded() {
            return _serviceAdded;
        }

        public List<ServiceEvent> servicesRemoved() {
            return _serviceRemoved;
        }

        public List<ServiceEvent> servicesResolved() {
            return _serviceResolved;
        }

        public synchronized void reset() {
            _serviceAdded.clear();
            _serviceRemoved.clear();
            _serviceResolved.clear();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Services Added: " + _serviceAdded.size());
            for (ServiceEvent event : _serviceAdded) {
                sb.append("\n\tevent name: '");
                sb.append(event.getName());
                sb.append("' type: '");
                sb.append(event.getType());
                sb.append("' info: '");
                sb.append(event.getInfo());
            }
            sb.append("\nServices Removed: " + _serviceRemoved.size());
            for (ServiceEvent event : _serviceRemoved) {
                sb.append("\n\tevent name: '");
                sb.append(event.getName());
                sb.append("' type: '");
                sb.append(event.getType());
                sb.append("' info: '");
                sb.append(event.getInfo());
            }
            sb.append("\nServices Resolved: " + _serviceResolved.size());
            for (ServiceEvent event : _serviceResolved) {
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

    @Before
    public void setup() {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(serviceKey, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        text = "Test hypothetical print server";
        properties.clear();
        properties.put(serviceKey, text.getBytes());
        printer = ServiceInfo.create("_html._tcp.local.", "printer-someuniqueid", "_printer", 80, 0, 0, true, properties);
        serviceListenerMock = new MockListener();
    }

    @Test
    public void testListenForTextUpdateOnOtherRegistry() throws IOException, InterruptedException {
        System.out.println("Unit Test: testListenForTextUpdateOnOtherRegistry()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals("We did not get the service added event.", 1, servicesAdded.size());
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service resolved event.", 1, servicesResolved.size());
            ServiceInfo result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", service, result);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), result.getPropertyString(serviceKey));

            serviceListenerMock.reset();
            String text = "Test improbable web server";
            Map<String, byte[]> properties = new HashMap<String, byte[]>();
            properties.put(serviceKey, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service text updated event.", 1, servicesResolved.size());
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertEquals("Did not get the expected service info text: ", text, result.getPropertyString(serviceKey));

            serviceListenerMock.reset();
            text = "Test more improbable web server";
            properties = new HashMap<String, byte[]>();
            properties.put(serviceKey, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service text updated event.", 1, servicesResolved.size());
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertEquals("Did not get the expected service info text: ", text, result.getPropertyString(serviceKey));

            serviceListenerMock.reset();
            text = "Test even more improbable web server";
            properties = new HashMap<String, byte[]>();
            properties.put(serviceKey, text.getBytes());
            service.setText(properties);
            Thread.sleep(3000);
            servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service text updated event.", 1, servicesResolved.size());
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertEquals("Did not get the expected service info text: ", text, result.getPropertyString(serviceKey));

        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    public void testRegisterEmptyTXTField() throws IOException, InterruptedException {
        System.out.println("Unit Test: testRegisterEmptyTXTField()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals("We did not get the service added event.", 1, servicesAdded.size());
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service resolved event.", 1, servicesResolved.size());
            ServiceInfo result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", service, result);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), result.getPropertyString(serviceKey));

            serviceListenerMock.reset();
            Map<String, byte[]> properties = new HashMap<String, byte[]>();
            service.setText(properties);
            Thread.sleep(4000);
            servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service text updated event.", 1, servicesResolved.size());
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNull("Did not get the expected service info text: ", result.getPropertyString(serviceKey));
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    public void testRegisterCaseSensitiveField() throws IOException {
        System.out.println("Unit Test: testRegisterCaseSensitiveField()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            String text = "Test hypothetical Web Server";
            Map<String, byte[]> properties = new HashMap<String, byte[]>();
            properties.put(serviceKey, text.getBytes());
            service = ServiceInfo.create("_Html._Tcp.local.", "Apache-SomeUniqueId", 80, 0, 0, true, properties);

            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals("We did not get the service added event.", 1, servicesAdded.size());
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service resolved event.", 1, servicesResolved.size());
            ServiceInfo result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", service, result);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), result.getPropertyString(serviceKey));

            ServiceInfo[] infos = registry.list(service.getType());
            assertEquals("We did not get the right list of service info.", 1, infos.length);
            assertEquals("Did not get the expected service info: ", service, infos[0]);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), infos[0].getPropertyString(serviceKey));

            infos = registry.list(service.getType().toLowerCase());
            assertEquals("We did not get the right list of service info.", 1, infos.length);
            assertEquals("Did not get the expected service info: ", service, infos[0]);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), infos[0].getPropertyString(serviceKey));

        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    public void testRenewExpiringRequests() throws IOException, InterruptedException {
        System.out.println("Unit Test: testRenewExpiringRequests()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {

            // To test for expiring TTL
            DNSStateTask.setDefaultTTL(1 * 60);

            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(service);

            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertTrue("We did not get the service added event.", servicesAdded.size() == 1);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);

            // wait for the TTL
            Thread.sleep(2 * 60 * 1000);

            services = registry.list(service.getType());
            assertEquals("We should see the service after the renewal: ", 1, services.length);
            assertEquals(service, services[0]);

        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
            DNSStateTask.setDefaultTTL(DNSConstants.DNS_TTL);
        }
    }

    @Test
    public void testSubtype() throws IOException {
        System.out.println("Unit Test: testSubtype()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(printer);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertEquals("We did not get the service added event.", 1, servicesAdded.size());
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals("We did not get the right name for the resolved service:", printer.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", printer.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertEquals("We did not get the service resolved event.", 1, servicesResolved.size());
            ServiceInfo result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", printer, result);
            assertEquals("Did not get the expected service info subtype: ", printer.getSubtype(), result.getSubtype());
            assertEquals("Did not get the expected service info text: ", printer.getPropertyString(serviceKey), result.getPropertyString(serviceKey));
            serviceListenerMock.reset();
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }

    }

}
