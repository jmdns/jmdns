//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Timer;
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
 * The ServiceInfoResolver queries up to three times consecutively for a service info, and then removes itself from the
 * timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service
 * resolvers for the same info in the timer queue.
 */
public class ServiceInfoResolver extends DNSTask
{
    static Logger logger = Logger.getLogger(ServiceInfoResolver.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    int _count = 0;
    private ServiceInfoImpl _info;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info)
    {
        super(jmDNSImpl);
        this._info = info;
        info.setDns(this._jmDNSImpl);
        this._jmDNSImpl.addListener(info, new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_ANY,
                DNSConstants.CLASS_IN));
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
            if (this._jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                if (_count++ < 3 && !_info.hasData())
                {
                    long now = System.currentTimeMillis();
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(new DNSQuestion(_info.getQualifiedName(), DNSConstants.TYPE_SRV,
                            DNSConstants.CLASS_IN));
                    out.addQuestion(new DNSQuestion(_info.getQualifiedName(), DNSConstants.TYPE_TXT,
                            DNSConstants.CLASS_IN));
                    if (_info.getServer() != null)
                    {
                        out.addQuestion(new DNSQuestion(_info.getServer(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN));
                    }
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().get(_info.getQualifiedName(),
                            DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN), now);
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().get(_info.getQualifiedName(),
                            DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN), now);
                    if (_info.getServer() != null)
                    {
                        out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().get(_info.getServer(),
                                DNSConstants.TYPE_A, DNSConstants.CLASS_IN), now);
                    }
                    this._jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                    this._jmDNSImpl.removeListener(_info);
                }
            }
            else
            {
                if (this._jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    this.cancel();
                    this._jmDNSImpl.removeListener(_info);
                }
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this._jmDNSImpl.recover();
        }
    }
}