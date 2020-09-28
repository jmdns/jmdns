/**
 *
 */
package javax.jmdns.test;

import static org.junit.Assert.assertEquals;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.junit.Before;
import org.junit.Test;

public class ServiceInfoTest {

    @Before
    public void setup() {
        // Placeholder
    }

    @Test
    public void testEncodeDecodeProperties() {

        String service_type = "_ros-master._tcp.local.";
        String service_name = "RosMaster";
        int service_port = 8888;
        String service_key = "description"; // Max 9 chars
        String service_text = "Hypothetical ros master";
        Map<String, byte[]> properties = new HashMap<String, byte[]>();
        properties.put(service_key, service_text.getBytes());
        ServiceInfo service_info = null;
        service_info = ServiceInfo.create(service_type, service_name, service_port, ""); // case 1, no text
        assertEquals("We should have got the same properties (Empty):", null, service_info.getPropertyString(service_key));
        service_info = ServiceInfo.create(service_type, service_name, service_port, 0, 0, true, service_key + "=" + service_text); // case 2, textual description
        assertEquals("We should have got the same properties (String):", service_text, service_info.getPropertyString(service_key));
        service_info = ServiceInfo.create(service_type, service_name, service_port, 0, 0, true, properties); // case 3, properties assigned textual description
        assertEquals("We should have got the same properties (Map):", service_text, service_info.getPropertyString(service_key));

    }

    @Test
    public void testDecodePropertiesWithoutEqualsSign() {
        String service_type = "_ros-master._tcp.local.";
        String service_name = "RosMaster";
        int service_port = 8888;
        ServiceInfo service_info = null;
        // Represents TXT records "a" "b=c"
        byte[] txt = {1, 97, 3, 98, 61, 99};
        service_info = ServiceInfo.create(service_type, service_name, service_port, 0, 0, txt);

        Set<String> expectedKeys = new HashSet<String>();
        expectedKeys.add("a");
        expectedKeys.add("b");

        Enumeration<String> enumeration = service_info.getPropertyNames();
        Set<String> keys = new HashSet<String>();
        while (enumeration.hasMoreElements()) {
            keys.add(enumeration.nextElement());
        }
        assertEquals(expectedKeys, keys);
        assertEquals("c", service_info.getPropertyString("b"));
    }

}
