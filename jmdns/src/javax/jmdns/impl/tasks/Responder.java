//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSEntry;
import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Responder sends a single answer for the specified service infos
 * and for the host name.
 */
public class Responder extends TimerTask
{
    static Logger logger = Logger.getLogger(Responder.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    private DNSIncoming in;
    private InetAddress addr;
    private int port;

    public Responder(JmDNSImpl jmDNSImpl, DNSIncoming in, InetAddress addr, int port)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.in = in;
        this.addr = addr;
        this.port = port;
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
        for (Iterator i = in.getQuestions().iterator(); i.hasNext();)
        {
            DNSEntry entry = (DNSEntry) i.next();
            if (entry instanceof DNSQuestion)
            {
                DNSQuestion q = (DNSQuestion) entry;
                logger.finest("start() question=" + q);
                iAmTheOnlyOne &= (q.getType() == DNSConstants.TYPE_SRV
                    || q.getType() == DNSConstants.TYPE_TXT
                    || q.getType() == DNSConstants.TYPE_A
                    || q.getType() == DNSConstants.TYPE_AAAA
                    || this.jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(q.getName())
                    || this.jmDNSImpl.getServices().containsKey(q.getName().toLowerCase()));
                if (!iAmTheOnlyOne)
                {
                    break;
                }
            }
        }
        int delay = (iAmTheOnlyOne && !in.isTruncated()) ? 0 : DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + JmDNSImpl.getRandom().nextInt(DNSConstants.RESPONSE_MAX_WAIT_INTERVAL - DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1) - in.elapseSinceArrival();
        if (delay < 0)
        {
            delay = 0;
        }
        logger.finest("start() Responder chosen delay=" + delay);
        this.jmDNSImpl.schedule(this, delay);
    }

    public void run()
    {
        synchronized (this.jmDNSImpl.getIoLock())
        {
            if (this.jmDNSImpl.getPlannedAnswer() == in)
            {
                this.jmDNSImpl.setPlannedAnswer(null);
            }

            // We use these sets to prevent duplicate records
            // FIXME - This should be moved into DNSOutgoing
            HashSet questions = new HashSet();
            HashSet answers = new HashSet();


            if (this.jmDNSImpl.getState() == DNSState.ANNOUNCED)
            {
                try
                {
                    boolean isUnicast = (port != DNSConstants.MDNS_PORT);


                    // Answer questions
                    for (Iterator iterator = in.getQuestions().iterator(); iterator.hasNext();)
                    {
                        DNSEntry entry = (DNSEntry) iterator.next();
                        if (entry instanceof DNSQuestion)
                        {
                            DNSQuestion q = (DNSQuestion) entry;

                            // for unicast responses the question must be included
                            if (isUnicast)
                            {
                                //out.addQuestion(q);
                                questions.add(q);
                            }

                            int type = q.getType();
                            if (type == DNSConstants.TYPE_ANY || type == DNSConstants.TYPE_SRV)
                            { // I ama not sure of why there is a special case here [PJYF Oct 15 2004]
                                if (this.jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(q.getName()))
                                {
                                    // type = DNSConstants.TYPE_A;
                                    DNSRecord answer = this.jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                    if (answer != null)
                                    {
                                        answers.add(answer);
                                    }
                                    answer = this.jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                    if (answer != null)
                                    {
                                        answers.add(answer);
                                    }
                                    type = DNSConstants.TYPE_IGNORE;
                                }
                                else
                                {
                                    if (this.jmDNSImpl.getServiceTypes().containsKey(q.getName().toLowerCase()))
                                    {
                                        type = DNSConstants.TYPE_PTR;
                                    }
                                }
                            }

                            switch (type)
                            {
                                case DNSConstants.TYPE_A:
                                    {
                                        // Answer a query for a domain name
                                        //out = addAnswer( in, addr, port, out, host );
                                        DNSRecord answer = this.jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        break;
                                    }
                                case DNSConstants.TYPE_AAAA:
                                    {
                                        // Answer a query for a domain name
                                        DNSRecord answer = this.jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                        if (answer != null)
                                        {
                                            answers.add(answer);
                                        }
                                        break;
                                    }
                                case DNSConstants.TYPE_PTR:
                                    {
                                        // Answer a query for services of a given type

                                        // find matching services
                                        for (Iterator serviceIterator = this.jmDNSImpl.getServices().values().iterator(); serviceIterator.hasNext();)
                                        {
                                            ServiceInfoImpl info = (ServiceInfoImpl) serviceIterator.next();
                                            if (info.getState() == DNSState.ANNOUNCED)
                                            {
                                                if (q.getName().equalsIgnoreCase(info.getType()))
                                                {
                                                    DNSRecord answer = this.jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                                    if (answer != null)
                                                    {
                                                        answers.add(answer);
                                                    }
                                                    answer = this.jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                                    if (answer != null)
                                                    {
                                                        answers.add(answer);
                                                    }
                                                    answers.add(new DNSRecord.Pointer(info.getType(), DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.getQualifiedName()));
                                                    answers.add(new DNSRecord.Service(info.getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, 
                                                            info.getPriority(), info.getWeight(), info.getPort(), this.jmDNSImpl.getLocalHost().getName()));
                                                    answers.add(new DNSRecord.Text(info.getQualifiedName(), DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, 
                                                            info.getText()));
                                                }
                                            }
                                        }
                                        if (q.getName().equalsIgnoreCase("_services._mdns._udp.local."))
                                        {
                                            for (Iterator serviceTypeIterator = this.jmDNSImpl.getServiceTypes().values().iterator(); serviceTypeIterator.hasNext();)
                                            {
                                                answers.add(new DNSRecord.Pointer("_services._mdns._udp.local.", DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, (String) serviceTypeIterator.next()));
                                            }
                                        }
                                        break;
                                    }
                                case DNSConstants.TYPE_SRV:
                                case DNSConstants.TYPE_ANY:
                                case DNSConstants.TYPE_TXT:
                                    {
                                        ServiceInfoImpl info = (ServiceInfoImpl) this.jmDNSImpl.getServices().get(q.getName().toLowerCase());
                                        if (info != null && info.getState() == DNSState.ANNOUNCED)
                                        {
                                            DNSRecord answer = this.jmDNSImpl.getLocalHost().getDNS4AddressRecord();
                                            if (answer != null)
                                            {
                                                answers.add(answer);
                                            }
                                            answer = this.jmDNSImpl.getLocalHost().getDNS6AddressRecord();
                                            if (answer != null)
                                            {
                                                answers.add(answer);
                                            }
                                            answers.add(new DNSRecord.Pointer(info.getType(), DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, info.getQualifiedName()));
                                            answers.add(new DNSRecord.Service(info.getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, 
                                                    info.getPriority(), info.getWeight(), info.getPort(), this.jmDNSImpl.getLocalHost().getName()));
                                            answers.add(new DNSRecord.Text(info.getQualifiedName(), DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN | DNSConstants.CLASS_UNIQUE, DNSConstants.DNS_TTL, info.getText()));
                                        }
                                        break;
                                    }
                                default :
                                    {
                                        //System.out.println("JmDNSResponder.unhandled query:"+q);
                                        break;
                                    }
                            }
                        }
                    }


                    // remove known answers, if the ttl is at least half of
                    // the correct value. (See Draft Cheshire chapter 7.1.).
                    for (Iterator i = in.getAnswers().iterator(); i.hasNext();)
                    {
                        DNSRecord knownAnswer = (DNSRecord) i.next();
                        if (knownAnswer.getTtl() > DNSConstants.DNS_TTL / 2 && answers.remove(knownAnswer))
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

                        for (Iterator i = questions.iterator(); i.hasNext();)
                        {
                            out.addQuestion((DNSQuestion) i.next());
                        }
                        for (Iterator i = answers.iterator(); i.hasNext();)
                        {
                            out = this.jmDNSImpl.addAnswer(in, addr, port, out, (DNSRecord) i.next());
                        }
                        this.jmDNSImpl.send(out);
                    }
                    this.cancel();
                }
                catch (Throwable e)
                {
                    logger.log(Level.WARNING, "run() exception ", e);
                    this.jmDNSImpl.close();
                }
            }
        }
    }
}