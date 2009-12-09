/**
 *
 */
package javax.jmdns.impl.tasks;

import java.util.TimerTask;

import javax.jmdns.impl.JmDNSImpl;

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
    public DNSTask(JmDNSImpl jmDNSImpl)
    {
        super();
        this._jmDNSImpl = jmDNSImpl;
    }

}
