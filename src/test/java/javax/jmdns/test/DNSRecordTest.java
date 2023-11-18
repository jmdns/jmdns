/*
 * Copyright 2015 JmDNS.
 *
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
package javax.jmdns.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import static org.easymock.EasyMock.expect;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DNSIncoming.class, DNSIncoming.MessageInputStream.class})
public class DNSRecordTest {

    private static final int TTL_IN_SECONDS = 60 * 60; // ONE HOUR
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;
    private static final long PERCENT_STALE_79 = ONE_HOUR_IN_MILLIS * 79 / 100;
    private static final long PERCENT_STALE_84 = ONE_HOUR_IN_MILLIS * 84 / 100;
    private static final long PERCENT_STALE_89 = ONE_HOUR_IN_MILLIS * 89 / 100;
    private static final long PERCENT_STALE_94 = ONE_HOUR_IN_MILLIS * 94 / 100;
    private static final long PERCENT_STALE_99 = ONE_HOUR_IN_MILLIS * 99 / 100;

    @Test
    public void testStaleDNSRecord() {

        DNSRecord record = new DNSRecord.Service("test", DNSRecordClass.CLASS_IN, true, TTL_IN_SECONDS, 0, 0, 0, "test");
        long now = System.currentTimeMillis();

        // stale threshold is 80% + random offset
        Assert.assertFalse("Record should not be stale after creation", record.isStaleAndShouldBeRefreshed(now));
        Assert.assertFalse("79% should not be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_79));
        Assert.assertTrue("84% should be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_84));

        // stale threshold is 85% + random offset
        record.incrementRefreshPercentage();
        Assert.assertFalse("84% should not be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_84));
        Assert.assertTrue("89% should be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_89));

        // stale threshold is 90% + random offset
        record.incrementRefreshPercentage();
        Assert.assertFalse("89% should not be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_89));
        Assert.assertTrue("94% should be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_94));

        // stale threshold is 95% + random offset
        record.incrementRefreshPercentage();
        Assert.assertFalse("94% should not be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_94));
        Assert.assertTrue("99% should be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_99));

        // stale threshold is 100%
        record.incrementRefreshPercentage();
        Assert.assertFalse("99% should not be stale", record.isStaleAndShouldBeRefreshed(now + PERCENT_STALE_99));
        Assert.assertTrue("100% should be stale", record.isStaleAndShouldBeRefreshed(now + ONE_HOUR_IN_MILLIS));

    }

    @Test
    public void testIPv4MappedIPv6Addresses() throws Exception {

        DNSIncoming.MessageInputStream stream = PowerMock.createNiceMock(DNSIncoming.MessageInputStream.class);
        expect(stream.readName()).andReturn("test");
        expect(stream.readUnsignedShort()).andReturn(DNSRecordType.TYPE_AAAA.indexValue());
        expect(stream.readUnsignedShort()).andReturn(DNSRecordClass.CLASS_IN.indexValue());
        expect(stream.readInt()).andReturn(3600);
        expect(stream.readUnsignedShort()).andReturn(16);
        expect(stream.readBytes(16)).andReturn(new byte[]
                        { 0, 0, 0, 0,
                          0, 0, 0, 0,
                          0, 0, (byte) 0xff, (byte) 0xff,
                        127, 0, 0, 1});

        DNSIncoming dnsIncoming = Whitebox.newInstance(DNSIncoming.class);
        Whitebox.setInternalState(dnsIncoming, "_messageInputStream", stream);
        PowerMock.replayAll();

        DNSRecord record = Whitebox.invokeMethod(dnsIncoming, "readAnswer", null);
        Assert.assertNull(record);
        PowerMock.verifyAll();

    }

    @Test
    public void testServiceInfoFromDNSRecord() {

        DNSRecord record = new DNSRecord.Service("test._http._tcp.local.", DNSRecordClass.CLASS_IN, true, TTL_IN_SECONDS, 0, 0, 0, "test_server");

        ServiceInfo serviceInfo = record.getServiceInfo(true);

        Assert.assertEquals("test", serviceInfo.getName());
        Assert.assertEquals("http", serviceInfo.getApplication());
        Assert.assertEquals("tcp", serviceInfo.getProtocol());
        Assert.assertEquals("local", serviceInfo.getDomain());
        Assert.assertEquals("test_server", serviceInfo.getServer());
    }
}
