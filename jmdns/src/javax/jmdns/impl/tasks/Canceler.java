//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl.tasks;

import java.util.Collection;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The Canceler sends two announces with TTL=0 for the specified services.
 */
public class Canceler extends DNSTask
{
    static Logger logger = Logger.getLogger(Canceler.class.getName());

    /**
     * Counts the number of announces being sent.
     */
    int _count = 0;
    /**
     * The services that need cancelling. Note: We have to use a local variable here, because the services that are canceled, are removed immediately from variable JmDNS.services.
     */
    private ServiceInfoImpl[] _infos;
    /**
     * We call notifyAll() on the lock object, when we have canceled the service infos. This is used by method JmDNS.unregisterService() and JmDNS.unregisterAllServices, to ensure that the JmDNS socket stays open until the Canceler has canceled all
     * services.
     * <p/>
     * Note: We need this lock, because ServiceInfos do the transition from state ANNOUNCED to state CANCELED before we get here. We could get rid of this lock, if we added a state named CANCELLING to DNSState.
     */
    private Object _lock;

    /**
     * By setting a 0 ttl we effectively expire the record.
     */
    private final int _ttl = 0;

    public Canceler(JmDNSImpl jmDNSImpl, ServiceInfoImpl info, Object lock)
    {
        super(jmDNSImpl);
        this._infos = new ServiceInfoImpl[] { info };
        this._lock = lock;
        this._jmDNSImpl.addListener(info, new DNSQuestion(info.getQualifiedName(), DNSRecordType.TYPE_ANY, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    public Canceler(JmDNSImpl jmDNSImpl, ServiceInfoImpl[] infos, Object lock)
    {
        super(jmDNSImpl);
        this._infos = infos;
        this._lock = lock;
    }

    public Canceler(JmDNSImpl jmDNSImpl, Collection<? extends ServiceInfo> infos, Object lock)
    {
        super(jmDNSImpl);
        this._infos = infos.toArray(new ServiceInfoImpl[infos.size()]);
        this._lock = lock;
    }

    public void start(Timer timer)
    {
        timer.schedule(this, 0, DNSConstants.ANNOUNCE_WAIT_INTERVAL);
    }

    @Override
    public void run()
    {
        try
        {
            if (++_count < 3)
            {
                logger.finer("run() JmDNS canceling service");
                // announce the service
                // long now = System.currentTimeMillis();
                DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA);
                for (int i = 0; i < _infos.length; i++)
                {
                    ServiceInfoImpl info = _infos[i];
                    info.addAnswers(out, _ttl, this._jmDNSImpl.getLocalHost());

                    this._jmDNSImpl.getLocalHost().addAddressRecords(out, false);
                }
                this._jmDNSImpl.send(out);
            }
            else
            {
                // After three successful announcements, we are finished.
                synchronized (_lock)
                {
                    this._jmDNSImpl.setClosed(true);
                    _lock.notifyAll();
                }
                this.cancel();
            }
        }
        catch (Throwable e)
        {
            logger.log(Level.WARNING, "run() exception ", e);
            this._jmDNSImpl.recover();
        }
    }
}