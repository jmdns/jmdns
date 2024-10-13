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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.test.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

class DNSRecordTest {

    private static final int TTL_IN_SECONDS = 60 * 60; // ONE HOUR
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final long PERCENT_STALE_79 = ONE_HOUR_IN_MILLIS * 79 / 100;
    private static final long PERCENT_STALE_84 = ONE_HOUR_IN_MILLIS * 84 / 100;
    private static final long PERCENT_STALE_89 = ONE_HOUR_IN_MILLIS * 89 / 100;
    private static final long PERCENT_STALE_94 = ONE_HOUR_IN_MILLIS * 94 / 100;
    private static final long PERCENT_STALE_99 = ONE_HOUR_IN_MILLIS * 99 / 100;

    @BeforeEach
    public void setUp() {
        Mockito.reset();
    }

    @Test
    void testStaleDNSRecord() {
        DNSRecord dnsRecord = createServiceRecord("test", "test_server");
        long now = System.currentTimeMillis();

        // stale threshold is 80% + random offset
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now), "Record should not be stale after creation");
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_79), "79% should not be stale");
        assertTrue(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_84), "84% should be stale");

        // stale threshold is 85% + random offset
        dnsRecord.incrementRefreshPercentage();
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_84), "84% should not be stale");
        assertTrue(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_89), "89% should be stale");

        // stale threshold is 90% + random offset
        dnsRecord.incrementRefreshPercentage();
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_89), "89% should not be stale");
        assertTrue(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_94), "94% should be stale");

        // stale threshold is 95% + random offset
        dnsRecord.incrementRefreshPercentage();
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_94), "94% should not be stale");
        assertTrue(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_99), "99% should be stale");

        // stale threshold is 100%
        dnsRecord.incrementRefreshPercentage();
        assertFalse(dnsRecord.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_99), "99% should not be stale");
        assertTrue(dnsRecord.isStaleAndShouldBeRefreshed(now + ONE_HOUR_IN_MILLIS), "100% should be stale");
    }

    @Test
    void testIPv4MappedIPv6Addresses() throws Exception {
        // Mock DNSIncoming.MessageInputStream
        DNSIncoming.MessageInputStream stream = mock(DNSIncoming.MessageInputStream.class);
        when(stream.readName()).thenReturn("test");
        when(stream.readUnsignedShort()).thenReturn(DNSRecordType.TYPE_AAAA.indexValue());
        when(stream.readUnsignedShort()).thenReturn(DNSRecordClass.CLASS_IN.indexValue());
        when(stream.readInt()).thenReturn(3600);
        when(stream.readUnsignedShort()).thenReturn(16);
        when(stream.readBytes(16)).thenReturn(new byte[]
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 127, 0, 0, 1});

        // Mock DNSIncoming and set the stream internally
        DNSIncoming dnsIncoming = mock(DNSIncoming.class);
        ReflectionUtils.setInternalState(dnsIncoming, "_messageInputStream", stream);

        // Invoke the method
        DNSRecord dnsRecord = doCallRealMethod().when(dnsIncoming).readAnswer();
        assertNull(dnsRecord);
    }



    @Test
    void testServiceInfoFromDNSRecord() {
        DNSRecord dnsRecord = createServiceRecord("test._http._tcp.local.", "test_server");

        ServiceInfo serviceInfo = dnsRecord.getServiceInfo(true);

        assertEquals("test", serviceInfo.getName());
        assertEquals("http", serviceInfo.getApplication());
        assertEquals("tcp", serviceInfo.getProtocol());
        assertEquals("local", serviceInfo.getDomain());
        assertEquals("test_server", serviceInfo.getServer());
    }

    @Test
    void testDNSRecordKey() {
        // test starting with underscore seen on some Android devices
        DNSRecord record1 = createServiceRecord("_android_123.local.", "test_server");
        assertEquals("_android_123.local.", record1.getKey());

        DNSRecord record2 = createServiceRecord("test._http._tcp.local.", "test_server");
        assertEquals("test._http._tcp.local.", record2.getKey());

        DNSRecord record3 = createServiceRecord("TEST._http._tcp.local.", "test_server");
        assertEquals("test._http._tcp.local.", record3.getKey());
    }

    @Test
    void testHostInformationEquals() {
        DNSRecord.HostInformation h1 = new DNSRecord.HostInformation(null, DNSRecordClass.CLASS_IN, true, 0, null, null);
        DNSRecord.HostInformation h2 = new DNSRecord.HostInformation(null, DNSRecordClass.CLASS_IN, true, 0, null, null);
        assertEquals(h1, h2);
    }

    private static DNSRecord.Service createServiceRecord(String name, String server) {
        return new DNSRecord.Service(name, DNSRecordClass.CLASS_IN, true, TTL_IN_SECONDS, 0, 0, 0, server);
    }
}