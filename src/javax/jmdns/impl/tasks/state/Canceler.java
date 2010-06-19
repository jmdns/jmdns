//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks.state;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;

/**
 * The Canceler sends two announces with TTL=0 for the specified services.
 */
public class Canceler extends DNSStateTask
{
    static Logger logger = Logger.getLogger(Canceler.class.getName());

    /**
     * The state of the canceler.
     */
    DNSState taskState = DNSState.CANCELING_1;

    public Canceler(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl, 0);

        this.associate(DNSState.CANCELING_1);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName()
    {
        return "Canceler(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return super.toString() + " state: " + taskState;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer)
    {
        timer.schedule(this, 0, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.TimerTask#cancel()
     */
    @Override
    public boolean cancel()
    {
        this.removeAssociation();

        return super.cancel();
    }

    @Override
    public void run()
    {
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
        try
        {
            logger.finer("run() JmDNS canceling service");
            // send probes for JmDNS itself
            synchronized (this.getDns())
            {
                if (this.getDns().isAssociatedWithTask(this, taskState))
                {
                    for (DNSRecord answer : this.getDns().getLocalHost().answers(this.getTTL()))
                    {
                        out = this.addAnswer(out, null, answer);
                    }
                    this.getDns().advanceState();
                }
            }
            // send announces for services
            for (ServiceInfo serviceInfo : this.getDns().getServices().values())
            {
                ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
                synchronized (info)
                {
                    if (info.isAssociatedWithTask(this, taskState))
                    {
                        logger.finer("run() JmDNS announcing " + info.getQualifiedName());
                        for (DNSRecord answer : info.answers(this.getTTL(), this.getDns().getLocalHost()))
                        {
                            out = this.addAnswer(out, null, answer);
                        }
                        info.advanceState();
                    }
                }
            }
            if (!out.isEmpty())
            {
                logger.finer("run() JmDNS announced");
                this.getDns().send(out);
            }
            else
            {
                // If we have nothing to send, another timer taskState ahead of us has done the job for us. We can cancel.
                cancel();
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.getDns().recover();
        }

        taskState = taskState.advance();
        if (!taskState.isCanceling())
        {
            cancel();
        }
    }
}