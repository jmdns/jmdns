//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Prober sends three consecutive probes for all service infos
 * that needs probing as well as for the host name.
 * The state of each service info of the host name is advanced, when a probe has
 * been sent for it.
 * When the prober has run three times, it launches an Announcer.
 * <p/>
 * If a conflict during probes occurs, the affected service infos (and affected
 * host name) are taken away from the prober. This eventually causes the prober
 * tho cancel itself.
 */
public class Prober extends TimerTask
{
    static Logger logger = Logger.getLogger(Prober.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    /**
     * The state of the prober.
     */
    DNSState taskState = DNSState.PROBING_1;

    public Prober(JmDNSImpl jmDNSImpl)
    {
        this.jmDNSImpl = jmDNSImpl;
        // Associate the host name to this, if it needs probing
        if (this.jmDNSImpl.getState() == DNSState.PROBING_1)
        {
            this.jmDNSImpl.setTask(this);
        }
        // Associate services to this, if they need probing
        synchronized (this.jmDNSImpl)
        {
            for (Iterator iterator = this.jmDNSImpl.getServices().values().iterator(); iterator.hasNext();)
            {
                ServiceInfoImpl info = (ServiceInfoImpl) iterator.next();
                if (info.getState() == DNSState.PROBING_1)
                {
                    info.setTask(this);
                }
            }
        }
    }


    public void start(Timer timer)
    {
        long now = System.currentTimeMillis();
        if (now - this.jmDNSImpl.getLastThrottleIncrement() < DNSConstants.PROBE_THROTTLE_COUNT_INTERVAL)
        {
            this.jmDNSImpl.setThrottle(this.jmDNSImpl.getThrottle() + 1);
        }
        else
        {
            this.jmDNSImpl.setThrottle(1);
        }
        this.jmDNSImpl.setLastThrottleIncrement(now);

        if (this.jmDNSImpl.getState() == DNSState.ANNOUNCED && this.jmDNSImpl.getThrottle() < DNSConstants.PROBE_THROTTLE_COUNT)
        {
            timer.schedule(this, JmDNSImpl.getRandom().nextInt(1 + DNSConstants.PROBE_WAIT_INTERVAL), DNSConstants.PROBE_WAIT_INTERVAL);
        }
        else
        {
            timer.schedule(this, DNSConstants.PROBE_CONFLICT_INTERVAL, DNSConstants.PROBE_CONFLICT_INTERVAL);
        }
    }

    public boolean cancel()
    {
        // Remove association from host name to this
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
        synchronized (this.jmDNSImpl.getIoLock())
        {
            DNSOutgoing out = null;
            try
            {
                // send probes for JmDNS itself
                if (this.jmDNSImpl.getState() == taskState && this.jmDNSImpl.getTask() == this)
                {
                    if (out == null)
                    {
                        out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    }
                    out.addQuestion(new DNSQuestion(this.jmDNSImpl.getLocalHost().getName(), DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));

                    this.jmDNSImpl.getLocalHost().addAddressRecords(out, true);
                    this.jmDNSImpl.advanceState();
                }
                // send probes for services
                // Defensively copy the services into a local list,
                // to prevent race conditions with methods registerService
                // and unregisterService.
                List list;
                synchronized (this.jmDNSImpl)
                {
                    list = new LinkedList(this.jmDNSImpl.getServices().values());
                }
                for (Iterator i = list.iterator(); i.hasNext();)
                {
                    ServiceInfoImpl info = (ServiceInfoImpl) i.next();

                    synchronized (info)
                    {
                        if (info.getState() == taskState && info.getTask() == this)
                        {
                            info.advanceState();
                            logger.fine("run() JmDNS probing " + info.getQualifiedName() + " state " + info.getState());
                            if (out == null)
                            {
                                out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                                out.addQuestion(new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));
                            }
                            // the "unique" flag should be not set here because these answers haven't been proven unique yet
                            // this means the record will not exactly match the announcement record
                            out.addAuthorativeAnswer(new DNSRecord.Service(info.getQualifiedName(), 
                                    DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, 
                                    info.getPriority(), info.getWeight(), info.getPort(), this.jmDNSImpl.getLocalHost().getName()));
                        }
                    }
                }
                if (out != null)
                {
                    logger.finer("run() JmDNS probing #" + taskState);
                    this.jmDNSImpl.send(out);
                }
                else
                {
                    // If we have nothing to send, another timer taskState ahead
                    // of us has done the job for us. We can cancel.
                    cancel();
                    return;
                }
            }
            catch (Throwable e)
            {
                logger.log(Level.WARNING, "run() exception ", e);
                this.jmDNSImpl.recover();
            }

            taskState = taskState.advance();
            if (!taskState.isProbing())
            {
                cancel();

                this.jmDNSImpl.startAnnouncer();
            }
        }
    }

}