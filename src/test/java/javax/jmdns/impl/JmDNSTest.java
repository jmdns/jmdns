package javax.jmdns.impl;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.jmdns.impl.constants.DNSConstants;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JmDNSTest {

    @SuppressWarnings("unused")
    private ServiceTypeListener typeListenerMock;
    private ServiceListener     serviceListenerMock;
    private ServiceInfo         service;

    private final static String serviceKey = "srvname"; // Max 9 chars

    @Before
    public void setup() {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(serviceKey, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        typeListenerMock = createMock(ServiceTypeListener.class);
        serviceListenerMock = createNiceMock("ServiceListener", ServiceListener.class);
    }

    @Test
    public void testCreate() throws IOException {
        System.out.println("Unit Test: testCreate()");
        JmDNS registry = JmDNS.create();
        registry.close();
    }

    @Test
    public void testCreateINet() throws IOException {
        System.out.println("Unit Test: testCreateINet()");
        JmDNS registry = JmDNS.create(InetAddress.getLocalHost());
        // assertEquals("We did not register on the local host inet:", InetAddress.getLocalHost(), registry.getInterface());
        registry.close();
    }

    @Test
    public void testRegisterService() throws IOException {
        System.out.println("Unit Test: testRegisterService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testUnregisterService() throws IOException, InterruptedException {
        System.out.println("Unit Test: testUnregisterService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);

            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);

            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);

            services = registry.list(service.getType());
            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testRegisterServiceTwice() throws IOException {
        System.out.println("Unit Test: testRegisterService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);
            // This should cause an exception
            registry.registerService(service);
            fail("Registering the same service info should fail.");
        } catch (IllegalStateException exception) {
            // Expected exception.
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testUnregisterAndReregisterService() throws IOException, InterruptedException {
        System.out.println("Unit Test: testUnregisterAndReregisterService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);

            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);

            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);

            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);

            services = registry.list(service.getType());
            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);

            registry.registerService(service);
            Thread.sleep(5000);
            services = registry.list(service.getType());
            assertTrue("We should see the service we just reregistered: ", services != null && services.length > 0);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    @Ignore
    public void testQueryMyService() throws IOException {
        System.out.println("Unit Test: testQueryMyService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);
            ServiceInfo queriedService = registry.getServiceInfo(service.getType(), service.getName());
            assertEquals(service, queriedService);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testListMyService() throws IOException {
        System.out.println("Unit Test: testListMyService()");
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testListMyServiceIPV6() throws IOException {
        System.out.println("Unit Test: testListMyServiceIPV6()");
        JmDNS registry = null;
        try {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface interfaze = NetworkInterface.getByInetAddress(address);
            for (Enumeration<InetAddress> iaenum = interfaze.getInetAddresses(); iaenum.hasMoreElements();) {
                InetAddress interfaceAddress = iaenum.nextElement();
                if (interfaceAddress instanceof Inet6Address) {
                    address = interfaceAddress;
                }
            }
            registry = JmDNS.create(address);
            registry.registerService(service);
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    @Ignore
    public void testListenForMyService() throws IOException {
        System.out.println("Unit Test: testListenForMyService()");
        JmDNS registry = null;
        try {
            Capture<ServiceEvent> capServiceAddedEvent = EasyMock.newCapture();
            Capture<ServiceEvent> capServiceResolvedEvent = EasyMock.newCapture();
            // Add an expectation that the listener interface will be called once capture the object so I can verify it separately.
            serviceListenerMock.serviceAdded(capture(capServiceAddedEvent));
            serviceListenerMock.serviceResolved(capture(capServiceResolvedEvent));
            EasyMock.replay(serviceListenerMock);
            // EasyMock.makeThreadSafe(serviceListenerMock, false);

            registry = JmDNS.create();

            registry.addServiceListener(service.getType(), serviceListenerMock);

            registry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals("We did not get the right name for the added service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the added service:", service.getType(), info.getType());
            assertEquals("We did not get the right fully qualified name for the added service:", service.getQualifiedName(), info.getQualifiedName());

            // assertEquals("We should not get the server for the added service:", "", info.getServer());
            // assertEquals("We should not get the address for the added service:", null, info.getAddress());
            // assertEquals("We should not get the HostAddress for the added service:", "", info.getHostAddress());
            // assertEquals("We should not get the InetAddress for the added service:", null, info.getInetAddress());
            // assertEquals("We should not get the NiceTextString for the added service:", "", info.getNiceTextString());
            // assertEquals("We should not get the Priority for the added service:", 0, info.getPriority());
            // assertFalse("We should not get the PropertyNames for the added service:", info.getPropertyNames().hasMoreElements());
            // assertEquals("We should not get the TextBytes for the added service:", 0, info.getTextBytes().length);
            // assertEquals("We should not get the TextString for the added service:", null, info.getTextString());
            // assertEquals("We should not get the Weight for the added service:", 0, info.getWeight());
            // assertNotSame("We should not get the URL for the added service:", "", info.getURL());

            registry.requestServiceInfo(service.getType(), service.getName());

            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
            verify(serviceListenerMock);
            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
            assertEquals("Did not get the expected service info: ", service, resolvedInfo);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    @Ignore
    public void testListenForMyServiceAndList() throws IOException {
        System.out.println("Unit Test: testListenForMyServiceAndList()");
        JmDNS registry = null;
        try {
            Capture<ServiceEvent> capServiceAddedEvent = EasyMock.newCapture();
            Capture<ServiceEvent> capServiceResolvedEvent = EasyMock.newCapture();
            // Expect the listener to be called once and capture the result
            serviceListenerMock.serviceAdded(capture(capServiceAddedEvent));
            serviceListenerMock.serviceResolved(capture(capServiceResolvedEvent));
            replay(serviceListenerMock);

            registry = JmDNS.create();
            registry.addServiceListener(service.getType(), serviceListenerMock);
            registry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());

            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());

            // This will force the resolution of the service which in turn will get the listener called with a service resolved event.
            // The info associated with a service resolved event has all the information available.
            // Which in turn populates the ServiceInfo opbjects returned by JmDNS.list.
            ServiceInfo[] services = registry.list(info.getType());
            assertEquals("We did not get the expected number of services: ", 1, services.length);
            assertEquals("The service returned was not the one expected", service, services[0]);

            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
            verify(serviceListenerMock);
            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
            assertEquals("Did not get the expected service info: ", service, resolvedInfo);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    @Ignore
    public void testListenForServiceOnOtherRegistry() throws IOException {
        System.out.println("Unit Test: testListenForServiceOnOtherRegistry()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            Capture<ServiceEvent> capServiceAddedEvent = EasyMock.newCapture();
            Capture<ServiceEvent> capServiceResolvedEvent = EasyMock.newCapture();
            // Expect the listener to be called once and capture the result
            serviceListenerMock.serviceAdded(capture(capServiceAddedEvent));
            serviceListenerMock.serviceResolved(capture(capServiceResolvedEvent));
            replay(serviceListenerMock);

            registry = JmDNS.create();
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create();
            newServiceRegistry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
            verify(serviceListenerMock);
            Object result = capServiceResolvedEvent.getValue().getInfo();
            assertEquals("Did not get the expected service info: ", service, result);
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    @Ignore
    public void testWaitAndQueryForServiceOnOtherRegistry() throws IOException {
        System.out.println("Unit Test: testWaitAndQueryForServiceOnOtherRegistry()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            newServiceRegistry = JmDNS.create();
            registry = JmDNS.create();

            registry.registerService(service);

            ServiceInfo fetchedService = newServiceRegistry.getServiceInfo(service.getType(), service.getName());

            assertEquals("Did not get the expected service info: ", service, fetchedService);
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    public void testRegisterAndListServiceOnOtherRegistry() throws IOException, InterruptedException {
        System.out.println("Unit Test: testRegisterAndListServiceOnOtherRegistry()");
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try {
            registry = JmDNS.create("Registry");
            registry.registerService(service);

            newServiceRegistry = JmDNS.create("Listener");
            Thread.sleep(6000);
            ServiceInfo[] fetchedServices = newServiceRegistry.list(service.getType());
            assertEquals("Did not get the expected services listed:", 1, fetchedServices.length);
            assertEquals("Did not get the expected service type:", service.getType(), fetchedServices[0].getType());
            assertEquals("Did not get the expected service name:", service.getName(), fetchedServices[0].getName());
            assertEquals("Did not get the expected service fully qualified name:", service.getQualifiedName(), fetchedServices[0].getQualifiedName());
            newServiceRegistry.getServiceInfo(service.getType(), service.getName());

            assertEquals("Did not get the expected service info: ", service, fetchedServices[0]);
            registry.close();
            registry = null;
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            fetchedServices = newServiceRegistry.list(service.getType());
            assertEquals("The service was not cancelled after the close:", 0, fetchedServices.length);
        } finally {
            if (registry != null) registry.close();
            if (newServiceRegistry != null) newServiceRegistry.close();
        }
    }

    @Test
    public void testAddServiceListenerTwice() throws IOException {
        System.out.println("Unit Test: testAddServiceListenerTwice()");
        JmDNSImpl registry = null;
        try {
            registry = (JmDNSImpl) JmDNS.create();
            registry.addServiceListener("test", serviceListenerMock);

            // we will have 2 listeners, since the collector is implicitly added as well
            assertEquals(2, registry._serviceListeners.get("test").size());

            registry.addServiceListener("test", serviceListenerMock);
            assertEquals(2, registry._serviceListeners.get("test").size());
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testRemoveServiceListener() throws IOException {
        System.out.println("Unit Test: testRemoveServiceListener()");
        JmDNSImpl registry = null;
        try {
            registry = (JmDNSImpl) JmDNS.create();
            registry.addServiceListener("test", serviceListenerMock);
            // we will have 2 listeners, since the collector is implicitly added as well
            assertEquals(2, registry._serviceListeners.get("test").size());

            registry.removeServiceListener("test", serviceListenerMock);
            // the collector is left in place
            assertEquals(1, registry._serviceListeners.get("test").size());
        } finally {
            if (registry != null) registry.close();
        }
    }

    public static final class Receive extends Thread {
        MulticastSocket _socket;
        DatagramPacket  _in;

        public Receive(MulticastSocket socket, DatagramPacket in) {
            super("Test Receive Multicast");
            _socket = socket;
            _in = in;
        }

        @Override
        public void run() {
            try {
                _socket.receive(_in);
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

    private final static int MPORT = 8053;

    @Test
    @Ignore
    public void testTwoMulticastPortsAtOnce() throws UnknownHostException, IOException {
        System.out.println("Unit Test: testTwoMulticastPortsAtOnce()");
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
            DatagramPacket out = new DatagramPacket(firstMessage.getBytes("UTF-8"), firstMessage.length(), someInet, MPORT);
            DatagramPacket inFirst = new DatagramPacket(firstMessage.getBytes("UTF-8"), firstMessage.length(), someInet, MPORT);
            DatagramPacket inSecond = new DatagramPacket(firstMessage.getBytes("UTF-8"), firstMessage.length(), someInet, MPORT);
            Receive receiveSecond = new Receive(secondSocket, inSecond);
            receiveSecond.start();
            Receive receiveFirst = new Receive(firstSocket, inSecond);
            receiveFirst.start();
            firstSocket.send(out);
            if (receiveSecond.waitForReceive()) {
                Assert.fail("We did not receive the data in the second socket");
            }
            String fromFirst = new String(inSecond.getData(), "UTF-8");
            assertEquals("Expected the second socket to recieve the same message the first socket sent", firstMessage, fromFirst);
            // Make sure the first socket had read its own message
            if (receiveSecond.waitForReceive()) {
                Assert.fail("We did not receive the data in the first socket");
            }
            // Reverse the roles
            out = new DatagramPacket(secondMessage.getBytes("UTF-8"), secondMessage.length(), someInet, MPORT);
            inFirst = new DatagramPacket(secondMessage.getBytes("UTF-8"), secondMessage.length(), someInet, MPORT);
            receiveFirst = new Receive(firstSocket, inSecond);
            receiveFirst.start();

            secondSocket.send(out);
            if (receiveFirst.waitForReceive()) {
                Assert.fail("We did not receive the data in the first socket");
            }
            String fromSecond = new String(inFirst.getData(), "UTF-8");
            assertEquals("Expected the first socket to recieve the same message the second socket sent", secondMessage, fromSecond);
        } finally {
            if (firstSocket != null) firstSocket.close();
            if (secondSocket != null) secondSocket.close();
        }
    }

    @Test
    public void testListMyServiceWithToLowerCase() throws IOException, InterruptedException {
        System.out.println("Unit Test: testListMyServiceWithToLowerCase()");
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(serviceKey, text.getBytes());
        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);

            // with toLowerCase
            ServiceInfo[] services = registry.list(service.getType().toLowerCase());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            services = registry.list(service.getType().toLowerCase());
            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    public void testListMyServiceWithoutLowerCase() throws IOException, InterruptedException {
        System.out.println("Unit Test: testListMyServiceWithoutLowerCase()");
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(serviceKey, text.getBytes());
        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
        JmDNS registry = null;
        try {
            registry = JmDNS.create();
            registry.registerService(service);

            // without toLowerCase
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
            // now unregister and make sure it's gone
            registry.unregisterService(services[0]);
            // According to the spec the record disappears from the cache 1s after it has been unregistered
            // without sleeping for a while, the service would not be unregistered fully
            Thread.sleep(1500);
            services = registry.list(service.getType());
            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
        } finally {
            if (registry != null) registry.close();
        }
    }

    @Test
    @Ignore
    public void shouldNotNotifyServiceListenersForServiceResolveAfterServiceRemoved() throws UnknownHostException, IOException, InterruptedException {
        String firstType = "_test._type1.local.";
        String secondType = "_test._type2.local.";

        JmmDNS jmmdns = JmmDNS.Factory.getInstance();

        EventStoringServiceListener firstListener = new EventStoringServiceListener(firstType);
        jmmdns.addServiceListener(firstType, firstListener);

        InetAddress[] addresses = jmmdns.getInetAddresses();
        JmDNS[] dns = new JmDNS[addresses.length];
        Map<String, String> txtRecord = new HashMap<String, String>();
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

        for (int i = 0; i < dns.length; i++) {
            dns[i].close();
        }

        assertFalse(firstListener.isServiceAddedReceived());
        assertTrue(firstListener.isServiceRemovedReceived());

        assertFalse(firstListener.isServiceResolvedReceived());
    }

}