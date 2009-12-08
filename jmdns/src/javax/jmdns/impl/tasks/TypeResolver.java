//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then
 * removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends TimerTask
{
    static Logger logger = Logger.getLogger(TypeResolver.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    public TypeResolver(JmDNSImpl jmDNSImpl)
    {
        this.jmDNSImpl = jmDNSImpl;
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
    }

    /**
     * Counts the number of queries that were sent.
     */
    int count = 0;

    @Override
    public void run()
    {
        try
        {
            if (this.jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                if (count++ < 3)
                {
                    logger.finer("run() JmDNS querying type");
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(new DNSQuestion("_services._mdns._udp.local.", DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN));
                    for (Iterator iterator = this.jmDNSImpl.getServiceTypes().values().iterator(); iterator.hasNext();)
                    {
                        out.addAnswer(new DNSRecord.Pointer("_services._mdns._udp.local.", DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, (String) iterator.next()), 0);
                    }
                    this.jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
            else
            {
                if (this.jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    this.cancel();
                }
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.jmDNSImpl.recover();
        }
    }
}