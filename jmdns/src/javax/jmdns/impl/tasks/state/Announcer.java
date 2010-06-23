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
 * The Announcer sends an accumulated query of all announces, and advances the state of all serviceInfos, for which it has sent an announce. The Announcer also sends announcements and advances the state of JmDNS itself.
 * <p/>
 * When the announcer has run two times, it finishes.
 */
public class Announcer extends DNSStateTask
{
    static Logger logger = Logger.getLogger(Announcer.class.getName());

    /**
     * The state of the announcer.
     */
    DNSState taskState = DNSState.ANNOUNCING_1;

    public Announcer(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl, defaultTTL());

        this.associate(DNSState.ANNOUNCING_1);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName()
    {
        return "Announcer(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
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
        if (!this.getDns().isCanceling() && !this.getDns().isCanceled())
        {
            timer.schedule(this, DNSConstants.ANNOUNCE_WAIT_INTERVAL, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
        }
    }

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
            // send probes for JmDNS itself
            synchronized (this.getDns())
            {
                if (this.getDns().isAssociatedWithTask(this, taskState))
                {
                    for (DNSRecord answer : this.getDns().getLocalHost().answers(this.getTTL()))
                    {
                        out = this.addAnswer(out, null, answer);
                    }
                    this.getDns().advanceState(this);
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
                        info.advanceState(this);
                    }
                }
            }
            if (!out.isEmpty())
            {
                logger.finer("run() JmDNS announcing #" + taskState);
                this.getDns().send(out);
            }
            else
            {
                // If we have nothing to send, another timer taskState ahead of us has done the job for us. We can cancel.
                this.cancel();
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.getDns().recover();
        }

        taskState = taskState.advance();
        if (!taskState.isAnnouncing())
        {
            this.cancel();

            this.getDns().startRenewer();
        }
    }

}