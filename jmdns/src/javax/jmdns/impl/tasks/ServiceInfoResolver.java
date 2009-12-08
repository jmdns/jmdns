//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

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
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The ServiceInfoResolver queries up to three times consecutively for
 * a service info, and then removes itself from the timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED.
 * REMIND: Prevent having multiple service resolvers for the same info in the
 * timer queue.
 */
public class ServiceInfoResolver extends TimerTask
{
    static Logger logger = Logger.getLogger(ServiceInfoResolver.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    /**
     * Counts the number of queries being sent.
     */
    int count = 0;
    private ServiceInfoImpl info;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.info = info;
        info.setDns(this.jmDNSImpl);
        this.jmDNSImpl.addListener(info, new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
    }

    @Override
    public void run()
    {
        try
        {
            if (this.jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                if (count++ < 3 && !info.hasData())
                {
                    long now = System.currentTimeMillis();
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN));
                    out.addQuestion(new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN));
                    if (info.getServer() != null)
                    {
                        out.addQuestion(new DNSQuestion(info.getServer(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN));
                    }
                    out.addAnswer((DNSRecord) this.jmDNSImpl.getCache().get(info.getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN), now);
                    out.addAnswer((DNSRecord) this.jmDNSImpl.getCache().get(info.getQualifiedName(), DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN), now);
                    if (info.getServer() != null)
                    {
                        out.addAnswer((DNSRecord) this.jmDNSImpl.getCache().get(info.getServer(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN), now);
                    }
                    this.jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                    this.jmDNSImpl.removeListener(info);
                }
            }
            else
            {
                if (this.jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    this.cancel();
                    this.jmDNSImpl.removeListener(info);
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