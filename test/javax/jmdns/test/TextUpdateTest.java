/**
 *
 */
package javax.jmdns.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertNotNull;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class TextUpdateTest
{

    private ServiceInfo service;
    private ServiceListener serviceListenerMock;

    @Before
    public void setup()
    {
        service = ServiceInfo.create("_html._http._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, "Test hypothetical web server");
        serviceListenerMock = createNiceMock(ServiceListener.class);
    }

    @Test
    public void testListenForTextUpdateOnOtherRegistry() throws IOException
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
            ServiceInfo result = capServiceResolvedEvent.getValue().getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", service, result);
            assertEquals("Did not get the expected service info text: ", service.getTextString(), result.getTextString());

            String text = "Test improbable web server";
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            service.setText(out.toByteArray());
            assertTrue("We did not get the service text updated event.", capServiceResolvedEvent.hasCaptured());
            result = capServiceResolvedEvent.getValue().getInfo();
            assertEquals("Did not get the expected service info text: ", text, result.getTextString());

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
