//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Announcer sends an accumulated query of all announces, and advances the state of all serviceInfos, for which it
 * has sent an announce. The Announcer also sends announcements and advances the state of JmDNS itself.
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
        // Associate host to this, if it needs announcing
        if (this._jmDNSImpl.getState() == DNSState.ANNOUNCING_1)
        {
            this._jmDNSImpl.setTask(this);
        }
        // Associate services to this, if they need announcing
        synchronized (this._jmDNSImpl)
        {
            for (Iterator<? extends ServiceInfo> s = this._jmDNSImpl.getServices().values().iterator(); s.hasNext();)
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

    @Override
    public boolean cancel()
    {
        // Remove association from host to this
        if (this._jmDNSImpl.getTask() == this)
        {
            this._jmDNSImpl.setTask(null);
        }

        // Remove associations from services to this
        synchronized (this._jmDNSImpl)
        {
            for (Iterator<? extends ServiceInfo> i = this._jmDNSImpl.getServices().values().iterator(); i.hasNext();)
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

    @Override
    public void run()
    {
        DNSOutgoing out = null;
        try
        {
            // send probes for JmDNS itself
            if (this._jmDNSImpl.getState() == taskState)
            {
                if (out == null)
                {
                    out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                }
                this._jmDNSImpl.getLocalHost().addAddressRecords(out, false);
                this._jmDNSImpl.advanceState();
            }
            // send announces for services
            // Defensively copy the services into a local list,
            // to prevent race conditions with methods registerService
            // and unregisterService.
            List<? extends ServiceInfo> list;
            synchronized (this._jmDNSImpl)
            {
                list = new ArrayList<ServiceInfo>(this._jmDNSImpl.getServices().values());
            }
            for (Iterator<? extends ServiceInfo> i = list.iterator(); i.hasNext();)
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
                        info.addAnswers(out, DNSConstants.DNS_TTL, this._jmDNSImpl.getLocalHost());
                    }
                }
            }
            if (out != null)
            {
                logger.finer("run() JmDNS announcing #" + taskState);
                this._jmDNSImpl.send(out);
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
            this._jmDNSImpl.recover();
        }

        taskState = taskState.advance();
        if (!taskState.isAnnouncing())
        {
            cancel();

            this._jmDNSImpl.startRenewer();
        }
    }
}