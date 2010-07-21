/**
 *
 */
package javax.jmdns.impl.tasks.state;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.ServiceInfoImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 *
 */
public abstract class DNSStateTask extends DNSTask
{

    /**
     * By setting a 0 ttl we effectively expire the record.
     */
    private final int _ttl;

    private static int _defaultTTL = DNSConstants.DNS_TTL;

    public static int defaultTTL()
    {
        return _defaultTTL;
    }

    /**
     * For testing only do not use in production.
     *
     * @param value
     */
    public static void setDefaultTTL(int value)
    {
        _defaultTTL = value;
    }

    /**
     * @param jmDNSImpl
     * @param ttl
     */
    public DNSStateTask(JmDNSImpl jmDNSImpl, int ttl)
    {
        super(jmDNSImpl);
        _ttl = ttl;
    }

    /**
     * @return the ttl
     */
    public int getTTL()
    {
        return _ttl;
    }

    /**
     * Associate the DNS host and the service infos with this task if not already associated and in the same state.
     *
     * @param state
     *            target state
     */
    protected void associate(DNSState state)
    {
        synchronized (this.getDns())
        {
            this.getDns().associateWithTask(this, state);
        }
        for (ServiceInfo serviceInfo : this.getDns().getServices().values())
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
        synchronized (this.getDns())
        {
            this.getDns().removeAssociationWithTask(this);
        }

        // Remove associations from services to this
        for (ServiceInfo serviceInfo : this.getDns().getServices().values())
        {
            ((ServiceInfoImpl) serviceInfo).removeAssociationWithTask(this);
        }
    }

}
