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
import javax.jmdns.ServiceInfo.Fields;
import javax.jmdns.impl.ServiceInfoImpl;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ServiceInfoTest {

    @Before
    public void setup() {
        // Placeholder
    }

    @Test
    public void testDecodeQualifiedNameMap() {
        String domain = "test.com";
        String protocol = "udp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "printer";

        String type = "_" + application + "._" + protocol + "." + domain + ".";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMap(type, name, subtype);

        assertEquals("We did not get the right domain:", domain, map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", protocol, map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", application, map.get(Fields.Application));
        assertEquals("We did not get the right name:", name, map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", subtype, map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeQualifiedNameMapDefaults() {
        String domain = "local";
        String protocol = "tcp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMap(application, name, subtype);

        assertEquals("We did not get the right domain:", domain, map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", protocol, map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", application, map.get(Fields.Application));
        assertEquals("We did not get the right name:", name, map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", subtype, map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeServiceType() {
        String type = "_home-sharing._tcp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));

    }

    @Test
    public void testDecodeServiceWithUnderscoreType() {
        String type = "_x_lumenera_mjpeg1._udp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "udp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "x_lumenera_mjpeg1", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));

    }

    @Test
    public void testDecodeServiceTCPType() {
        String type = "_afpovertcp._tcp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "afpovertcp", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeServiceTypeWithSubType() {
        String type = "_00000000-0b44-f234-48c8-071c565644b3._sub._home-sharing._tcp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "00000000-0b44-f234-48c8-071c565644b3", map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeServiceName() {
        String type = "My New Itunes Service._home-sharing._tcp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "My New Itunes Service", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeServiceNameWithSpecialCharacter() {
        String type = "&test._home-sharing._tcp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "&test", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeDNSMetaQuery() {
        String type = "_services._dns-sd._udp.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "udp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "dns-sd", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "_services", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testReverseDNSQuery() {
        String type = "100.50.168.192.in-addr.arpa.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "in-addr.arpa", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "100.50.168.192", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testAddress() {
        String type = "panoramix.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "panoramix", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testCasePreserving() {
        String type = "My New Itunes Service._Home-Sharing._TCP.Panoramix.local.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "Panoramix.local", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "TCP", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "Home-Sharing", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "My New Itunes Service", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));
    }

    @Test
    public void testDecodeServiceTypeMissingDomain() {
        String type = "myservice._ftp._tcp.";

        Map<Fields, String> map = ServiceInfoImpl.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "", map.get(Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(Fields.Protocol));
        assertEquals("We did not get the right application:", "ftp", map.get(Fields.Application));
        assertEquals("We did not get the right name:", "myservice", map.get(Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(Fields.Subtype));

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
