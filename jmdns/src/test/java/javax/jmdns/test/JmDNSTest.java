package javax.jmdns.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class JmDNSTest
{

    @SuppressWarnings("unused")
    private ServiceTypeListener typeListenerMock;
    private ServiceListener serviceListenerMock;
    private ServiceInfo service;

    private final static String serviceKey = "srvname"; // Max 9 chars

    @Before
    public void setup()
    {
        boolean log = true;
        if (log)
        {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.FINEST);
            for (Enumeration<String> enumerator = LogManager.getLogManager().getLoggerNames(); enumerator.hasMoreElements();)
            {
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
    public void testCreate() throws IOException
    {
        JmDNS registry = JmDNS.create();
        registry.close();
    }

    @Test
    public void testCreateINet() throws IOException
    {
        JmDNS registry = JmDNS.create(InetAddress.getLocalHost());
        // assertEquals("We did not register on the local host inet:", InetAddress.getLocalHost(), registry.getInterface());
        registry.close();
    }

    @Test
    public void testRegisterService() throws IOException
    {
        JmDNS registry = null;
        try
        {
            registry = JmDNS.create();
            registry.registerService(service);
        }
        finally
        {
            if (registry != null)
                registry.close();
        }
    }

    @Test
    public void testQueryMyService() throws IOException
    {
        JmDNS registry = null;
        try
        {
            registry = JmDNS.create();
            registry.registerService(service);
            ServiceInfo queriedService = registry.getServiceInfo(service.getType(), service.getName());
            assertEquals(service, queriedService);
        }
        finally
        {
            if (registry != null)
                registry.close();
        }
    }

    @Test
    public void testListMyService() throws IOException
    {
        JmDNS registry = null;
        try
        {
            registry = JmDNS.create();
            registry.registerService(service);
            ServiceInfo[] services = registry.list(service.getType());
            assertEquals("We should see the service we just registered: ", 1, services.length);
            assertEquals(service, services[0]);
        }
        finally
        {
            if (registry != null)
                registry.close();
        }
    }

    @Test
    public void testListenForMyService() throws IOException
    {
        JmDNS registry = null;
        try
        {
            Capture<ServiceEvent> capServiceAddedEvent = new Capture<ServiceEvent>();
            Capture<ServiceEvent> capServiceResolvedEvent = new Capture<ServiceEvent>();
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
        }
        finally
        {
            if (registry != null)
                registry.close();
        }
    }

    @Test
    public void testListenForMyServiceAndList() throws IOException
    {
        JmDNS registry = null;
        try
        {
            Capture<ServiceEvent> capServiceAddedEvent = new Capture<ServiceEvent>();
            Capture<ServiceEvent> capServiceResolvedEvent = new Capture<ServiceEvent>();
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
        }
        finally
        {
            if (registry != null)
                registry.close();
        }
    }

    @Test
    public void testListenForServiceOnOtherRegistry() throws IOException
    {
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try
        {
            Capture<ServiceEvent> capServiceAddedEvent = new Capture<ServiceEvent>();
            Capture<ServiceEvent> capServiceResolvedEvent = new Capture<ServiceEvent>();
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
        }
        finally
        {
            if (registry != null)
                registry.close();
            if (newServiceRegistry != null)
                newServiceRegistry.close();
        }
    }

    @Test
    public void testWaitAndQueryForServiceOnOtherRegistry() throws IOException
    {
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try
        {
            newServiceRegistry = JmDNS.create();
            registry = JmDNS.create();

            registry.registerService(service);

            ServiceInfo fetchedService = newServiceRegistry.getServiceInfo(service.getType(), service.getName());

            assertEquals("Did not get the expected service info: ", service, fetchedService);
        }
        finally
        {
            if (registry != null)
                registry.close();
            if (newServiceRegistry != null)
                newServiceRegistry.close();
        }
    }

    @Test
    public void testRegisterAndListServiceOnOtherRegistry() throws IOException, InterruptedException
    {
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try
        {
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
            fetchedServices = newServiceRegistry.list(service.getType());
            assertEquals("The service was not cancelled after the close:", 0, fetchedServices.length);
        }
        finally
        {
            if (registry != null)
                registry.close();
            if (newServiceRegistry != null)
                newServiceRegistry.close();
        }
    }

    @Test
    public void testTwoMulticastPortsAtOnce() throws UnknownHostException, IOException
    {
        MulticastSocket firstSocket = null;
        MulticastSocket secondSocket = null;
        try
        {
            String firstMessage = "ping";
            String secondMessage = "pong";
            InetAddress someInet = InetAddress.getByName("224.0.0.252");
            firstSocket = new MulticastSocket(8053);
            secondSocket = new MulticastSocket(8053);

            firstSocket.joinGroup(someInet);
            secondSocket.joinGroup(someInet);

            DatagramPacket data = new DatagramPacket(firstMessage.getBytes("UTF-8"), firstMessage.length(), someInet, 8053);
            firstSocket.send(data);

            secondSocket.receive(data);
            String fromFirst = new String(data.getData(), "UTF-8");
            assertEquals("Expected the second socket to recieve the same message the first socket sent", firstMessage, fromFirst);
            // Make sure the first socket had read its own message
            firstSocket.receive(data);

            data = new DatagramPacket(secondMessage.getBytes("UTF-8"), secondMessage.length(), someInet, 8053);
            secondSocket.send(data);

            firstSocket.receive(data);
            String fromSecond = new String(data.getData(), "UTF-8");
            assertEquals("Expected the first socket to recieve the same message the second socket sent", secondMessage, fromSecond);
        }
        finally
        {
            if (firstSocket != null)
                firstSocket.close();
            if (secondSocket != null)
                secondSocket.close();
        }
    }

}