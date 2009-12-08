//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSCache;
import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;

/**
 * Periodicaly removes expired entries from the cache.
 */
public class RecordReaper extends TimerTask
{
    static Logger logger = Logger.getLogger(RecordReaper.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    public RecordReaper(JmDNSImpl jmDNSImpl)
    {
        this.jmDNSImpl = jmDNSImpl;
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.RECORD_REAPER_INTERVAL, DNSConstants.RECORD_REAPER_INTERVAL);
    }

    @Override
    public void run()
    {
        synchronized (this.jmDNSImpl)
        {
            if (this.jmDNSImpl.getState() == DNSState.CANCELED)
            {
                return;
            }
            logger.finest("run() JmDNS reaping cache");

            // Remove expired answers from the cache
            // -------------------------------------
            // To prevent race conditions, we defensively copy all cache
            // entries into a list.
            List list = new ArrayList();
            synchronized (this.jmDNSImpl.getCache())
            {
                for (Iterator i = this.jmDNSImpl.getCache().iterator(); i.hasNext();)
                {
                    for (DNSCache.CacheNode n = (DNSCache.CacheNode) i.next(); n != null; n = n.next())
                    {
                        list.add(n.getValue());
                    }
                }
            }
            // Now, we remove them.
            long now = System.currentTimeMillis();
            for (Iterator i = list.iterator(); i.hasNext();)
            {
                DNSRecord c = (DNSRecord) i.next();
                if (c.isExpired(now))
                {
                    this.jmDNSImpl.updateRecord(now, c);
                    this.jmDNSImpl.getCache().remove(c);
                }
            }
        }
    }
}