//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
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

    /**
     *
     */
    private boolean _unicast;

    public Responder(JmDNSImpl jmDNSImpl, DNSIncoming in, int port)
    {
        super(jmDNSImpl);
        this._in = in;
        this._unicast = (port != DNSConstants.MDNS_PORT);
    }

    public void start()
    {
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "8 Responding":
        // We respond immediately if we know for sure, that we are the only one who can respond to the query.
        // In all other cases, we respond within 20-120 ms.
        //
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "7.2 Multi-Packet Known Answer Suppression":
        // We respond after 20-120 ms if the query is truncated.

        boolean iAmTheOnlyOne = true;
        for (DNSQuestion question : _in.getQuestions())
        {
            logger.finest("start() question=" + question);
            iAmTheOnlyOne &= DNSRecordType.TYPE_SRV.equals(question.getRecordType()) || DNSRecordType.TYPE_TXT.equals(question.getRecordType()) || DNSRecordType.TYPE_A.equals(question.getRecordType())
                    || DNSRecordType.TYPE_AAAA.equals(question.getRecordType()) || this._jmDNSImpl.getLocalHost().getName().equalsIgnoreCase(question.getName()) || this._jmDNSImpl.getServices().containsKey(question.getName().toLowerCase());
            if (!iAmTheOnlyOne)
            {
                break;
            }
        }
        int delay = (iAmTheOnlyOne && !_in.isTruncated()) ? 0 : DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + JmDNSImpl.getRandom().nextInt(DNSConstants.RESPONSE_MAX_WAIT_INTERVAL - DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1) - _in.elapseSinceArrival();
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
        this._jmDNSImpl.ioLock();
        try
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
                    // Answer questions
                    for (DNSQuestion question : _in.getQuestions())
                    {
                        // for unicast responses the question must be included
                        if (_unicast)
                        {
                            // out.addQuestion(q);
                            questions.add(question);
                        }

                        question.addAnswers(_jmDNSImpl, answers);
                    }

                    // remove known answers, if the ttl is at least half of the correct value. (See Draft Cheshire chapter 7.1.).
                    for (DNSRecord knownAnswer : _in.getAnswers())
                    {
                        if (knownAnswer.getTTL() > DNSConstants.DNS_TTL / 2 && answers.remove(knownAnswer))
                        {
                            logger.log(Level.FINER, "JmDNS Responder Known Answer Removed");
                        }
                    }

                    // respond if we have answers
                    if (answers.size() != 0)
                    {
                        logger.finer("run() JmDNS responding");
                        DNSOutgoing out = newDNSOutgoing(_unicast, _in.getId());
                        for (DNSQuestion question : questions)
                        {
                            out.addQuestion(question);
                        }
                        for (DNSRecord answer : answers)
                        {
                            try
                            {
                                out.addAnswer(_in, answer);
                            }
                            catch (final IOException e)
                            {
                                // The message is full send it and start a new one
                                // Mark the response as truncated
                                out.setFlags(out.getFlags() | DNSConstants.FLAGS_TC);
                                this._jmDNSImpl.send(out);

                                // Start a new one.
                                out = newDNSOutgoing(_unicast, _in.getId());
                                out.addAnswer(_in, answer);
                            }
                        }
                        this._jmDNSImpl.send(out);
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
        finally
        {
            this._jmDNSImpl.ioUnlock();
        }
    }

    private static final DNSOutgoing newDNSOutgoing(boolean isUnicast, int id)
    {
        DNSOutgoing out = null;
        if (isUnicast)
        {
            out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false);
        }
        out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        out.setId(id);
        return out;
    }

}