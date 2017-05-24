// Licensed under Apache License version 2.0
package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * This is the root class for all resolver tasks.
 * 
 * @author Pierre Frisch
 */
public abstract class DNSResolverTask extends DNSTask {
    private static Logger logger = LoggerFactory.getLogger(DNSResolverTask.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    protected int         _count = 0;

    /**
     * @param jmDNSImpl
     */
    public DNSResolverTask(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " count: " + _count;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer) {
        if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
            timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        try {
            if (this.getDns().isCanceling() || this.getDns().isCanceled()) {
                this.cancel();
            } else {
                if (_count++ < 3) {
                    logger.debug("{}.run() JmDNS {}",this.getName(), this.description());

                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out = this.addQuestions(out);
                    if (this.getDns().isAnnounced()) {
                        out = this.addAnswers(out);
                    }
                    if (!out.isEmpty()) {
                        this.getDns().send(out);
                    }
                } else {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
        } catch (Throwable e) {
            logger.warn(this.getName() + ".run() exception ", e);
            this.getDns().recover();
        }
    }

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different than the message parameter.
     * 
     * @param out
     *            outgoing message
     * @return the outgoing message.
     * @exception IOException
     */
    protected abstract DNSOutgoing addQuestions(DNSOutgoing out) throws IOException;

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different than the message parameter.
     * 
     * @param out
     *            outgoing message
     * @return the outgoing message.
     * @exception IOException
     */
    protected abstract DNSOutgoing addAnswers(DNSOutgoing out) throws IOException;

    /**
     * Returns a description of the resolver for debugging
     * 
     * @return resolver description
     */
    protected abstract String description();

}
