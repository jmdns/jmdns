/**
 *
 */
package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.tasks.DNSTask;

/**
 *
 */
public abstract class DNSResolverTask extends DNSTask
{
    private static Logger logger = Logger.getLogger(DNSResolverTask.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    protected int _count = 0;

    /**
     * @param jmDNSImpl
     */
    public DNSResolverTask(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return super.toString() + " count: " + _count;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer)
    {
        if (!this._jmDNSImpl.isCanceling() && !this._jmDNSImpl.isCanceled())
        {
            timer.schedule(this, DNSConstants.QUERY_WAIT_INTERVAL, DNSConstants.QUERY_WAIT_INTERVAL);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run()
    {
        try
        {
            if (this._jmDNSImpl.isCanceling() || this._jmDNSImpl.isCanceled())
            {
                this.cancel();
            }
            else
            {
                if (_count++ < 3)
                {
                    logger.finer("run() JmDNS " + this.description());
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out = this.addQuestions(out);
                    if (this._jmDNSImpl.isAnnounced())
                    {
                        out = this.addAnswers(out);
                    }
                    if (!out.isEmpty())
                        this._jmDNSImpl.send(out);
                }
                else
                {
                    // After three queries, we can quit.
                    this.cancel();
                }
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this._jmDNSImpl.recover();
        }
    }

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different than the message parameter.
     *
     * @param out
     *            outgoing message
     * @return the outgoing message.
     * @throws IOException
     */
    protected abstract DNSOutgoing addQuestions(DNSOutgoing out) throws IOException;

    /**
     * Overridden by subclasses to add questions to the message.<br/>
     * <b>Note:</b> Because of message size limitation the returned message may be different than the message parameter.
     *
     * @param out
     *            outgoing message
     * @return the outgoing message.
     * @throws IOException
     */
    protected abstract DNSOutgoing addAnswers(DNSOutgoing out) throws IOException;

    /**
     * Returns a description of the resolver for debugging
     *
     * @return resolver description
     */
    protected abstract String description();

}
