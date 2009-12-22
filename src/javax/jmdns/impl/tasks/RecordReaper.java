//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Collection;
import java.util.Timer;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;

/**
 * Periodically removes expired entries from the cache.
 */
public class RecordReaper extends DNSTask
{
    static Logger logger = Logger.getLogger(RecordReaper.class.getName());

    /**
     * @param jmDNSImpl
     */
    public RecordReaper(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.RECORD_REAPER_INTERVAL, DNSConstants.RECORD_REAPER_INTERVAL);
    }

    @Override
    public void run()
    {
        synchronized (this._jmDNSImpl)
        {
            if (this._jmDNSImpl.getState() == DNSState.CANCELED)
            {
                return;
            }
            logger.finest("run() JmDNS reaping cache");

            // Remove expired answers from the cache
            // -------------------------------------
            // To prevent race conditions, we defensively copy all cache
            // entries into a list.
            Collection<? extends DNSEntry> dnsEntryLits = this._jmDNSImpl.getCache().allValues();
            // Now, we remove them.
            long now = System.currentTimeMillis();
            for (DNSEntry entry : dnsEntryLits)
            {
                DNSRecord record = (DNSRecord) entry;
                if (record.isExpired(now))
                {
                    this._jmDNSImpl.updateRecord(now, record);
                    this._jmDNSImpl.getCache().removeDNSEntry(record);
                }
            }
        }
    }
}