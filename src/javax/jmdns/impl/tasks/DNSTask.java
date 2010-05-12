/**
 *
 */
package javax.jmdns.impl.tasks;

import java.util.TimerTask;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSState;

/**
 *
 */
public abstract class DNSTask extends TimerTask
{

    /**
     *
     */
    protected final JmDNSImpl _jmDNSImpl;

    /**
     * @param jmDNSImpl
     */
    protected DNSTask(JmDNSImpl jmDNSImpl)
    {
        super();
        this._jmDNSImpl = jmDNSImpl;
    }

    /**
     * Associate the DNS host and the service infos with this task if not already associated and in the same state.
     *
     * @param state
     *            target state
     */
    protected void associate(DNSState state)
    {
        synchronized (_jmDNSImpl)
        {
            if ((this._jmDNSImpl.getTask() == null) && (this._jmDNSImpl.getState() == state))
            {
                this._jmDNSImpl.setTask(this);
            }
        }
        for (ServiceInfo serviceInfo : this._jmDNSImpl.getServices().values())
        {
            ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
            synchronized (info)
            {
                if ((info.getTask() == null) && (info.getState() == state))
                {
                    info.setTask(this);
                }
            }
        }
    }

    /**
     * Remove the DNS host and service info association with this task.
     */
    protected void removeAssociation()
    {
        // Remove association from host to this
        synchronized (_jmDNSImpl)
        {
            if (this._jmDNSImpl.getTask() == this)
            {
                this._jmDNSImpl.setTask(null);
            }
        }

        // Remove associations from services to this
        for (ServiceInfo serviceInfo : this._jmDNSImpl.getServices().values())
        {
            ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
            synchronized (info)
            {
                if (info.getTask() == this)
                {
                    info.setTask(null);
                }
            }
        }
    }

}
