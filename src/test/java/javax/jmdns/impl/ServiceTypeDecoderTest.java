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

import javax.jmdns.ServiceInfo;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceTypeDecoderTest {

    @Test
    void decodeItxptStyle() {
        assertDecodeProperly("aabbcc11eeff._apc._itxpt_http._tcp.local.", "aabbcc11eeff._apc", "itxpt_http", "tcp", "local", "");
    }

    @Test
    void decodeWithoutDot() {
        assertDecodeProperly("any_instance_name._http._tcp.local.", "any_instance_name", "http", "tcp", "local", "");
    }

    @Test
    void decodeWithSubtype() {
        assertDecodeProperly("_printer._sub._http._tcp.local.", "", "http", "tcp", "local", "printer");
        assertDecodeProperly("4c33057a._sub._apple-mobdev2._tcp.local.", "", "apple-mobdev2", "tcp", "local", "4c33057a");
        assertDecodeProperly("abb22cc._sub._apple-mobdev2._tcp.local.", "", "apple-mobdev2", "tcp", "local", "abb22cc");
    }

    @Test
    void decodeWithSubtype2() {
        assertDecodeProperly("abcde._printer._sub._http._tcp.local.", "abcde", "http", "tcp", "local", "printer");
    }

    @Test
    void decode() {
        Map<ServiceInfo.Fields, String> actual = ServiceTypeDecoder.decodeQualifiedNameMapForType("DIST123_7-F07_OC030_05_03941.local.");
        Map<ServiceInfo.Fields, String> expected = ServiceInfoImpl.createQualifiedMap("DIST123_7-F07_OC030_05_03941", "", "", "local", "");
        assertEquals(expected, actual);
    }

    @Test
    void decode2() {
        assertDecodeProperly("DeviceManagementService._ibisip_http._tcp.local.", "DeviceManagementService", "ibisip_http", "tcp", "local", "");
    }

    @Test
    void decode3() {
        assertDecodeProperly("_ibisip_http._tcp.local.", "", "ibisip_http", "tcp", "local", "");
    }

    @Test
    void decode4() {
        assertDecodeProperly("_itxpt_http._tcp.local", "", "itxpt_http", "tcp", "local", "");
        assertDecodeProperly("_itxpt_http._tcp.local.", "", "itxpt_http", "tcp", "local", "");
        assertDecodeProperly("ABC-PC2-berlin-company-com.local.", "ABC-PC2-berlin-company-com", "", "", "local", "");
        assertDecodeProperly("abc123.local.", "abc123", "", "", "local", "");
        assertDecodeProperly("abc123-2.local.", "abc123-2", "", "", "local", "");
        assertDecodeProperly("23.7.16.172.in-addr.arpa.", "23.7.16.172", "", "", "in-addr.arpa", "");
        assertDecodeProperly("0.0.5.6.0.0.e.f.f.f.5.3.a.0.2.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.e.f.ip6.arpa.", "0.0.5.6.0.0.e.f.f.f.5.3.a.0.2.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.e.f", "", "", "ip6.arpa", "");
    }

    @Test
    void decode5() {
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
    void testDecodeQualifiedNameMap() {
        String domain = "test.com";
        String protocol = "udp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "printer";

        String type = "_" + application + "._" + protocol + "." + domain + ".";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMap(type, name, subtype);

        assertEquals(domain, map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals(protocol, map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals(application, map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals(name, map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals(subtype, map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeQualifiedNameMapDefaults() {
        String domain = "local";
        String protocol = "tcp";
        String application = "ftp";
        String name = "My Service";
        String subtype = "";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMap(application, name, subtype);

        assertEquals(domain, map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals(protocol, map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals(application, map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals(name, map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals(subtype, map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceType() {
        String type = "_home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("home-sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceWithUnderscoreType() {
        String type = "_x_lumenera_mjpeg1._udp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("udp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("x_lumenera_mjpeg1", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceTCPType() {
        String type = "_afpovertcp._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("afpovertcp", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceTypeWithSubType() {
        String type = "_00000000-0b44-f234-48c8-071c565644b3._sub._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("home-sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("00000000-0b44-f234-48c8-071c565644b3", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceName() {
        String type = "My New Itunes Service._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("home-sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("My New Itunes Service", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceNameWithSpecialCharacter() {
        String type = "&test._home-sharing._tcp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("home-sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("&test", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeDNSMetaQuery() {
        String type = "_services._dns-sd._udp.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("udp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("dns-sd", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("_services", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testReverseDNSQuery() {
        String type = "100.50.168.192.in-addr.arpa.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("in-addr.arpa", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("100.50.168.192", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testAddress() {
        String type = "panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("panoramix", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testAddressPreserveCase() {
        String type = "pano_RAmix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("pano_RAmix", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testNameWithUnderscore() {
        String type = "pano_ramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("pano_ramix", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testNameWithSpecialChar() {
        String type = "panoramİx.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("panoramİx", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testCasePreserving() {
        String type = "My New Itunes Service._Home-Sharing._TCP.Panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("Panoramix.local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("TCP", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("Home-Sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("My New Itunes Service", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testCasePreservingSpecialChar() {
        String type = "aBcİ._Home-Sharing._TCP.Panoramix.local.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("Panoramix.local", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("TCP", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("Home-Sharing", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("aBcİ", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    @Test
    void testDecodeServiceTypeMissingDomain() {
        String type = "myservice._ftp._tcp.";

        Map<ServiceInfo.Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);

        assertEquals("", map.get(ServiceInfo.Fields.Domain), "We did not get the right domain:");
        assertEquals("tcp", map.get(ServiceInfo.Fields.Protocol), "We did not get the right protocol:");
        assertEquals("ftp", map.get(ServiceInfo.Fields.Application), "We did not get the right application:");
        assertEquals("myservice", map.get(ServiceInfo.Fields.Instance), "We did not get the right name:");
        assertEquals("", map.get(ServiceInfo.Fields.Subtype), "We did not get the right subtype:");
    }

    private void assertDecodeProperly(String type, String... qualifiedMap) {
        Map<ServiceInfo.Fields, String> actual = ServiceTypeDecoder.decodeQualifiedNameMapForType(type);
        Map<ServiceInfo.Fields, String> expected = ServiceInfoImpl.createQualifiedMap(qualifiedMap[0], qualifiedMap[1], qualifiedMap[2], qualifiedMap[3], qualifiedMap[4]);
        assertEquals(expected, actual);
    }

}
