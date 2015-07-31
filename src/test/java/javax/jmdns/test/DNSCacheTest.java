/**
 *
 */
package javax.jmdns.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import javax.jmdns.impl.DNSCache;
import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSRecordClass;

import org.junit.Before;
import org.junit.Test;

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

    }

}
