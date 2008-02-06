//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Announcer sends an accumulated query of all announces, and advances
 * the state of all serviceInfos, for which it has sent an announce.
 * The Announcer also sends announcements and advances the state of JmDNS itself.
 * <p/>
 * When the announcer has run two times, it finishes.
 */
public class Announcer extends TimerTask
{
    static Logger logger = Logger.getLogger(Announcer.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    /**
     * The state of the announcer.
     */
    DNSState taskState = DNSState.ANNOUNCING_1;

    public Announcer(JmDNSImpl jmDNSImpl)
    {
        this.jmDNSImpl = jmDNSImpl;
        // Associate host to this, if it needs announcing
        if (this.jmDNSImpl.getState() == DNSState.ANNOUNCING_1)
        {
            this.jmDNSImpl.setTask(this);
        }
        // Associate services to this, if they need announcing
        synchronized (this.jmDNSImpl)
        {
            for (Iterator s = this.jmDNSImpl.getServices().values().iterator(); s.hasNext();)
            {
                ServiceInfoImpl info = (ServiceInfoImpl) s.next();
                if (info.getState() == DNSState.ANNOUNCING_1)
                {
                    info.setTask(this);
                }
            }
        }
    }

    public void start(Timer timer)
    {
        timer.schedule(this, DNSConstants.ANNOUNCE_WAIT_INTERVAL, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
    }

    public boolean cancel()
    {
        // Remove association from host to this
        if (this.jmDNSImpl.getTask() == this)
        {
            this.jmDNSImpl.setTask(null);
        }

        // Remove associations from services to this
        synchronized (this.jmDNSImpl)
        {
            for (Iterator i = this.jmDNSImpl.getServices().values().iterator(); i.hasNext();)
            {
                ServiceInfoImpl info = (ServiceInfoImpl) i.next();
                if (info.getTask() == this)
                {
                    info.setTask(null);
                }
            }
        }

        return super.cancel();
    }

    public void run()
    {
        DNSOutgoing out = null;
        try
        {
            // send probes for JmDNS itself
            if (this.jmDNSImpl.getState() == taskState)
            {
                if (out == null)
                {
                    out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                }
                this.jmDNSImpl.getLocalHost().addAddressRecords(out, false);
                this.jmDNSImpl.advanceState();
            }
            // send announces for services
            // Defensively copy the services into a local list,
            // to prevent race conditions with methods registerService
            // and unregisterService.
            List list;
            synchronized (this.jmDNSImpl)
            {
                list = new ArrayList(this.jmDNSImpl.getServices().values());
            }
            for (Iterator i = list.iterator(); i.hasNext();)
            {
                ServiceInfoImpl info = (ServiceInfoImpl) i.next();
                synchronized (info)
                {
                    if (info.getState() == taskState && info.getTask() == this)
                    {
                        info.advanceState();
                        logger.finer("run() JmDNS announcing " + info.getQualifiedName() + " state " + info.getState());
                        if (out == null)
                        {
                            out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                        }
                        info.addAnswers(out, DNSConstants.DNS_TTL, this.jmDNSImpl.getLocalHost());
                    }
                }
            }
            if (out != null)
            {
                logger.finer("run() JmDNS announcing #" + taskState);
                this.jmDNSImpl.send(out);
            }
            else
            {
                // If we have nothing to send, another timer taskState ahead
                // of us has done the job for us. We can cancel.
                cancel();
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.jmDNSImpl.recover();
        }

        taskState = taskState.advance();
        if (!taskState.isAnnouncing())
        {
            cancel();

            this.jmDNSImpl.startRenewer();
        }
    }
}