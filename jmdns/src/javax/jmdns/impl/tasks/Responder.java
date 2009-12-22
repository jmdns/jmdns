//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSIncoming;
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
 * The Responder sends a single answer for the specified service infos and for the host name.
 */
public class Responder extends DNSTask
{
    static Logger logger = Logger.getLogger(Responder.class.getName());

    /**
     *
     */
    private DNSIncoming _in;
    private InetAddress _addr;
    private int _port;

    public Responder(JmDNSImpl jmDNSImpl, DNSIncoming in, InetAddress addr, int port)
    {
        super(jmDNSImpl);
        this._in = in;
        this._addr = addr;
        this._port = port;
    }

    public void start()
    {
        // According to draft-cheshire-dnsext-multicastdns.txt
        // chapter "8 Responding":
        // We respond immediately if we know for sure, that we are
        // the only one who can respond to the query.
        // In all other cases, we respond within 20-120 ms.
        //
        // According to draft-cheshire-dnsext-multicastdns.txt
        // chapter "7.2 Multi-Packet Known Answer Suppression":
        // We respond after 20-120 ms if the query is truncated.

        boolean iAmTheOnlyOne = true;
        for (DNSQuestion question : _in.getQuestions())
        {
            logger.finest("start() question=" + question);
            iAmTheOnlyOne &= DNSRecordType.TYPE_SRV.equals(question.getRecordType())
                    || DNSRecordType.TYPE_TXT.equals(question.getRecordType())
                    || DNSRecordType.TYPE_A.equals(question.getRecordType())
                    || DNSRecordType.TYPE_AAAA.equals(question.getRecordType())
                    || this._jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(question.getName())
                    || this._jmDNSImpl.getServices().containsKey(question.getName().toLowerCase());
            if (!iAmTheOnlyOne)
            {
                break;
            }
        }
        int delay = (iAmTheOnlyOne && !_in.isTruncated()) ? 0 : DNSConstants.RESPONSE_MIN_WAIT_INTERVAL
                + JmDNSImpl.getRandom().nextInt(
                        DNSConstants.RESPONSE_MAX_WAIT_INTERVAL - DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1)
                - _in.elapseSinceArrival();
        if (delay < 0)
        {
            delay = 0;
        }
        logger.finest("start() Responder chosen delay=" + delay);
        this._jmDNSImpl.schedule(this, delay);
    }

    @Override
    public void run()
    {
        synchronized (this._jmDNSImpl.getIoLock())
        {
            if (this._jmDNSImpl.getPlannedAnswer() == _in)
            {
                this._jmDNSImpl.setPlannedAnswer(null);
            }

            // We use these sets to prevent duplicate records
            // FIXME - This should be moved into DNSOutgoing
            Set<DNSQuestion> questions = new HashSet<DNSQuestion>();
            Set<DNSRecord> answers = new HashSet<DNSRecord>();

            if (this._jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                try
                {
                    boolean isUnicast = (_port != DNSConstants.MDNS_PORT);

                    // Answer questions
                    for (DNSQuestion q : _in.getQuestions())
                    {
                        // for unicast responses the question must be included
                        if (isUnicast)
                        {
                            // out.addQuestion(q);
                            questions.add(q);
                        }

                        DNSRecordType type = q.getRecordType();
                        if (DNSRecordType.TYPE_ANY.equals(type) || DNSRecordType.TYPE_SRV.equals(type))
                        { // I ama not sure of why there is a special case here [PJYF Oct 15 2004]
                            if (this._jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(q.getName()))
                            {
                                // type = DNSConstants.TYPE_A;
                                DNSRecord answer = this._jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                if (answer != null)
                                {
                                    answers.add(answer);
                                }
                                answer = this._jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                if (answer != null)
                                {
                                    answers.add(answer);
                                }
                                type = DNSRecordType.TYPE_IGNORE;
                            }
                            else
                            {
                                if (this._jmDNSImpl.getServiceTypes().containsKey(q.getName().toLowerCase()))
                                {
                                    type = DNSRecordType.TYPE_PTR;
                                }
                            }
                        }

                        switch (type)
                        {
                            case TYPE_A:
                                {
                                    // Answer a query for a domain name
                                    // out = addAnswer( in, addr, port, out, host );
                                    DNSRecord answer = this._jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                    if (answer != null)
                                    {
                                        answers.add(answer);
                                    }
                                    break;
                                }
                            case TYPE_AAAA:
                                {
                                    // Answer a query for a domain name
                                    DNSRecord answer = this._jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                    if (answer != null)
                                    {
                                        answers.add(answer);
                                    }
                                    break;
                                }
                            case TYPE_PTR:
                                {
                                    // Answer a query for services of a given type

                                    // find matching services
                                    for (Iterator<? extends ServiceInfo> serviceIterator = this._jmDNSImpl
                                            .getServices().values().iterator(); serviceIterator.hasNext();)
                                    {
                                        ServiceInfoImpl info = (ServiceInfoImpl) serviceIterator.next();
                                        if (info.getState() == DNSState.ANNOUNCED)
                                        {
                                            if (q.getName().equalsIgnoreCase(info.getType()))
                                            {
                                                DNSRecord answer = this._jmDNSImpl.getLocalHost()
                                                        .getDNS4AddressRecord();
                                                if (answer != null)
                                                {
                                                    answers.add(answer);
                                                }
                                                answer = this._jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                                if (answer != null)
                                                {
                                                    answers.add(answer);
                                                }
                                                answers.add(new DNSRecord.Pointer(info.getType(),
                                                        DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN,
                                                        DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, info
                                                                .getQualifiedName()));
                                                answers.add(new DNSRecord.Service(info.getQualifiedName(),
                                                        DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN,
                                                        DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL,
                                                        info.getPriority(), info.getWeight(), info.getPort(),
                                                        this._jmDNSImpl.getLocalHost().getName()));
                                                answers.add(new DNSRecord.Text(info.getQualifiedName(),
                                                        DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN,
                                                        DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, info.getText()));
                                            }
                                        }
                                    }
                                    if (q.getName().equalsIgnoreCase("_services._mdns._udp.local."))
                                    {
                                        for (Iterator<String> serviceTypeIterator = this._jmDNSImpl.getServiceTypes()
                                                .values().iterator(); serviceTypeIterator.hasNext();)
                                        {
                                            answers.add(new DNSRecord.Pointer("_services._mdns._udp.local.",
                                                    DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN,
                                                    DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL,
                                                    serviceTypeIterator.next()));
                                        }
                                    }
                                    break;
                                }
                            case TYPE_SRV:
                            case TYPE_ANY:
                            case TYPE_TXT:
                                {
                                    ServiceInfoImpl info = (ServiceInfoImpl) this._jmDNSImpl.getServices().get(
                                            q.getName().toLowerCase());
                                    if (info != null && info.getState() == DNSState.ANNOUNCED)
                                    {
                                        DNSRecord answer = this._jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        answer = this._jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        answers.add(new DNSRecord.Pointer(info.getType(), DNSRecordType.TYPE_PTR,
                                                DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE,
                                                DNSConstants.DNS_TTL, info.getQualifiedName()));
                                        answers.add(new DNSRecord.Service(info.getQualifiedName(),
                                                DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE,
                                                DNSConstants.DNS_TTL, info.getPriority(), info.getWeight(), info
                                                        .getPort(), this._jmDNSImpl.getLocalHost().getName()));
                                        answers.add(new DNSRecord.Text(info.getQualifiedName(), DNSRecordType.TYPE_TXT,
                                                DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL,
                                                info.getText()));
                                    }
                                    break;
                                }
                            default:
                                {
                                    // System.out.println("JmDNSResponder.unhandled query:"+q);
                                    break;
                                }
                        }
                    }

                    // remove known answers, if the ttl is at least half of
                    // the correct value. (See Draft Cheshire chapter 7.1.).
                    for (DNSRecord knownAnswer : _in.getAnswers())
                    {
                        if (knownAnswer.getTTL() > DNSConstants.DNS_TTL / 2 && answers.remove(knownAnswer))
                        {
                            logger.log(Level.FINER, "JmDNS Responder Known Answer Removed");
                        }
                    }

                    // responde if we have answers
                    if (answers.size() != 0)
                    {
                        logger.finer("run() JmDNS responding");
                        DNSOutgoing out = null;
                        if (isUnicast)
                        {
                            out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false);
                        }
                        if (out != null)
                        {
                            for (DNSQuestion question : questions)
                            {
                                out.addQuestion(question);
                            }
                            for (DNSRecord answer : answers)
                            {
                                out = this._jmDNSImpl.addAnswer(_in, _addr, _port, out, answer);
                            }
                            this._jmDNSImpl.send(out);
                        }
                    }
                    this.cancel();
                }
                catch (Throwable e)
                {
                    logger.log(Level.WARNING, "run() exception ", e);
                    this._jmDNSImpl.close();
                }
            }
        }
    }
}