//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.JmDNSImpl.Operation;
import javax.jmdns.impl.constants.DNSConstants;

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

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName()
    {
        return "RecordReaper(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer)
    {
        if (!this.getDns().isCanceling() && !this.getDns().isCanceled())
        {
            timer.schedule(this, DNSConstants.RECORD_REAPER_INTERVAL, DNSConstants.RECORD_REAPER_INTERVAL);
        }
    }

    @Override
    public void run()
    {
        if (this.getDns().isCanceling() || this.getDns().isCanceled())
        {
            return;
        }
        if (logger.isLoggable(Level.FINEST))
        {
            logger.finest(this.getName() + ".run() JmDNS reaping cache");
        }

        // Remove expired answers from the cache
        // -------------------------------------
        long now = System.currentTimeMillis();
        for (DNSEntry entry : this.getDns().getCache().allValues())
        {
            try
            {
                DNSRecord record = (DNSRecord) entry;
                if (record.isStale(now))
                {
                    // we should query for the record we care about i.e. those in the service collectors
                    this.getDns().renewServiceCollector(record);
                }
                if (record.isExpired(now))
                {
                    this.getDns().updateRecord(now, record, Operation.Remove);
                    this.getDns().getCache().removeDNSEntry(record);
                }
            }
            catch (Exception exception)
            {
                logger.log(Level.SEVERE, this.getName() + ".Error while reaping records: " + entry, exception);
                logger.severe(this.getDns().toString());
            }
        }
    }
}