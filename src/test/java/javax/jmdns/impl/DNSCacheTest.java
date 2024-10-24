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

import javax.jmdns.impl.DNSCache;
import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSRecordClass;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DNSCacheTest {

    @Test
    void testCacheCreation() {
        DNSCache cache = new DNSCache();
        assertNotNull(cache, "Could not create a new DNS cache.");
    }

    @Test
    void testCacheAddEntry() {
        DNSCache cache = new DNSCache();

        DNSEntry entry = new DNSRecord.Service("pierre._home-sharing._tcp.local.", DNSRecordClass.CLASS_IN, false, 0, 0, 0, 0, "panoramix.local.");
        cache.addDNSEntry(entry);
        assertEquals(entry, cache.getDNSEntry(entry), "Could not retrieve the value we inserted");
    }

    @Test
    void testCacheRemoveEntry() {
        DNSCache cache = new DNSCache();

        DNSEntry entry = new DNSRecord.Service("pierre._home-sharing._tcp.local.", DNSRecordClass.CLASS_IN, false, 0, 0, 0, 0, "panoramix.local.");
        cache.addDNSEntry(entry);
        assertEquals(entry, cache.getDNSEntry(entry), "Could not retrieve the value we inserted");
        cache.removeDNSEntry(entry);
        assertNull(cache.getDNSEntry(entry), "Could not remove the value we inserted");
        assertEquals(0, cache.size());

        List<DNSEntry> values = cache.get(entry.getKey());
        assertTrue(values == null || values.isEmpty(), "Cache still has entries for the key");
        assertNull(values, "Cache contains key with no entries");
    }

}
