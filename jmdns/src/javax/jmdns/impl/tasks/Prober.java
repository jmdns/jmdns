//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.DNSRecordClass;
import javax.jmdns.impl.DNSRecordType;
import javax.jmdns.impl.DNSState;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Prober sends three consecutive probes for all service infos that needs probing as well as for the host name. The
 * state of each service info of the host name is advanced, when a probe has been sent for it. When the prober has run
 * three times, it launches an Announcer.
 * <p/>
 * If a conflict during probes occurs, the affected service infos (and affected host name) are taken away from the
 * prober. This eventually causes the prober tho cancel itself.
 */
public class Prober extends DNSTask
{
    static Logger logger = Logger.getLogger(Prober.class.getName());

    /**
     * The state of the prober.
     */
    DNSState taskState = DNSState.PROBING_1;

    public Prober(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
        // Associate the host name to this, if it needs probing
        if (this._jmDNSImpl.getState() == DNSState.PROBING_1)
        {
            this._jmDNSImpl.setTask(this);
        }
        // Associate services to this, if they need probing
        synchronized (this._jmDNSImpl)
        {
            for (Iterator<? extends ServiceInfo> iterator = this._jmDNSImpl.getServices().values().iterator(); iterator
                    .hasNext();)
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
        if (now - this._jmDNSImpl.getLastThrottleIncrement() < DNSConstants.PROBE_THROTTLE_COUNT_INTERVAL)
        {
            this._jmDNSImpl.setThrottle(this._jmDNSImpl.getThrottle() + 1);
        }
        else
        {
            this._jmDNSImpl.setThrottle(1);
        }
        this._jmDNSImpl.setLastThrottleIncrement(now);

        if (this._jmDNSImpl.getState() == DNSState.ANNOUNCED
                && this._jmDNSImpl.getThrottle() < DNSConstants.PROBE_THROTTLE_COUNT)
        {
            timer.schedule(this, JmDNSImpl.getRandom().nextInt(1 + DNSConstants.PROBE_WAIT_INTERVAL),
                    DNSConstants.PROBE_WAIT_INTERVAL);
        }
        else
        {
            timer.schedule(this, DNSConstants.PROBE_CONFLICT_INTERVAL, DNSConstants.PROBE_CONFLICT_INTERVAL);
        }
    }

    @Override
    public boolean cancel()
    {
        // Remove association from host name to this
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
        synchronized (this._jmDNSImpl.getIoLock())
        {
            DNSOutgoing out = null;
            try
            {
                // send probes for JmDNS itself
                if (this._jmDNSImpl.getState() == taskState && this._jmDNSImpl.getTask() == this)
                {
                    // if (out == null)
                    // {
                    // }
                    out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
                    out.addQuestion(new DNSQuestion(this._jmDNSImpl.getLocalHost().getName(), DNSRecordType.TYPE_ANY,
                            DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));

                    this._jmDNSImpl.getLocalHost().addAddressRecords(out, true);
                    this._jmDNSImpl.advanceState();
                }
                // send probes for services
                // Defensively copy the services into a local list,
                // to prevent race conditions with methods registerService
                // and unregisterService.
                List<? extends ServiceInfo> list;
                synchronized (this._jmDNSImpl)
                {
                    list = new LinkedList<ServiceInfo>(this._jmDNSImpl.getServices().values());
                }
                for (Iterator<? extends ServiceInfo> i = list.iterator(); i.hasNext();)
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
                                out.addQuestion(new DNSQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY,
                                        DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                            }
                            // the "unique" flag should be not set here because these answers haven't been proven unique
                            // yet this means the record will not exactly match the announcement record
                            out.addAuthorativeAnswer(new DNSRecord.Service(info.getQualifiedName(),
                                    DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE,
                                    DNSConstants.DNS_TTL, info.getPriority(), info.getWeight(), info.getPort(),
                                    this._jmDNSImpl.getLocalHost().getName()));
                        }
                    }
                }
                if (out != null)
                {
                    logger.finer("run() JmDNS probing #" + taskState);
                    this._jmDNSImpl.send(out);
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
                this._jmDNSImpl.recover();
            }

            taskState = taskState.advance();
            if (!taskState.isProbing())
            {
                cancel();

                this._jmDNSImpl.startAnnouncer();
            }
        }
    }

}