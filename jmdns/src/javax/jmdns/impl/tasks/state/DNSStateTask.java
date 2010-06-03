/**
 *
 */
package javax.jmdns.impl.tasks.state;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 *
 */
public abstract class DNSStateTask extends DNSTask
{

    /**
     * @param jmDNSImpl
     */
    public DNSStateTask(JmDNSImpl jmDNSImpl)
    {
        super(jmDNSImpl);
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
            this._jmDNSImpl.associateWithTask(this, state);
        }
        for (ServiceInfo serviceInfo : this._jmDNSImpl.getServices().values())
        {
            ((ServiceInfoImpl) serviceInfo).associateWithTask(this, state);
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
            this._jmDNSImpl.removeAssociationWithTask(this);
        }

        // Remove associations from services to this
        for (ServiceInfo serviceInfo : this._jmDNSImpl.getServices().values())
        {
            ((ServiceInfoImpl) serviceInfo).removeAssociationWithTask(this);
        }
    }

}
