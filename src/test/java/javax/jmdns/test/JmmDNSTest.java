package javax.jmdns.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.test.util.ReflectionUtils;

class JmmDNSTest {
    private ServiceListener serviceListenerMock;
    private ServiceInfo service;
    private static final String SERVICE_KEY = "srvname"; // Max 9 chars

    @BeforeEach
    public void setup() {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(SERVICE_KEY, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        serviceListenerMock = mock(ServiceListener.class);
    }

    @Test
    void testCreate() throws IOException, NoSuchFieldException, IllegalAccessException {
        JmmDNS registry = JmmDNS.Factory.getInstance();
        assertNotNull(registry);
        registry.close();
        AtomicBoolean closed = (AtomicBoolean) ReflectionUtils.getInternalState(registry, "_closed");
        assertTrue(closed.get());
    }

    @Test
    void testRegisterService() throws IOException, InterruptedException {
        try (JmmDNS registry = JmmDNS.Factory.getInstance()) {
            registry.registerService(service);
            Thread.sleep(6000);

            ServiceInfo[] services = registry.list(service.getType());
            assertTrue(services.length > 0, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
        }
    }

    // @Test
    // public void testUnregisterService() throws IOException, InterruptedException {
    // System.out.println("Unit Test: testUnregisterService()");
    // JmmDNS registry = null;
    // try {
    // registry = JmmDNS.Factory.getInstance();
    // registry.registerService(service);
    //
    // ServiceInfo[] services = registry.list(service.getType());
    // assertTrue("We should see the service we just registered: ", services.length > 0);
    // assertEquals(service, services[0]);
    //
    // // now unregister and make sure it's gone
    // registry.unregisterService(service);
    //
    // // According to the spec the record disappears from the cache 1s after it has been unregistered
    // // without sleeping for a while, the service would not be unregistered fully
    // Thread.sleep(1500);
    //
    // services = registry.list(service.getType());
    // assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
    // } finally {
    // if (registry != null) registry.close();
    // }
    // }
    //
    // @Test
    // public void testUnregisterAndReregisterService() throws IOException, InterruptedException {
    // System.out.println("Unit Test: testUnregisterAndReregisterService()");
    // JmmDNS registry = null;
    // try {
    // registry = JmmDNS.Factory.getInstance();
    // registry.registerService(service);
    //
    // ServiceInfo[] services = registry.list(service.getType());
    // assertTrue("We should see the service we just registered: ", services.length > 0);
    // assertEquals(service, services[0]);
    //
    // // now unregister and make sure it's gone
    // registry.unregisterService(services[0]);
    //
    // // According to the spec the record disappears from the cache 1s after it has been unregistered
    // // without sleeping for a while, the service would not be unregistered fully
    // Thread.sleep(1500);
    //
    // services = registry.list(service.getType());
    // assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
    //
    // registry.registerService(service);
    // Thread.sleep(5000);
    // services = registry.list(service.getType());
    // assertTrue("We should see the service we just reregistered: ", services != null && services.length > 0);
    // } finally {
    // if (registry != null) registry.close();
    // }
    // }

    @Test
    void testQueryMyService() throws IOException, InterruptedException {
        try (JmmDNS registry = JmmDNS.Factory.getInstance()) {
            registry.registerService(service);
            Thread.sleep(6000);

            ServiceInfo[] queriedService = registry.getServiceInfos(service.getType(), service.getName());
            assertTrue(queriedService.length > 0, "We expect to see the service we just registered");
            assertEquals(service, queriedService[0]);
        }
    }

    @Test
    void testListMyService() throws IOException, InterruptedException {
        try (JmmDNS registry = JmmDNS.Factory.getInstance()) {
            registry.registerService(service);
            Thread.sleep(6000);

            ServiceInfo[] services = registry.list(service.getType());
            assertTrue(services.length > 0, "We should see the service we just registered: ");
            assertEquals(service, services[0]);
        }
    }

//    @Test
//    void testListenForMyService() throws IOException {
//        JmmDNS registry = null;
//        try {
//            Capture<ServiceEvent> capServiceAddedEvent = EasyMock.newCapture();
//            Capture<ServiceEvent> capServiceResolvedEvent = EasyMock.newCapture();
//            // Add an expectation that the listener interface will be called once capture the object so I can verify it separately.
//            serviceListenerMock.serviceAdded(capture(capServiceAddedEvent));
//            serviceListenerMock.serviceResolved(capture(capServiceResolvedEvent));
//            EasyMock.replay(serviceListenerMock);
//            // EasyMock.makeThreadSafe(serviceListenerMock, false);
//
//            registry = JmmDNS.Factory.getInstance();
//
//            registry.addServiceListener(service.getType(), serviceListenerMock);
//
//            registry.registerService(service);
//
//            // We get the service added event when we register the service. However the service has not been resolved at this point.
//            // The info associated with the event only has the minimum information i.e. name and type.
//            assertTrue(capServiceAddedEvent.hasCaptured(), "We did not get the service added event.");
//            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
//            assertEquals(service.getName(), info.getName(), "We did not get the right name for the added service:");
//            assertEquals(service.getType(), info.getType(), "We did not get the right type for the added service:");
//            assertEquals(service.getQualifiedName(), info.getQualifiedName(), "We did not get the right fully qualified name for the added service:");
//
//            registry.requestServiceInfo(service.getType(), service.getName());
//
//            assertTrue(capServiceResolvedEvent.hasCaptured(), "We did not get the service resolved event.");
//            verify(serviceListenerMock);
//            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
//            assertEquals(service, resolvedInfo, "Did not get the expected service info: ");
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    void testListenForMyServiceAndList() throws IOException {
//        JmmDNS registry = null;
//        try {
//            Capture<ServiceEvent> capServiceAddedEvent = EasyMock.newCapture();
//            Capture<ServiceEvent> capServiceResolvedEvent = EasyMock.newCapture();
//            // Expect the listener to be called once and capture the result
//            serviceListenerMock.serviceAdded(capture(capServiceAddedEvent));
//            serviceListenerMock.serviceResolved(capture(capServiceResolvedEvent));
//            replay(serviceListenerMock);
//
//            registry = JmmDNS.Factory.getInstance();
//            registry.addServiceListener(service.getType(), serviceListenerMock);
//            registry.registerService(service);
//
//            // We get the service added event when we register the service. However the service has not been resolved at this point.
//            // The info associated with the event only has the minimum information i.e. name and type.
//            assertTrue(capServiceAddedEvent.hasCaptured(), "We did not get the service added event.");
//
//            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
//            assertEquals(service.getName(), info.getName(), "We did not get the right name for the resolved service:");
//            assertEquals(service.getType(), info.getType(), "We did not get the right type for the resolved service:");
//
//            // This will force the resolution of the service which in turn will get the listener called with a service resolved event.
//            // The info associated with a service resolved event has all the information available.
//            // Which in turn populates the ServiceInfo opbjects returned by JmmDNS.list.
//            ServiceInfo[] services = registry.list(info.getType());
//            assertTrue(services.length > 0, "We did not get the expected number of services: ");
//            assertEquals(service, services[0], "The service returned was not the one expected");
//
//            assertTrue(capServiceResolvedEvent.hasCaptured(), "We did not get the service resolved event.");
//            verify(serviceListenerMock);
//            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
//            assertEquals(service, resolvedInfo, "Did not get the expected service info: ");
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }

    // @Test
    // public void testListMyServiceWithToLowerCase() throws IOException, InterruptedException {
    // System.out.println("Unit Test: testListMyServiceWithToLowerCase()");
    // String text = "Test hypothetical web server";
    // Map<String, byte[]> properties = new HashMap<String, byte[]>();
    // properties.put(serviceKey, text.getBytes());
    // service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
    // JmmDNS registry = null;
    // try {
    // registry = JmmDNS.Factory.getInstance();
    // registry.registerService(service);
    //
    // // with toLowerCase
    // ServiceInfo[] services = registry.list(service.getType().toLowerCase());
    // assertTrue("We should see the service we just registered: ", services.length > 0);
    // assertEquals(service, services[0]);
    // // now unregister and make sure it's gone
    // registry.unregisterService(services[0]);
    // // According to the spec the record disappears from the cache 1s after it has been unregistered
    // // without sleeping for a while, the service would not be unregistered fully
    // Thread.sleep(1500);
    // services = registry.list(service.getType().toLowerCase());
    // assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
    // } finally {
    // if (registry != null) registry.close();
    // }
    // }
    //
    // @Test
    // public void testListMyServiceWithoutLowerCase() throws IOException, InterruptedException {
    // System.out.println("Unit Test: testListMyServiceWithoutLowerCase()");
    // String text = "Test hypothetical web server";
    // Map<String, byte[]> properties = new HashMap<String, byte[]>();
    // properties.put(serviceKey, text.getBytes());
    // service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
    // JmmDNS registry = null;
    // try {
    // registry = JmmDNS.Factory.getInstance();
    // registry.registerService(service);
    //
    // // without toLowerCase
    // ServiceInfo[] services = registry.list(service.getType());
    // assertTrue("We should see the service we just registered: ", services.length > 0);
    // assertEquals(service, services[0]);
    // // now unregister and make sure it's gone
    // registry.unregisterService(services[0]);
    // // According to the spec the record disappears from the cache 1s after it has been unregistered
    // // without sleeping for a while, the service would not be unregistered fully
    // Thread.sleep(1500);
    // services = registry.list(service.getType());
    // assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
    // } finally {
    // if (registry != null) registry.close();
    // }
    // }
}
