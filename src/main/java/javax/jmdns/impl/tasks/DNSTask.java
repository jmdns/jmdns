// Licensed under Apache License version 2.0
package javax.jmdns.impl.tasks;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSOutgoing.MessageFullException;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;

/**
 * This is the root class for all task scheduled by the timer in JmDNS.
 * 
 * @author Pierre Frisch
 */
public abstract class DNSTask extends TimerTask {

    /**
     *
     */
    private final JmDNSImpl _jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    protected DNSTask(JmDNSImpl jmDNSImpl) {
        super();
        this._jmDNSImpl = jmDNSImpl;
    }

    /**
     * Return the DNS associated with this task.
     * 
     * @return associated DNS
     */
    public JmDNSImpl getDns() {
        return _jmDNSImpl;
    }

    /**
     * Start this task.
     * 
     * @param timer
     *            task timer.
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
     * @param out
     *            outgoing message
     * @param rec
     *            DNS question
     * @return outgoing message for the next question
     * @exception IOException
     */
    public DNSOutgoing addQuestion(DNSOutgoing out, DNSQuestion rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addQuestion(rec);
        } catch (MessageFullException exception) {
            newOut = this._jmDNSImpl.handleOutgoingMessageOverflow(newOut);
            try {
                newOut.addQuestion(rec);
            } catch (MessageFullException e) {
                // This makes no sense we should at least be able to add a question to a newly created message
                throw new IOException("Cannot add a question to a new message");
            }
        }
        return newOut;
    }

    /**
     * Add an answer if it is not suppressed.
     * 
     * @param out
     *            outgoing message
     * @param in
     *            incoming request
     * @param rec
     *            DNS record answer
     * @return outgoing message for the next answer
     * @exception IOException
     */
    public DNSOutgoing addAnswer(DNSOutgoing out, DNSIncoming in, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAnswer(in, rec);
        } catch (MessageFullException exception) {
            newOut = this._jmDNSImpl.handleOutgoingMessageOverflow(newOut);
            try {
                newOut.addAnswer(in, rec);
            } catch (MessageFullException e) {
                // This makes no sense we should at least be able to add an answer to a newly created message
                throw new IOException("Cannot add an answer to a new message");
            }
        }
        return newOut;
    }

    /**
     * Add an answer to the message.
     * 
     * @param out
     *            outgoing message
     * @param rec
     *            DNS record answer
     * @param now
     * @return outgoing message for the next answer
     * @exception IOException
     */
    public DNSOutgoing addAnswer(DNSOutgoing out, DNSRecord rec, long now) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAnswer(rec, now);
        } catch (MessageFullException exception) {
            newOut = this._jmDNSImpl.handleOutgoingMessageOverflow(newOut);
            try {
                newOut.addAnswer(rec, now);
            } catch (MessageFullException e) {
                // This makes no sense we should at least be able to add an answer to a newly created message
                throw new IOException("Cannot add an answer to a new message");
            }
        }
        return newOut;
    }

    /**
     * Add an authoritative answer to the message.
     * 
     * @param out
     *            outgoing message
     * @param rec
     *            DNS record answer
     * @return outgoing message for the next answer
     * @exception IOException
     */
    public DNSOutgoing addAuthoritativeAnswer(DNSOutgoing out, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAuthorativeAnswer(rec);
        } catch (MessageFullException exception) {
            newOut = this._jmDNSImpl.handleOutgoingMessageOverflow(newOut);
            try {
                newOut.addAuthorativeAnswer(rec);
            } catch (MessageFullException e) {
                // This makes no sense we should at least be able to add an answer to a newly created message
                throw new IOException("Cannot add an authoritative answer to a new message");
            }
        }
        return newOut;
    }

    /**
     * Add an additional answer to the record. Omit if there is no room.
     * 
     * @param out
     *            outgoing message
     * @param in
     *            incoming request
     * @param rec
     *            DNS record answer
     * @return outgoing message for the next answer
     * @exception IOException
     */
    public DNSOutgoing addAdditionalAnswer(DNSOutgoing out, DNSIncoming in, DNSRecord rec) throws IOException {
        DNSOutgoing newOut = out;
        try {
            newOut.addAdditionalAnswer(in, rec);
        } catch (MessageFullException exception) {
            newOut = this._jmDNSImpl.handleOutgoingMessageOverflow(newOut);
            try {
                newOut.addAdditionalAnswer(in, rec);
            } catch (MessageFullException e) {
                // This makes no sense we should at least be able to add an answer to a newly created message
                throw new IOException("Cannot add an additional answer to a new message");
            }
        }
        return newOut;
    }

}
