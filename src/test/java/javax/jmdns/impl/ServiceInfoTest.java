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

import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceInfoTest {

    @Test
    void testEncodeDecodeProperties() {
        String serviceType = "_ros-master._tcp.local.";
        String serviceName = "RosMaster";
        int servicePort = 8888;
        String serviceKey = "description"; // Max 9 chars
        String serviceText = "Hypothetical ros master";
        Map<String, byte[]> properties = new HashMap<>();
        properties.put(serviceKey, serviceText.getBytes());
        ServiceInfo serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, ""); // case 1, no text
        assertNull(serviceInfo.getPropertyString(serviceKey), "We should have got the same properties (Empty):");
        serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, 0, 0, true, serviceKey + "=" + serviceText); // case 2, textual description
        assertEquals(serviceText, serviceInfo.getPropertyString(serviceKey), "We should have got the same properties (String):");
        serviceInfo = ServiceInfo.create(serviceType, serviceName, servicePort, 0, 0, true, properties); // case 3, properties assigned textual description
        assertEquals(serviceText, serviceInfo.getPropertyString(serviceKey), "We should have got the same properties (Map):");
    }

    @Test
    void testDecodePropertiesWithoutEqualsSign() {
        String serviceType = "_ros-master._tcp.local.";
        String serviceName = "RosMaster";
        int serviceOort = 8888;
        // Represents TXT records "a" "b=c"
        byte[] txt = {1, 97, 3, 98, 61, 99};
        ServiceInfo serviceInfo = ServiceInfo.create(serviceType, serviceName, serviceOort, 0, 0, txt);

        Set<String> expectedKeys = new HashSet<>();
        expectedKeys.add("a");
        expectedKeys.add("b");

        Enumeration<String> enumeration = serviceInfo.getPropertyNames();
        Set<String> keys = new HashSet<>();
        while (enumeration.hasMoreElements()) {
            keys.add(enumeration.nextElement());
        }
        assertEquals(expectedKeys, keys);
        assertEquals("c", serviceInfo.getPropertyString("b"));
    }
}
