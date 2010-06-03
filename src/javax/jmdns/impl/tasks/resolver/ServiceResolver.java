//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The ServiceResolver queries three times consecutively for services of a given type, and then removes itself from the timer.
 * <p/>
 * The ServiceResolver will run only if JmDNS is in state ANNOUNCED. REMIND: Prevent having multiple service resolvers for the same type in the timer queue.
 */
public class ServiceResolver extends DNSResolverTask
{

    private String _type;

    public ServiceResolver(JmDNSImpl jmDNSImpl, String type)
    {
        super(jmDNSImpl);
        this._type = type;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName()
    {
        return "ServiceResolver";
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(DNSOutgoing out) throws IOException
    {
        DNSOutgoing newOut = out;
        long now = System.currentTimeMillis();
        for (ServiceInfo info : this._jmDNSImpl.getServices().values())
        {
            newOut = this.addAnswer(newOut, new DNSRecord.Pointer(info.getType(), DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, info.getQualifiedName()), now);
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException
    {
        return this.addQuestion(out, DNSQuestion.newQuestion(_type, DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description()
    {
        return "querying service";
    }
}