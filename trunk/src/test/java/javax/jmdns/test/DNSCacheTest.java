/**
 *
 */
package javax.jmdns.test;

import static junit.framework.Assert.assertNotNull;

import javax.jmdns.impl.DNSCache;

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

}
