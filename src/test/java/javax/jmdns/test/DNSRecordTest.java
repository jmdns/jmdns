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

import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSRecordClass;
import org.junit.Assert;
import org.junit.Test;

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

}
