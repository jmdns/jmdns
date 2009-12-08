//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

// REMIND: Listener should follow Java idiom for listener or have a different
//         name.

/**
 * DNSListener. Listener for record updates.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version 1.0 May 22, 2004 Created.
 */
interface DNSListener
{
    /**
     * Update a DNS record.
     *
     * @param jmdns
     *            jmDNS
     * @param now
     *            update date
     * @param record
     *            DNS record
     */
    void updateRecord(JmDNSImpl jmdns, long now, DNSRecord record);
}
