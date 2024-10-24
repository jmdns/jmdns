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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.test.EventStoringServiceListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class JmDNSTest {

    private ServiceListener serviceListenerMock;
    private ServiceInfo service;
    private static final String SERVICE_KEY = "srvname"; // Max 9 chars
    private static final int MPORT = 8053;

    @BeforeEach
    public void setup() {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(SERVICE_KEY, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        serviceListenerMock = mock(ServiceListener.class);
    }

    @Test
    void testCreate() throws IOException {
        JmDNSImpl registry = (JmDNSImpl) JmDNS.create();
        assertNotNull(registry);
        registry.close();
        assertTrue(registry.isCanceled()); // should it be canceled after close?
    }

    @Test
    void testCreateINet() throws IOException {
        InetAddress localhost = InetAddress.getLocalHost();
        try (JmDNS registry = JmDNS.create(localhost)) {
            assertEquals(localhost, registry.getInetAddress(), "We did not register on the inetAddress of the localhost");
        }
    }

    @Test
    void testRegisterService() throws IOException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
        }
    }

    @Test
    void testUnregisterService() throws IOException, InterruptedException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);

            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);

            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);

            services = registry.list(service.getType());
            assertTrue(services == null || services.length == 0, "We should not see the service we just unregistered: ");
        }
    }

    @Test
    void testRegisterServiceTwice() throws IOException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);
            assertThrows(IllegalStateException.class,
                    () -> registry.registerService(service),
                    "Registering the same service info should fail.");
        }
    }

    @Test
    void testUnregisterAndRegisterService() throws IOException, InterruptedException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);

            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);

            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);

            services = registry.list(service.getType());
            assertTrue(services == null || services.length == 0, "We should not see the service we just unregistered: ");

            registry.registerService(service);
            Thread.sleep(5000);
            services = registry.list(service.getType());
            assertTrue(services != null && services.length > 0, "We should see the service we just registered again: ");
        }
    }

    @Test
    void testQueryMyService() throws IOException, InterruptedException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);
            Thread.sleep(6000);
            ServiceInfo queriedService = registry.getServiceInfo(service.getType(), service.getName());
            assertEquals(service, queriedService);
        }
    }

    @Test
    void testListMyService() throws IOException {
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
        }
    }

    @Test
    void testListMyServiceIPV6() throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
        for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses(); inetAddresses.hasMoreElements(); ) {
            InetAddress interfaceAddress = inetAddresses.nextElement();
            if (interfaceAddress instanceof Inet6Address) {
                address = interfaceAddress;
            }
        }
        try (JmDNS registry = JmDNS.create(address)) {
            registry.registerService(service);
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
        }
    }

    @Test
    void testListenForMyService() throws IOException, InterruptedException {
        ArgumentCaptor<ServiceEvent> capServiceAddedEvent = ArgumentCaptor.forClass(ServiceEvent.class);
        ArgumentCaptor<ServiceEvent> capServiceResolvedEvent = ArgumentCaptor.forClass(ServiceEvent.class);

        try (JmDNS registry = JmDNS.create()) {
            registry.addServiceListener(service.getType(), serviceListenerMock);
            registry.registerService(service);
            Thread.sleep(6000);

            verify(serviceListenerMock, atLeastOnce()).serviceAdded(capServiceAddedEvent.capture());
            verify(serviceListenerMock, atLeastOnce()).serviceResolved(capServiceResolvedEvent.capture());
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            assertFalse(capServiceAddedEvent.getAllValues().isEmpty(), "We did not get the service added event.");
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the added service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the added service:");
            assertEquals(service.getQualifiedName(), info.getQualifiedName(), "We did not get the right fully qualified name for the added service:");

            registry.requestServiceInfo(service.getType(), service.getName());

            assertFalse(capServiceResolvedEvent.getAllValues().isEmpty(), "We did not get the service resolved event.");

            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
            assertEquals(service, resolvedInfo, "Did not get the expected service info: ");
        }
    }

    @Test
    void testListenForMyServiceAndList() throws IOException, InterruptedException {
        ArgumentCaptor<ServiceEvent> capServiceAddedEvent = ArgumentCaptor.forClass(ServiceEvent.class);
        ArgumentCaptor<ServiceEvent> capServiceResolvedEvent = ArgumentCaptor.forClass(ServiceEvent.class);

        try (JmDNS registry = JmDNS.create()) {
            registry.addServiceListener(service.getType(), serviceListenerMock);
            registry.registerService(service);
            Thread.sleep(6000);

            verify(serviceListenerMock, atLeastOnce()).serviceAdded(capServiceAddedEvent.capture());
            verify(serviceListenerMock, atLeastOnce()).serviceResolved(capServiceResolvedEvent.capture());
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");

            // This will force the resolution of the service which in turn will get the listener called with a service resolved event.
            // The info associated with a service resolved event has all the information available.
            // Which in turn populates the ServiceInfo objects returned by JmDNS.list.
            ServiceInfo[] services = registry.list(info.getType());
            assertEquals(1, services.length, "We did not get the expected number of services: ");
            assertEquals(service, services[0], "The service returned was not the one expected");

            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
            assertEquals(service, resolvedInfo, "Did not get the expected service info: ");
        }
    }

    @Test
    void testListenForServiceOnOtherRegistry() throws IOException, InterruptedException {
        ArgumentCaptor<ServiceEvent> capServiceAddedEvent = ArgumentCaptor.forClass(ServiceEvent.class);
        ArgumentCaptor<ServiceEvent> capServiceResolvedEvent = ArgumentCaptor.forClass(ServiceEvent.class);

        try (JmDNS registry = JmDNS.create();
             JmDNS newServiceRegistry = JmDNS.create()) {
            registry.addServiceListener(service.getType(), serviceListenerMock);
            newServiceRegistry.registerService(service);
            Thread.sleep(6000);

            verify(serviceListenerMock, atLeastOnce()).serviceAdded(capServiceAddedEvent.capture());
            verify(serviceListenerMock, atLeastOnce()).serviceResolved(capServiceResolvedEvent.capture());
            // We get the service added event when we register the service. However, the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");
            Object result = capServiceResolvedEvent.getValue().getInfo();
            assertEquals(service, result, "Did not get the expected service info: ");
        }
    }

    @Test
    void testWaitAndQueryForServiceOnOtherRegistry() throws Exception {
        try (JmDNS newServiceRegistry = JmDNS.create(InetAddress.getLocalHost());
             JmDNS registry = JmDNS.create(InetAddress.getLocalHost())) {
            registry.registerService(service);
            Thread.sleep(6000);
            ServiceInfo fetchedService = newServiceRegistry.getServiceInfo(service.getType(), service.getName());
            assertNotNull(fetchedService, "ServiceInfo is a null reference");
            assertEquals(service, fetchedService, "Did not get the expected service info: ");
        }
    }

    @Test
    void testRegisterAndListServiceOnOtherRegistry() throws IOException, InterruptedException {
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            registry = JmDNS.create("Registry");
            registry.registerService(service);

            newServiceRegistry = JmDNS.create("Listener");
            Thread.sleep(6000);
            ServiceInfo[] fetchedServices = newServiceRegistry.list(service.getType());
            assertEquals(1, fetchedServices.length, "Did not get the expected services listed:");
            assertEquals(service.getType(), fetchedServices[0].getType(), "Did not get the expected service type:");
            assertEquals(service.getName(), fetchedServices[0].getName(), "Did not get the expected service name:");
            assertEquals(service.getQualifiedName(), fetchedServices[0].getQualifiedName(), "Did not get the expected service fully qualified name:");
            newServiceRegistry.getServiceInfo(service.getType(), service.getName());

            assertEquals(service, fetchedServices[0], "Did not get the expected service info: ");
            registry.close();
            registry = null;
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            fetchedServices = newServiceRegistry.list(service.getType());
            assertEquals(0, fetchedServices.length, "The service was not cancelled after the close:");
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    void testAddServiceListenerTwice() throws IOException {
        try (JmDNSImpl registry = (JmDNSImpl) JmDNS.create()) {
            registry.addServiceListener("test", serviceListenerMock);

            // we will have 2 listeners, since the collector is implicitly added as well
            assertEquals(2, registry._serviceListeners.get("test").size());

            registry.addServiceListener("test", serviceListenerMock);
            assertEquals(2, registry._serviceListeners.get("test").size());
        }
    }

    @Test
    void testRemoveServiceListener() throws IOException {
        try (JmDNSImpl registry = (JmDNSImpl) JmDNS.create()) {
            registry.addServiceListener("test", serviceListenerMock);
            // we will have 2 listeners, since the collector is implicitly added as well
            assertEquals(2, registry._serviceListeners.get("test").size());

            registry.removeServiceListener("test", serviceListenerMock);
            // the collector is left in place
            assertEquals(1, registry._serviceListeners.get("test").size());
        }
    }

    private static final class Receive extends Thread {
        MulticastSocket multicastSocket;
        DatagramPacket datagramPacket;

        public Receive(MulticastSocket socket, DatagramPacket in) {
            super("Test Receive Multicast");
            multicastSocket = socket;
            datagramPacket = in;
        }

        @Override
        public void run() {
            try {
                multicastSocket.receive(datagramPacket);
            } catch (IOException exception) {
                // Ignore
            }
        }

        public boolean waitForReceive() {
            try {
                this.join(1000);
            } catch (InterruptedException exception) {
                // Ignore
            }
            return this.isAlive();
        }

    }

    @Test
    void testTwoMulticastPortsAtOnce() throws IOException {
        MulticastSocket firstSocket = null;
        MulticastSocket secondSocket = null;
        try {
            String firstMessage = "ping";
            String secondMessage = "pong";
            InetAddress someInet = InetAddress.getByName(DNSConstants.MDNS_GROUP);
            firstSocket = new MulticastSocket(MPORT);
            secondSocket = new MulticastSocket(MPORT);

            firstSocket.joinGroup(someInet);
            secondSocket.joinGroup(someInet);
            //
            DatagramPacket out = new DatagramPacket(firstMessage.getBytes(StandardCharsets.UTF_8), firstMessage.length(), someInet, MPORT);
            DatagramPacket inSecond = new DatagramPacket(firstMessage.getBytes(StandardCharsets.UTF_8), firstMessage.length(), someInet, MPORT);
            Receive receiveSecond = new Receive(secondSocket, inSecond);
            receiveSecond.start();
            Receive receiveFirst = new Receive(firstSocket, inSecond);
            receiveFirst.start();
            firstSocket.send(out);
            if (receiveSecond.waitForReceive()) {
                fail("We did not receive the data in the second socket");
            }
            String fromFirst = new String(inSecond.getData(), StandardCharsets.UTF_8);
            assertEquals(firstMessage, fromFirst, "Expected the second socket to recieve the same message the first socket sent");
            // Make sure the first socket had read its own message
            if (receiveSecond.waitForReceive()) {
                fail("We did not receive the data in the first socket");
            }
            // Reverse the roles
            out = new DatagramPacket(secondMessage.getBytes(StandardCharsets.UTF_8), secondMessage.length(), someInet, MPORT);
            DatagramPacket inFirst = new DatagramPacket(secondMessage.getBytes(StandardCharsets.UTF_8), secondMessage.length(), someInet, MPORT);
            receiveFirst = new Receive(firstSocket, inSecond);
            receiveFirst.start();

            secondSocket.send(out);
            if (receiveFirst.waitForReceive()) {
                fail("We did not receive the data in the first socket");
            }
            String fromSecond = new String(inFirst.getData(), StandardCharsets.UTF_8);
            assertEquals(secondMessage, fromSecond, "Expected the first socket to recieve the same message the second socket sent");
        } finally {
            if (firstSocket != null) firstSocket.close();
            if (secondSocket != null) secondSocket.close();
        }
    }

    @Test
    void testListMyServiceWithToLowerCase() throws IOException, InterruptedException {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(SERVICE_KEY, text.getBytes());
        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);

            // with toLowerCase
            ServiceInfo[] services = registry.list(service.getType().toLowerCase());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            services = registry.list(service.getType().toLowerCase());
            assertTrue(services == null || services.length == 0, "We should not see the service we just unregistered: ");
        }
    }

    @Test
    void testListMyServiceWithoutLowerCase() throws IOException, InterruptedException {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(SERVICE_KEY, text.getBytes());
        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
        try (JmDNS registry = JmDNS.create()) {
            registry.registerService(service);

            // without toLowerCase
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals(1, services.length, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            services = registry.list(service.getType());
            assertTrue(services == null || services.length == 0, "We should not see the service we just unregistered: ");
        }
    }

    @Test
    void shouldNotNotifyServiceListenersForServiceResolveAfterServiceRemoved() throws IOException, InterruptedException {
        String firstType = "_test._type1.local.";

        try (JmmDNS jmmdns = JmmDNS.Factory.getInstance()) {
            Thread.sleep(5000);

            EventStoringServiceListener firstListener = new EventStoringServiceListener(firstType);
            jmmdns.addServiceListener(firstType, firstListener);

            InetAddress[] addresses = jmmdns.getInetAddresses();
            JmDNS[] dns = new JmDNS[addresses.length];
            Map<String, String> txtRecord = new HashMap<>();
            txtRecord.put("SOME KEY", "SOME VALUE");

            ServiceInfo info = ServiceInfo.create(firstType, "SOME TEST NAME", 4444, 0, 0, true, txtRecord);

            for (int i = 0; i < addresses.length; i++) {
                dns[i] = JmDNS.create(addresses[i], null);
                dns[i].registerService(info.clone());
            }


            assertTrue(firstListener.isServiceAddedReceived());
            assertTrue(firstListener.isServiceResolvedReceived());
            assertFalse(firstListener.isServiceRemovedReceived());

            Thread.sleep(30 * 1000);

            firstListener.reset();

            for (JmDNS dn : dns) {
                dn.close();
            }

            assertFalse(firstListener.isServiceAddedReceived());
            assertTrue(firstListener.isServiceRemovedReceived());
            assertFalse(firstListener.isServiceResolvedReceived());
        }
    }

}
