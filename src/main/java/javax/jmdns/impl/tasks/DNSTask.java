// Licensed under Apache License version 2.0
package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;

/**
 * This is the root class for all task scheduled by the timer in JmDNS.
 *
 * @author Pierre Frisch
 */
public abstract class DNSTask extends TimerTask {

    private final JmDNSImpl jmDNS;

    protected DNSTask(JmDNSImpl jmDNSImpl) {
        super();
        jmDNS = jmDNSImpl;
    }

    /**
     * Return the DNS associated with this task.
     *
     * @return associated DNS
     */
    public JmDNSImpl getDns() {
        return jmDNS;
    }

    /**
     * Start this task.
     *
     * @param timer task timer.
     */
    public abstract void start(Timer timer);

    /**
     * Return this task name.
     *
     * @return task name
     */
    public abstract String getName();

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Add a question to the message.
     *
     * @param out outgoing message
     * @param rec DNS question
     * @return outgoing message for the next question
     * @throws IOException if any IO error occurs
     */
    public DNSOutgoing addQuestion(DNSOutgoing out, DNSQuestion rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addQuestion(rec);
        } catch (final IOException e) {
            newOut = getDnsOutgoing(newOut);
            newOut.addQuestion(rec);
        }
        return newOut;
    }

    private DNSOutgoing getDnsOutgoing(DNSOutgoing newOut) throws IOException {
        int flags = newOut.getFlags();
        boolean multicast = newOut.isMulticast();
        int maxUDPPayload = newOut.getMaxUDPPayload();
        int id = newOut.getId();

        newOut.setFlags(flags | DNSConstants.FLAGS_TC);
        newOut.setId(id);
        jmDNS.send(newOut);

        newOut = new DNSOutgoing(flags, multicast, maxUDPPayload);
        return newOut;
    }

    /**
     * Add an answer if it is not suppressed.
     *
     * @param out outgoing message
     * @param in  incoming request
     * @param rec DNS record answer
     * @return outgoing message for the next answer
     * @throws IOException if any IO error occurs
     */
    public DNSOutgoing addAnswer(DNSOutgoing out, DNSIncoming in, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAnswer(in, rec);
        } catch (final IOException e) {
            newOut = getDnsOutgoing(newOut);
            newOut.addAnswer(in, rec);
        }
        return newOut;
    }

    /**
     * Add an answer to the message.
     *
     * @param out outgoing message
     * @param rec DNS record answer
     * @param now the current time
     * @return outgoing message for the next answer
     * @throws IOException if any IO error occurs
     */
    public DNSOutgoing addAnswer(DNSOutgoing out, DNSRecord rec, long now) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAnswer(rec, now);
        } catch (final IOException e) {
            newOut = getDnsOutgoing(newOut);
            newOut.addAnswer(rec, now);
        }
        return newOut;
    }

    /**
     * Add an authoritative answer to the message.
     *
     * @param out outgoing message
     * @param rec DNS record answer
     * @return outgoing message for the next answer
     * @throws IOException if any IO error occurs
     */
    public DNSOutgoing addAuthoritativeAnswer(DNSOutgoing out, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAuthorativeAnswer(rec);
        } catch (final IOException e) {
            newOut = getDnsOutgoing(newOut);
            newOut.addAuthorativeAnswer(rec);
        }
        return newOut;
    }

    /**
     * Adds an answer to the record. Omit if there is no room.
     *
     * @param out outgoing message
     * @param in  incoming request
     * @param rec DNS record answer
     * @return outgoing message for the next answer
     * @throws IOException if any IO error occurs
     */
    public DNSOutgoing addAdditionalAnswer(DNSOutgoing out, DNSIncoming in, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAdditionalAnswer(in, rec);
        } catch (final IOException e) {
            newOut = getDnsOutgoing(newOut);
            newOut.addAdditionalAnswer(in, rec);
        }
        return newOut;
    }

}