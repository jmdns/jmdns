package javax.jmdns.impl;

import org.junit.Test;

import javax.jmdns.ServiceInfo;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ServiceTypeDecoderTest {

    @Test
    public void decodeItxptStyle() {
        assertDecodeProperly("aabbcc11eeff._apc._itxpt_http._tcp.local.", "aabbcc11eeff._apc", "itxpt_http", "tcp", "local", "");
    }

    @Test
    public void decodeWithoutDot() {
        assertDecodeProperly("any_instance_name._http._tcp.local.", "any_instance_name", "http", "tcp", "local", "");
    }

    @Test
    public void decodeWithSubtype() {
        assertDecodeProperly("_printer._sub._http._tcp.local.", "", "http", "tcp", "local", "printer");
        assertDecodeProperly("4c33057a._sub._apple-mobdev2._tcp.local.", "", "apple-mobdev2", "tcp", "local", "4c33057a");
        assertDecodeProperly("abb22cc._sub._apple-mobdev2._tcp.local.", "", "apple-mobdev2", "tcp", "local", "abb22cc");
    }

    @Test
    public void decodeWithSubtype2() {
        assertDecodeProperly("abcde._printer._sub._http._tcp.local.", "abcde", "http", "tcp", "local", "printer");
    }

    @Test
    public void decode() {
        Map<ServiceInfo.Fields, String> actual = ServiceTypeDecoder.decodeQualifiedNameMapForType("DIST123_7-F07_OC030_05_03941.local.");
        Map<ServiceInfo.Fields, String> expected = ServiceInfoImpl.createQualifiedMap("DIST123_7-F07_OC030_05_03941", "", "", "local", "");
        assertEquals(expected, actual);
    }

    @Test
    public void decode2() {
        assertDecodeProperly("DeviceManagementService._ibisip_http._tcp.local.", "DeviceManagementService", "ibisip_http", "tcp", "local", "");
    }

    @Test
    public void decode3() {
        assertDecodeProperly("_ibisip_http._tcp.local.", "", "ibisip_http", "tcp", "local", "");
    }

    @Test
    public void decode4() {
        assertDecodeProperly("_itxpt_http._tcp.local", "", "itxpt_http", "tcp", "local", "");
        assertDecodeProperly("_itxpt_http._tcp.local.", "", "itxpt_http", "tcp", "local", "");
        assertDecodeProperly("ABC-PC2-berlin-company-com.local.", "ABC-PC2-berlin-company-com", "", "", "local", "");
        assertDecodeProperly("abc123.local.", "abc123", "", "", "local", "");
        assertDecodeProperly("abc123-2.local.", "abc123-2", "", "", "local", "");
        assertDecodeProperly("23.7.16.172.in-addr.arpa.", "23.7.16.172", "", "", "in-addr.arpa", "");
        assertDecodeProperly("0.0.5.6.0.0.e.f.f.f.5.3.a.0.2.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.e.f.ip6.arpa.", "0.0.5.6.0.0.e.f.f.f.5.3.a.0.2.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.e.f", "", "", "ip6.arpa", "");
    }

    @Test
    public void decode5() {
        assertDecodeProperly("abc123-0000-00000-3.local.", "abc123-0000-00000-3", "", "", "local", "");
        assertDecodeProperly("HP LaserJet 10000 colorMFP M570dw (A11111F)._ipps._tcp.local.", "HP LaserJet 10000 colorMFP M570dw (A11111F)", "ipps", "tcp", "local", "");
        assertDecodeProperly("HP LaserJet 700 color MFP M775 [520D0D]._privet._tcp.local.", "HP LaserJet 700 color MFP M775 [520D0D]", "privet", "tcp", "local", "");
        assertDecodeProperly("abc123-0000-00000-3._sftp-ssh._tcp.local.", "abc123-0000-00000-3", "sftp-ssh", "tcp", "local", "");
        assertDecodeProperly("AXAA 123 ABC - 001122334455._http._tcp.local.", "AXAA 123 ABC - 001122334455", "http", "tcp", "local", "");
        assertDecodeProperly("AbcDef Test 123 ABC (DEMO) - 001122334455._http._tcp.local.", "AbcDef Test 123 ABC (DEMO) - 001122334455", "http", "tcp", "local", "");
        assertDecodeProperly("10-20-30-40.1 xxx Time Capsule 01._sleep-proxy._udp.local.", "10-20-30-40.1 xxx Time Capsule 01", "sleep-proxy", "udp", "local", "");
        assertDecodeProperly("Jenkins (3)._http._tcp.local.", "Jenkins (3)", "http", "tcp", "local", "");
    }

    @Test
    public void testDecodeQualifiedNameMap() {
        String domain = "test.com";
        String protocol = "udp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "printer";

        String type = "_" + application + "._" + protocol + "." + domain + ".";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMap(type, name, subtype);

        assertEquals("We did not get the right domain:", domain, map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", protocol, map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", application, map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", name, map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", subtype, map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeQualifiedNameMapDefaults() {
        String domain = "local";
        String protocol = "tcp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMap(application, name, subtype);

        assertEquals("We did not get the right domain:", domain, map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", protocol, map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", application, map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", name, map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", subtype, map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeServiceType() {
        String type = "_home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));

    }

    @Test
    public void testDecodeServiceWithUnderscoreType() {
        String type = "_x_lumenera_mjpeg1._udp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "udp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "x_lumenera_mjpeg1", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));

    }

    @Test
    public void testDecodeServiceTCPType() {
        String type = "_afpovertcp._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "afpovertcp", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeServiceTypeWithSubType() {
        String type = "_00000000-0b44-f234-48c8-071c565644b3._sub._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "00000000-0b44-f234-48c8-071c565644b3", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeServiceName() {
        String type = "My New Itunes Service._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "My New Itunes Service", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeServiceNameWithSpecialCharacter() {
        String type = "&test._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "home-sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "&test", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeDNSMetaQuery() {
        String type = "_services._dns-sd._udp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "udp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "dns-sd", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "_services", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testReverseDNSQuery() {
        String type = "100.50.168.192.in-addr.arpa.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "in-addr.arpa", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "100.50.168.192", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testAddress() {
        String type = "panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "panoramix", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testAddressPreserveCase() {
        String type = "pano_RAmix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "pano_RAmix", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testNameWithUnderscore() {
        String type = "pano_ramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "pano_ramix", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testNameWithSpecialChar() {
        String type = "panoramİx.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "panoramİx", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testCasePreserving() {
        String type = "My New Itunes Service._Home-Sharing._TCP.Panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "Panoramix.local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "TCP", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "Home-Sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "My New Itunes Service", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testCasePreservingSpecialChar() {
        String type = "aBcİ._Home-Sharing._TCP.Panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "Panoramix.local", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "TCP", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "Home-Sharing", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "aBcİ", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));
    }

    @Test
    public void testDecodeServiceTypeMissingDomain() {
        String type = "myservice._ftp._tcp.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("We did not get the right domain:", "", map.get(ServiceInfo.Fields.Domain));
        assertEquals("We did not get the right protocol:", "tcp", map.get(ServiceInfo.Fields.Protocol));
        assertEquals("We did not get the right application:", "ftp", map.get(ServiceInfo.Fields.Application));
        assertEquals("We did not get the right name:", "myservice", map.get(ServiceInfo.Fields.Instance));
        assertEquals("We did not get the right subtype:", "", map.get(ServiceInfo.Fields.Subtype));

    }

    private void assertDecodeProperly(String type, String... qualifiedMap) {
        Map<ServiceInfo.Fields, String> actual = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);
        Map<ServiceInfo.Fields, String> expected = ServiceInfoImpl.createQualifiedMap(qualifiedMap[0], qualifiedMap[1], qualifiedMap[2], qualifiedMap[3], qualifiedMap[4]);
        assertEquals(expected, actual);
    }

}
