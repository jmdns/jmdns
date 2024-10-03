// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.tasks;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;

/**
 * The Responder sends a single answer for the specified service infos and for the host name.
 */
public class Responder extends DNSTask {
    private static final Logger logger = LoggerFactory.getLogger(Responder.class);
    private final DNSIncoming dnsIncoming;
    private final InetAddress inetAddress;
    private final int port;
    private final boolean unicast;

    public Responder(JmDNSImpl jmDNSImpl, DNSIncoming in, InetAddress addr, int port) {
        super(jmDNSImpl);
        this.dnsIncoming = in;
        this.inetAddress = addr;
        this.port = port;
        this.unicast = (port != DNSConstants.MDNS_PORT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "Responder(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " incoming: " + dnsIncoming;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer) {
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "7 Responding":
        // We respond immediately if we know for sure, that we are the only one who can respond to the query.
        // In all other cases, we respond within 20-120 ms.
        //
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "6.2 Multi-Packet Known Answer Suppression":
        // We respond after 20-120 ms if the query is truncated.

        boolean iAmTheOnlyOne = true;
        for (DNSQuestion question : dnsIncoming.getQuestions()) {
            logger.trace("{}.start() question={}", this.getName(), question);
            iAmTheOnlyOne = question.iAmTheOnlyOne(this.getDns());
            if (!iAmTheOnlyOne) {
                break;
            }
        }
        int delay = (iAmTheOnlyOne && !dnsIncoming.isTruncated()) ? 0 : DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + JmDNSImpl.getRandom().nextInt(DNSConstants.RESPONSE_MAX_WAIT_INTERVAL - DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1) - dnsIncoming.elapseSinceArrival();
        if (delay < 0) {
            delay = 0;
        }
        logger.trace("{}.start() Responder chosen delay={}", this.getName(), delay);

        if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
            timer.schedule(this, delay);
        }
    }

    @Override
    public void run() {
        this.getDns().respondToQuery(dnsIncoming);

        // We use these sets to prevent duplicate records
        Set<DNSQuestion> questions = new HashSet<>();
        Set<DNSRecord> answers = new HashSet<>();

        if (this.getDns().isAnnounced()) {
            try {
                // Answer questions
                for (DNSQuestion question : dnsIncoming.getQuestions()) {
                    logger.debug("{}.run() JmDNS responding to: {}", this.getName(), question);

                    // for unicast responses the question must be included
                    if (unicast) {
                        questions.add(question);
                    }

                    question.addAnswers(this.getDns(), answers);
                }

                // remove known answers, if the TTL is at least half of the correct value. (See Draft Cheshire chapter 7.1.).
                long now = System.currentTimeMillis();
                for (DNSRecord knownAnswer : dnsIncoming.getAnswers()) {
                    if (knownAnswer.isStale(now)) {
                        answers.remove(knownAnswer);
                        logger.debug("{} - JmDNS Responder Known Answer Removed", this.getName());
                    }
                }

                // respond if we have answers
                if (!answers.isEmpty()) {
                    logger.debug("{}.run() JmDNS responding", this.getName());

                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, !unicast, dnsIncoming.getSenderUDPPayload());
                    out.setDestination(new InetSocketAddress(inetAddress, port));
                    out.setId(dnsIncoming.getId());
                    for (DNSQuestion question : questions) {
                        if (question != null) {
                            out = this.addQuestion(out, question);
                        }
                    }
                    for (DNSRecord answer : answers) {
                        if (answer != null) {
                            out = this.addAnswer(out, dnsIncoming, answer);

                        }
                    }
                    if (!out.isEmpty()) this.getDns().send(out);
                }
            } catch (Throwable e) {
                logger.warn("{}.run() exception ", this.getName(), e);
                this.getDns().close();
            }
        }
    }
}