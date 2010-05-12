//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The ServiceInfoResolver queries up to three times consecutively for a service info, and then removes itself from the timer.
 * <p/>
 * The ServiceInfoResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same info in the timer queue.
 */
public class ServiceInfoResolver extends Resolver
{
    private static Logger logger = Logger.getLogger(ServiceInfoResolver.class.getName());

    private final ServiceInfoImpl _info;

    public ServiceInfoResolver(JmDNSImpl jmDNSImpl, ServiceInfoImpl info)
    {
        super(jmDNSImpl);
        this._info = info;
        info.setDns(this._jmDNSImpl);
        this._jmDNSImpl.addListener(info, DNSQuestion.newQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.TimerTask#cancel()
     */
    @Override
    public boolean cancel()
    {
        // We should not forget to remove the listener
        boolean result = super.cancel();
        if (!_info.isPersistent())
        {
            this._jmDNSImpl.removeListener(_info);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected boolean addAnswers(DNSOutgoing out)
    {
        boolean result = false;
        if (!_info.hasData())
        {
            long now = System.currentTimeMillis();
            try
            {
                out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN), now);
                out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN), now);
                result = true;
                if (_info.getServer() != null)
                {
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN), now);
                    out.addAnswer((DNSRecord) this._jmDNSImpl.getCache().getDNSEntry(_info.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN), now);
                }
            }
            catch (IOException exception)
            {
                logger.log(Level.WARNING, "addAnswers() exception ", exception);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected boolean addQuestions(DNSOutgoing out)
    {
        boolean result = false;
        if (!_info.hasData())
        {
            try
            {
                out.addQuestion(DNSQuestion.newQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                out.addQuestion(DNSQuestion.newQuestion(_info.getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                result = true;
                if (_info.getServer() != null)
                {
                    out.addQuestion(DNSQuestion.newQuestion(_info.getServer(), DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    out.addQuestion(DNSQuestion.newQuestion(_info.getServer(), DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                }
            }
            catch (IOException exception)
            {
                logger.log(Level.WARNING, "addQuestions() exception ", exception);
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description()
    {
        return "querying service info";
    }

}