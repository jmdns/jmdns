package javax.jmdns.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class JmmDNSTest {

    @SuppressWarnings("unused")
    private ServiceTypeListener typeListenerMock;
    private ServiceListener     serviceListenerMock;
    private ServiceInfo         service;

    private final static String serviceKey = "srvname"; // Max 9 chars

    @Before
    public void setup() {
        boolean log = false;
        if (log) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            for (Enumeration<String> enumerator = LogManager.getLogManager().getLoggerNames(); enumerator.hasMoreElements();) {
                String loggerName = enumerator.nextElement();
                Logger logger = Logger.getLogger(loggerName);
                logger.addHandler(handler);
                logger.setLevel(Level.FINEST);
            }
        }

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
        JmmDNS registry = JmmDNS.Factory.getInstance();
        registry.close();
    }

    @Test
    public void testCreateINet() throws IOException {
        System.out.println("Unit Test: testCreateINet()");
        JmmDNS registry = JmmDNS.Factory.getInstance();
        // assertEquals("We did not register on the local host inet:", InetAddress.getLocalHost(), registry.getInterface());
        registry.close();
    }

    @Test
    public void testRegisterService() throws IOException, InterruptedException {
        System.out.println("Unit Test: testRegisterService()");
        JmmDNS registry = null;
        try {
            registry = JmmDNS.Factory.getInstance();
            registry.registerService(service);

            Thread.sleep(1000);
            ServiceInfo[] services = registry.list(service.getType());
            assertTrue("We should see the service we just registered: ", services.length > 0);
            assertEquals(service, services[0]);
        } finally {
            if (registry != null) registry.close();
        }
    }

//    @Test
//    public void testUnregisterService() throws IOException, InterruptedException {
//        System.out.println("Unit Test: testUnregisterService()");
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//
//            Thread.sleep(1000);
//            ServiceInfo[] services = registry.list(service.getType());
//            assertTrue("We should see the service we just registered: ", services.length > 0);
//            assertEquals(service, services[0]);
//
//            // now unregister and make sure it's gone
//            registry.unregisterService(service);
//
//            // According to the spec the record disappears from the cache 1s after it has been unregistered
//            // without sleeping for a while, the service would not be unregistered fully
//            Thread.sleep(1500);
//
//            services = registry.list(service.getType());
//            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testUnregisterAndReregisterService() throws IOException, InterruptedException {
//        System.out.println("Unit Test: testUnregisterAndReregisterService()");
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//
//            ServiceInfo[] services = registry.list(service.getType());
//            assertTrue("We should see the service we just registered: ", services.length > 0);
//            assertEquals(service, services[0]);
//
//            // now unregister and make sure it's gone
//            registry.unregisterService(services[0]);
//
//            // According to the spec the record disappears from the cache 1s after it has been unregistered
//            // without sleeping for a while, the service would not be unregistered fully
//            Thread.sleep(1500);
//
//            services = registry.list(service.getType());
//            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
//
//            registry.registerService(service);
//            Thread.sleep(5000);
//            services = registry.list(service.getType());
//            assertTrue("We should see the service we just reregistered: ", services != null && services.length > 0);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testQueryMyService() throws IOException {
//        System.out.println("Unit Test: testQueryMyService()");
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//            ServiceInfo[] queriedService = registry.getServiceInfos(service.getType(), service.getName());
//            assertTrue("We expect to see the service we just registered", queriedService.length > 0);
//            assertEquals(service, queriedService[0]);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testListMyService() throws IOException {
//        System.out.println("Unit Test: testListMyService()");
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//            ServiceInfo[] services = registry.list(service.getType());
//            assertEquals("We should see the service we just registered: ", 1, services.length);
//            assertEquals(service, services[0]);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testListenForMyService() throws IOException {
//        System.out.println("Unit Test: testListenForMyService()");
//        JmmDNS registry = null;
//        try {
//            Capture<ServiceEvent> capServiceAddedEvent = new Capture<ServiceEvent>();
//            Capture<ServiceEvent> capServiceResolvedEvent = new Capture<ServiceEvent>();
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
//            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
//            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
//            assertEquals("We did not get the right name for the added service:", service.getName(), info.getName());
//            assertEquals("We did not get the right type for the added service:", service.getType(), info.getType());
//            assertEquals("We did not get the right fully qualified name for the added service:", service.getQualifiedName(), info.getQualifiedName());
//
//            // assertEquals("We should not get the server for the added service:", "", info.getServer());
//            // assertEquals("We should not get the address for the added service:", null, info.getAddress());
//            // assertEquals("We should not get the HostAddress for the added service:", "", info.getHostAddress());
//            // assertEquals("We should not get the InetAddress for the added service:", null, info.getInetAddress());
//            // assertEquals("We should not get the NiceTextString for the added service:", "", info.getNiceTextString());
//            // assertEquals("We should not get the Priority for the added service:", 0, info.getPriority());
//            // assertFalse("We should not get the PropertyNames for the added service:", info.getPropertyNames().hasMoreElements());
//            // assertEquals("We should not get the TextBytes for the added service:", 0, info.getTextBytes().length);
//            // assertEquals("We should not get the TextString for the added service:", null, info.getTextString());
//            // assertEquals("We should not get the Weight for the added service:", 0, info.getWeight());
//            // assertNotSame("We should not get the URL for the added service:", "", info.getURL());
//
//            registry.requestServiceInfo(service.getType(), service.getName());
//
//            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
//            verify(serviceListenerMock);
//            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
//            assertEquals("Did not get the expected service info: ", service, resolvedInfo);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testListenForMyServiceAndList() throws IOException {
//        System.out.println("Unit Test: testListenForMyServiceAndList()");
//        JmmDNS registry = null;
//        try {
//            Capture<ServiceEvent> capServiceAddedEvent = new Capture<ServiceEvent>();
//            Capture<ServiceEvent> capServiceResolvedEvent = new Capture<ServiceEvent>();
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
//            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
//
//            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
//            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
//            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
//
//            // This will force the resolution of the service which in turn will get the listener called with a service resolved event.
//            // The info associated with a service resolved event has all the information available.
//            // Which in turn populates the ServiceInfo opbjects returned by JmmDNS.list.
//            ServiceInfo[] services = registry.list(info.getType());
//            assertEquals("We did not get the expected number of services: ", 1, services.length);
//            assertEquals("The service returned was not the one expected", service, services[0]);
//
//            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
//            verify(serviceListenerMock);
//            ServiceInfo resolvedInfo = capServiceResolvedEvent.getValue().getInfo();
//            assertEquals("Did not get the expected service info: ", service, resolvedInfo);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testListMyServiceWithToLowerCase() throws IOException, InterruptedException {
//        System.out.println("Unit Test: testListMyServiceWithToLowerCase()");
//        String text = "Test hypothetical web server";
//        Map<String, byte[]> properties = new HashMap<String, byte[]>();
//        properties.put(serviceKey, text.getBytes());
//        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//
//            // with toLowerCase
//            ServiceInfo[] services = registry.list(service.getType().toLowerCase());
//            assertEquals("We should see the service we just registered: ", 1, services.length);
//            assertEquals(service, services[0]);
//            // now unregister and make sure it's gone
//            registry.unregisterService(services[0]);
//            // According to the spec the record disappears from the cache 1s after it has been unregistered
//            // without sleeping for a while, the service would not be unregistered fully
//            Thread.sleep(1500);
//            services = registry.list(service.getType().toLowerCase());
//            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
//
//    @Test
//    public void testListMyServiceWithoutLowerCase() throws IOException, InterruptedException {
//        System.out.println("Unit Test: testListMyServiceWithoutLowerCase()");
//        String text = "Test hypothetical web server";
//        Map<String, byte[]> properties = new HashMap<String, byte[]>();
//        properties.put(serviceKey, text.getBytes());
//        service = ServiceInfo.create("_HtmL._TcP.lOcAl.", "apache-someUniqueid", 80, 0, 0, true, properties);
//        JmmDNS registry = null;
//        try {
//            registry = JmmDNS.Factory.getInstance();
//            registry.registerService(service);
//
//            // without toLowerCase
//            ServiceInfo[] services = registry.list(service.getType());
//            assertEquals("We should see the service we just registered: ", 1, services.length);
//            assertEquals(service, services[0]);
//            // now unregister and make sure it's gone
//            registry.unregisterService(services[0]);
//            // According to the spec the record disappears from the cache 1s after it has been unregistered
//            // without sleeping for a while, the service would not be unregistered fully
//            Thread.sleep(1500);
//            services = registry.list(service.getType());
//            assertTrue("We should not see the service we just unregistered: ", services == null || services.length == 0);
//        } finally {
//            if (registry != null) registry.close();
//        }
//    }
}
