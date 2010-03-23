//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * The ServiceInfoResolver queries up to three times consecutively for a service info, and then removes itself from the timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same info in the timer queue.
 */
public class ServiceInfoResolver extends DNSTask
{
    static Logger logger = Logger.getLogger(ServiceInfoResolver.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    int _count = 0;
    private final ServiceInfoImpl _info;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info)
    {
        super(jmDNSImpl);
        this._info = info;
        info.setDns(this._jmDNSImpl);
        this._jmDNSImpl.addListener(info, new DNSQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    public void start(Timer timer)
    {
        if (this._jmDNSImpl.getState() != DNSState.CANCELED)
        {
            timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
        }
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
                    out.addQuestion(new DNSQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    out.addQuestion(new DNSQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    if (_info.getServer() != null)
                    {
                        out.addQuestion(new DNSQuestion(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    }
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN), now);
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN), now);
                    if (_info.getServer() != null)
                    {
                        out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN), now);
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