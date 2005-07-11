/*
 * @(#)DNSListener.java  1.0  May 22, 2004
 *
 * Copyright (c) 2004 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package javax.jmdns;

// REMIND: Listener should follow Java idiom for listener or have a different
//         name.

/**
 * DNSListener.
 * Listener for record updates.
 *
 * @author Werner Randelshofer
 * @version 1.0  May 22, 2004  Created.
 */
interface DNSListener
{
    /**
     * Update a DNS record.
     */
    void updateRecord(JmDNS jmdns, long now, DNSRecord record);
}
