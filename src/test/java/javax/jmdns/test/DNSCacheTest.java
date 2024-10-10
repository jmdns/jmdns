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
/**
 *
 */
package javax.jmdns.test;

import static org.junit.Assert.*;

import javax.jmdns.impl.DNSCache;
import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSRecordClass;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 *
 */
public class DNSCacheTest {

    @Before
    public void setup() {
        //
    }

    @Test
    public void testCacheCreation() {
        DNSCache cache = new DNSCache();
        assertNotNull("Could not create a new DNS cache.", cache);
    }

    @Test
    public void testCacheAddEntry() {
        DNSCache cache = new DNSCache();

        DNSEntry entry = new DNSRecord.Service("pierre._home-sharing._tcp.local.", DNSRecordClass.CLASS_IN, false, 0, 0, 0, 0, "panoramix.local.");
        cache.addDNSEntry(entry);
        assertEquals("Could not retrieve the value we inserted", entry, cache.getDNSEntry(entry));

    }

    @Test
    public void testCacheRemoveEntry() {
        DNSCache cache = new DNSCache();

        DNSEntry entry = new DNSRecord.Service("pierre._home-sharing._tcp.local.", DNSRecordClass.CLASS_IN, false, 0, 0, 0, 0, "panoramix.local.");
        cache.addDNSEntry(entry);
        assertEquals("Could not retrieve the value we inserted", entry, cache.getDNSEntry(entry));
        cache.removeDNSEntry(entry);
        assertNull("Could not remove the value we inserted", cache.getDNSEntry(entry));
        assertEquals(0, cache.size());

        List<DNSEntry> values = cache.get(entry.getKey());
        assertTrue("Cache still has entries for the key", values == null || values.isEmpty());
        assertNull("Cache contains key with no entries", values);
    }

}
