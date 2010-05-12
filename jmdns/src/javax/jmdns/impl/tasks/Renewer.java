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
 * The Renewer is there to send renewal announcement when the record expire for ours infos.
 */
public class Renewer extends DNSTask
{
    static Logger logger = Logger.getLogger(Renewer.class.getName());

    /**
     * The state of the announcer.
     */
    DNSState taskState = DNSState.ANNOUNCED;

    public Renewer(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);

        this.associate(DNSState.ANNOUNCED);
    }

    public void start(Timer timer)
    {
        if (this._jmDNSImpl.getState() != DNSState.CANCELED)
        {
            timer.schedule(this, DNSConstants.ANNOUNCED_RENEWAL_TTL_INTERVAL, DNSConstants.ANNOUNCED_RENEWAL_TTL_INTERVAL);
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
                    if ((info.getTask() == this) && info.getState().isAnnounced())
                    {
                        info.advanceState();
                        logger.finer("run() JmDNS announcing " + info.getQualifiedName() + " state " + info.getState());
                        info.addAnswers(out, DNSConstants.DNS_TTL, this._jmDNSImpl.getLocalHost());
                    }
                }
            }
            if (!out.isEmpty())
            {
                logger.finer("run() JmDNS announced");
                this._jmDNSImpl.send(out);
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
            this._jmDNSImpl.recover();
        }

        taskState = taskState.advance();
        if (!taskState.isAnnounced())
        {
            cancel();
        }
    }
}