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
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends DNSTask
{
    static Logger logger = Logger.getLogger(TypeResolver.class.getName());

    /**
     * @param jmDNSImpl
     */
    public TypeResolver(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
    }

    public void start(Timer timer)
    {
        if (this._jmDNSImpl.getState() != DNSState.CANCELED)
        {
            timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
        }
    }

    /**
     * Counts the number of queries that were sent.
     */
    int _count = 0;

    @Override
    public void run()
    {
        try
        {
            if (this._jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                if (_count++ < 3)
                {
                    logger.finer("run() JmDNS querying type");
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(DNSQuestion.newQuestion("_services._mdns._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    for (String type : this._jmDNSImpl.getServiceTypes().values())
                    {
                        out.addAnswer(new DNSRecord.Pointer("_services._mdns._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, type), 0);
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