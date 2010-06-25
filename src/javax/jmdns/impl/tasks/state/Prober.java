//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks.state;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * The Prober sends three consecutive probes for all service infos that needs probing as well as for the host name. The state of each service info of the host name is advanced, when a probe has been sent for it. When the prober has run three times,
 * it launches an Announcer.
 * <p/>
 * If a conflict during probes occurs, the affected service infos (and affected host name) are taken away from the prober. This eventually causes the prober to cancel itself.
 */
public class Prober extends DNSStateTask
{
    static Logger logger = Logger.getLogger(Prober.class.getName());

    /**
     * The state of the prober.
     */
    DNSState taskState = DNSState.PROBING_1;

    public Prober(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl, defaultTTL());

        this.associate(DNSState.PROBING_1);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName()
    {
        return "Prober(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
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
        long now = System.currentTimeMillis();
        if (now - this.getDns().getLastThrottleIncrement() < DNSConstants.PROBE_THROTTLE_COUNT_INTERVAL)
        {
            this.getDns().setThrottle(this.getDns().getThrottle() + 1);
        }
        else
        {
            this.getDns().setThrottle(1);
        }
        this.getDns().setLastThrottleIncrement(now);

        if (this.getDns().isAnnounced() && this.getDns().getThrottle() < DNSConstants.PROBE_THROTTLE_COUNT)
        {
            timer.schedule(this, JmDNSImpl.getRandom().nextInt(1 + DNSConstants.PROBE_WAIT_INTERVAL), DNSConstants.PROBE_WAIT_INTERVAL);
        }
        else if (!this.getDns().isCanceling() && !this.getDns().isCanceled())
        {
            timer.schedule(this, DNSConstants.PROBE_CONFLICT_INTERVAL, DNSConstants.PROBE_CONFLICT_INTERVAL);
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
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
        try
        {
            if (this.getDns().isCanceling() || this.getDns().isCanceled())
            {
                this.cancel();
                return;
            }
            // send probes for JmDNS itself
            synchronized (this.getDns())
            {
                if (this.getDns().isAssociatedWithTask(this, taskState))
                {
                    out.addQuestion(DNSQuestion.newQuestion(this.getDns().getLocalHost().getName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                    for (DNSRecord answer : this.getDns().getLocalHost().answers(this.getTTL()))
                    {
                        out = this.addAuthorativeAnswer(out, answer);
                    }
                    this.getDns().advanceState(this);
                }
            }
            // send probes for services
            for (ServiceInfo serviceInfo : this.getDns().getServices().values())
            {
                ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;

                synchronized (info)
                {
                    if (info.isAssociatedWithTask(this, taskState))
                    {
                        logger.fine("run() JmDNS probing " + info.getQualifiedName());
                        out = this.addQuestion(out, DNSQuestion.newQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
                        // the "unique" flag should be not set here because these answers haven't been proven unique
                        // yet this means the record will not exactly match the announcement record
                        out = this.addAuthorativeAnswer(out, new DNSRecord.Service(info.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, this.getTTL(), info.getPriority(), info.getWeight(), info.getPort(), this.getDns()
                                .getLocalHost().getName()));
                        info.advanceState(this);
                    }
                }
            }
            if (!out.isEmpty())
            {
                logger.finer("run() JmDNS probing #" + taskState);
                this.getDns().send(out);
            }
            else
            {
                // If we have nothing to send, another timer taskState ahead of us has done the job for us. We can cancel.
                cancel();
                return;
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.getDns().recover();
        }

        taskState = taskState.advance();
        if (!taskState.isProbing())
        {
            cancel();

            this.getDns().startAnnouncer();
        }
    }

}