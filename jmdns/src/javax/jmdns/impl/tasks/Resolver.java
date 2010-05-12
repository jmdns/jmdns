/**
 *
 */
package javax.jmdns.impl.tasks;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;

/**
 *
 */
public abstract class Resolver extends DNSTask
{
    private static Logger logger = Logger.getLogger(Resolver.class.getName());

    /**
     * Counts the number of queries being sent.
     */
    protected int _count = 0;

    /**
     * @param jmDNSImpl
     */
    public Resolver(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
    }

    public void start(Timer timer)
    {
        if (this._jmDNSImpl.getState() != DNSState.CANCELED)
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
            if (this._jmDNSImpl.getState() == DNSState.CANCELED)
            {
                this.cancel();
            }
            else
            {
                if (_count++ < 3)
                {
                    logger.finer("run() JmDNS " + this.description());
                    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    boolean answersAdded = false;
                    boolean questionsAdded = this.addQuestions(out);
                    if (this._jmDNSImpl.getState() == DNSState.ANNOUNCED)
                    {
                        answersAdded = this.addAnswers(out);
                    }
                    if (questionsAdded || answersAdded)
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
     * Overridden by subclasses to add questions to the message
     *
     * @param out
     *            outgoing message
     * @return <code>true</code> if questions where added to the message, <code>false</code> otherwise.
     */
    protected abstract boolean addQuestions(DNSOutgoing out);

    /**
     * Overridden by subclasses to add questions to the message
     *
     * @param out
     *            outgoing message
     * @return <code>true</code> if answers where added to the message, <code>false</code> otherwise.
     */
    protected abstract boolean addAnswers(DNSOutgoing out);

    /**
     * Returns a description of the resolver for debugging
     *
     * @return resolver description
     */
    protected abstract String description();

}
