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
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends Resolver
{
    private static Logger logger = Logger.getLogger(TypeResolver.class.getName());

    /**
     * @param jmDNSImpl
     */
    public TypeResolver(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
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
        long now = System.currentTimeMillis();
        for (String type : this._jmDNSImpl.getServiceTypes().values())
        {
            try
            {
                out.addAnswer(new DNSRecord.Pointer("_services" + DNSConstants.DNS_META_QUERY + "local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, type), now);
                result = true;
            }
            catch (IOException exception)
            {
                logger.log(Level.WARNING, "addAnswers() exception ", exception);
                break;
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
        try
        {
            out.addQuestion(DNSQuestion.newQuestion("_services" + DNSConstants.DNS_META_QUERY + "local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
        }
        catch (IOException exception)
        {
            logger.log(Level.WARNING, "addQuestions() exception ", exception);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description()
    {
        return "querying type";
    }
}