//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;

/**
 * The Announcer sends an accumulated query of all announces, and advances the state of all serviceInfos, for which it has sent an announce. The Announcer also sends announcements and advances the state of JmDNS itself.
 * <p/>
 * When the announcer has run two times, it finishes.
 */
public class Announcer extends DNSTask
{
    static Logger logger = Logger.getLogger(Announcer.class.getName());

    /**
     * The state of the announcer.
     */
    DNSState taskState = DNSState.ANNOUNCING_1;

    public Announcer(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);

        this.associate(DNSState.ANNOUNCING_1);
    }

    public void start(Timer timer)
    {
        if (this._jmDNSImpl.getState() != DNSState.CANCELED)
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
            synchronized (_jmDNSImpl)
            {
                if ((this._jmDNSImpl.getTask() == this) && this._jmDNSImpl.getState() == taskState)
                {
                    this._jmDNSImpl.getLocalHost().addAddressRecords(out, false);
                    this._jmDNSImpl.advanceState();
                }
            }
            // send announces for services
            for (ServiceInfo serviceInfo : this._jmDNSImpl.getServices().values())
            {
                ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
                synchronized (info)
                {
                    if ((info.getTask() == this) && info.getState().isAnnouncing())
                    {
                        info.advanceState();
                        logger.finer("run() JmDNS announcing " + info.getQualifiedName() + " state " + info.getState());
                        info.addAnswers(out, DNSConstants.DNS_TTL, this._jmDNSImpl.getLocalHost());
                    }
                }
            }
            if (!out.isEmpty())
            {
                logger.finer("run() JmDNS announcing #" + taskState);
                this._jmDNSImpl.send(out);
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
            this._jmDNSImpl.recover();
        }

        taskState = taskState.advance();
        if (!taskState.isAnnouncing())
        {
            this.cancel();

            this._jmDNSImpl.startRenewer();
        }
    }

}