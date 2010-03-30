//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
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
 * The ServiceResolver queries three times consecutively for services of a given type, and then removes itself from the timer.
 * <p/>
 * The ServiceResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same type in the timer queue.
 */
public class ServiceResolver extends DNSTask
{
    static Logger logger = Logger.getLogger(ServiceResolver.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    int _count = 0;
    private String _type;

    public ServiceResolver(JmDNSImpl jmDNSImpl, String type)
    {
        super(jmDNSImpl);
        this._type = type;
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
                if (_count++ < 3)
                {
                    logger.finer("run() JmDNS querying service");
                    long now = System.currentTimeMillis();
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(DNSQuestion.newQuestion(_type, DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    for (Iterator<? extends ServiceInfo> s = this._jmDNSImpl.getServices().values().iterator(); s.hasNext();)
                    {
                        final ServiceInfoImpl info = (ServiceInfoImpl) s.next();
                        try
                        {
                            out.addAnswer(new DNSRecord.Pointer(info.getType(), DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, info.getQualifiedName()), now);
                        }
                        catch (IOException ee)
                        {
                            break;
                        }
                    }
                    this._jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
            else
            {
                if (this._jmDNSImpl.getState() == DNSState.CANCELED)
                {
                    this.cancel();
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