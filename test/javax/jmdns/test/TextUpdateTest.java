/**
 *
 */
package javax.jmdns.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

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

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class TextUpdateTest
{

    private ServiceInfo service;
    private MockListener serviceListenerMock;

    private final static String serviceKey = "srvname"; // Max 9 chars

    public static class MockListener implements ServiceListener
    {

        private final List<ServiceEvent> _serviceAdded = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));
        private final List<ServiceEvent> _serviceRemoved = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));
        private final List<ServiceEvent> _serviceResolved = Collections.synchronizedList(new ArrayList<ServiceEvent>(2));

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceAdded(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceAdded(ServiceEvent event)
        {
            try
            {
                _serviceAdded.add((ServiceEvent) event.clone());
            }
            catch (CloneNotSupportedException exception)
            {
                //
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceRemoved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceRemoved(ServiceEvent event)
        {
            try
            {
                _serviceRemoved.add((ServiceEvent) event.clone());
            }
            catch (CloneNotSupportedException exception)
            {
                //
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.jmdns.ServiceListener#serviceResolved(javax.jmdns.ServiceEvent)
         */
        @Override
        public void serviceResolved(ServiceEvent event)
        {
            try
            {
                _serviceResolved.add((ServiceEvent) event.clone());
            }
            catch (CloneNotSupportedException exception)
            {
                //
            }
        }

        public List<ServiceEvent> servicesAdded()
        {
            return _serviceAdded;
        }

        public List<ServiceEvent> servicesRemoved()
        {
            return _serviceRemoved;
        }

        public List<ServiceEvent> servicesResolved()
        {
            return _serviceResolved;
        }

        public synchronized void reset()
        {
            _serviceAdded.clear();
            _serviceRemoved.clear();
            _serviceResolved.clear();
        }

        @Override
        public String toString()
        {
            StringBuilder aLog = new StringBuilder();
            aLog.append("Services Added: " + _serviceAdded.size());
            for (ServiceEvent event : _serviceAdded)
            {
                aLog.append("\n\tevent name: '");
                aLog.append(event.getName());
                aLog.append("' type: '");
                aLog.append(event.getType());
                aLog.append("' info: '");
                aLog.append(event.getInfo());
            }
            aLog.append("\nServices Removed: " + _serviceRemoved.size());
            for (ServiceEvent event : _serviceRemoved)
            {
                aLog.append("\n\tevent name: '");
                aLog.append(event.getName());
                aLog.append("' type: '");
                aLog.append(event.getType());
                aLog.append("' info: '");
                aLog.append(event.getInfo());
            }
            aLog.append("\nServices Resolved: " + _serviceResolved.size());
            for (ServiceEvent event : _serviceResolved)
            {
                aLog.append("\n\tevent name: '");
                aLog.append(event.getName());
                aLog.append("' type: '");
                aLog.append(event.getType());
                aLog.append("' info: '");
                aLog.append(event.getInfo());
            }
            return aLog.toString();
        }

    }

    @Before
    public void setup()
    {
        String text = "Test hypothetical web server";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(serviceKey, text.getBytes());
        service = ServiceInfo.create("_html._tcp.local.", "apache-someuniqueid", 80, 0, 0, true, properties);
        serviceListenerMock = new MockListener();
    }

    @Test
    public void testListenForTextUpdateOnOtherRegistry() throws IOException, InterruptedException
    {
        JmDNS registry = null;
        JmDNS newServiceRegistry = null;
        try
        {
            registry = JmDNS.create("Listener");
            registry.addServiceListener(service.getType(), serviceListenerMock);
            //
            newServiceRegistry = JmDNS.create("Registry");
            newServiceRegistry.registerService(service);

            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesAdded = serviceListenerMock.servicesAdded();
            assertTrue("We did not get the service added event.", servicesAdded.size() == 1);
            ServiceInfo info = servicesAdded.get(servicesAdded.size() - 1).getInfo();
            assertEquals("We did not get the right name for the resolved service:", service.getName(), info.getName());
            assertEquals("We did not get the right type for the resolved service:", service.getType(), info.getType());
            // We get the service added event when we register the service. However the service has not been resolved at this point.
            // The info associated with the event only has the minimum information i.e. name and type.
            List<ServiceEvent> servicesResolved = serviceListenerMock.servicesResolved();
            assertTrue("We did not get the service resolved event.", servicesResolved.size() == 1);
            ServiceInfo result = servicesResolved.get(servicesAdded.size() - 1).getInfo();
            assertNotNull("Did not get the expected service info: ", result);
            assertEquals("Did not get the expected service info: ", service, result);
            assertEquals("Did not get the expected service info text: ", service.getPropertyString(serviceKey), result.getPropertyString(serviceKey));
            serviceListenerMock.reset();

            String text = "Test improbable web server";
            Map<String, byte[]> properties = new HashMap<String, byte[]>();
            properties.put(serviceKey, text.getBytes());
            service.setText(properties);
            Thread.sleep(2000);
            servicesResolved = serviceListenerMock.servicesResolved();
            assertTrue("We did not get the service text updated event.", servicesResolved.size() == 1);
            result = servicesResolved.get(servicesResolved.size() - 1).getInfo();
            assertEquals("Did not get the expected service info text: ", text, result.getPropertyString(serviceKey));

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
