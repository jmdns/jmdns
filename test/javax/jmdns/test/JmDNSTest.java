package javax.jmdns.test;

import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;
import static org.easymock.EasyMock.*;

public class JmDNSTest
{

    @SuppressWarnings("unused")
    private ServiceTypeListener typeListenerMock;
    private ServiceListener serviceListenerMock;
    private ServiceInfo service;

    @Before
    public void setup()
    {
        service = ServiceInfo.create("_html._http._tcp.local.", "apache-someuniqueid", 80, "Test hypothetical web server");
        typeListenerMock = createMock(ServiceTypeListener.class);
        serviceListenerMock = createMock(ServiceListener.class);
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
            replay(serviceListenerMock);

            registry = JmDNS.create();
            registry.addServiceListener(service.getType(), serviceListenerMock);
            registry.registerService(service);

            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
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

            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            ServiceInfo info = capServiceAddedEvent.getValue().getInfo();
            // This will force the resolution of the service which in turn will get the listener called with a service resolved event.
            // The info associated with a service resolved event has all the information available.
            ServiceInfo[] services = registry.list(info.getType());
            assertEquals("We did not get the expected number of services: ", 1, services.length);
            assertEquals("The service returned was not the one expected", service, services[0]);

            assertTrue("We did not get the service resolved event.", capServiceResolvedEvent.hasCaptured());
            verify(serviceListenerMock);
            Object result = capServiceResolvedEvent.getValue().getInfo();
            assertEquals("Did not get the expected service info: ", service, result);
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

            assertTrue("We did not get the service added event.", capServiceAddedEvent.hasCaptured());
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

}