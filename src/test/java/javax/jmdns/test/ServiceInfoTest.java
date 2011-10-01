/**
 *
 */
package javax.jmdns.test;

import static junit.framework.Assert.assertEquals;

import java.util.Map;

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

}
