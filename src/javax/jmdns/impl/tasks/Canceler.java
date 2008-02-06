//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.DNSConstants;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;

/**
 * The Canceler sends two announces with TTL=0 for the specified services.
 */
public class Canceler extends TimerTask
{
    static Logger logger = Logger.getLogger(Canceler.class.getName());

    /**
     * 
     */
    private final JmDNSImpl jmDNSImpl;
    /**
     * Counts the number of announces being sent.
     */
    int count = 0;
    /**
     * The services that need cancelling.
     * Note: We have to use a local variable here, because the services
     * that are canceled, are removed immediately from variable JmDNS.services.
     */
    private ServiceInfoImpl[] infos;
    /**
     * We call notifyAll() on the lock object, when we have canceled the
     * service infos.
     * This is used by method JmDNS.unregisterService() and
     * JmDNS.unregisterAllServices, to ensure that the JmDNS
     * socket stays open until the Canceler has canceled all services.
     * <p/>
     * Note: We need this lock, because ServiceInfos do the transition from
     * state ANNOUNCED to state CANCELED before we get here. We could get
     * rid of this lock, if we added a state named CANCELLING to DNSState.
     */
    private Object lock;
    int ttl = 0;

    public Canceler(JmDNSImpl jmDNSImpl, ServiceInfoImpl info, Object lock)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.infos = new ServiceInfoImpl[]{info};
        this.lock = lock;
        this.jmDNSImpl.addListener(info, new DNSQuestion(info.getQualifiedName(), DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));
    }

    public Canceler(JmDNSImpl jmDNSImpl, ServiceInfoImpl[] infos, Object lock)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.infos = infos;
        this.lock = lock;
    }

    public Canceler(JmDNSImpl jmDNSImpl, Collection infos, Object lock)
    {
        this.jmDNSImpl = jmDNSImpl;
        this.infos = (ServiceInfoImpl[]) infos.toArray(new ServiceInfoImpl[infos.size()]);
        this.lock = lock;
    }

    public void start(Timer timer)
    {
        timer.schedule(this, 0, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
    }

    public void run()
    {
        try
        {
            if (++count < 3)
            {
                logger.finer("run() JmDNS canceling service");
                // announce the service
                //long now = System.currentTimeMillis();
                DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                for (int i = 0; i < infos.length; i++)
                {
                    ServiceInfoImpl info = infos[i];
                    info.addAnswers(out, ttl, this.jmDNSImpl.getLocalHost());

                    this.jmDNSImpl.getLocalHost().addAddressRecords(out, false);
                }
                this.jmDNSImpl.send(out);
            }
            else
            {
                // After three successful announcements, we are finished.
                synchronized (lock)
                {
                    this.jmDNSImpl.setClosed(true);
                    lock.notifyAll();
                }
                this.cancel();
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this.jmDNSImpl.recover();
        }
    }
}