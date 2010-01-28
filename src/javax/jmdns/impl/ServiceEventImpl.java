///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

/**
 * ServiceEvent.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version %I%, %G%
 */
public class ServiceEventImpl extends ServiceEvent
{
    /**
     *
     */
    private static final long serialVersionUID = 7107973622016897488L;
    // private static Logger logger = Logger.getLogger(ServiceEvent.class.getName());
    /**
     * The type name of the service.
     */
    private String _type;
    /**
     * The instance name of the service. Or null, if the event was fired to a service type listener.
     */
    private String _name;
    /**
     * The service info record, or null if the service could be be resolved. This is also null, if the event was fired
     * to a service type listener.
     */
    private ServiceInfo _info;

    /**
     * Creates a new instance.
     *
     * @param jmDNS
     *            the JmDNS instance which originated the event.
     * @param type
     *            the type name of the service.
     * @param name
     *            the instance name of the service.
     * @param info
     *            the service info record, or null if the service could be be resolved.
     */
    public ServiceEventImpl(JmDNSImpl jmDNS, String type, String name, ServiceInfo info)
    {
        super(jmDNS);
        this._type = type;
        this._name = name;
        this._info = info;
    }

    /**
     * @see javax.jmdns.ServiceEvent#getDNS()
     */
    @Override
    public JmDNS getDNS()
    {
        return (JmDNS) getSource();
    }

    /**
     * @see javax.jmdns.ServiceEvent#getType()
     */
    @Override
    public String getType()
    {
        return _type;
    }

    /**
     * @see javax.jmdns.ServiceEvent#getName()
     */
    @Override
    public String getName()
    {
        return _name;
    }

    /**
     * @see javax.jmdns.ServiceEvent#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<" + getClass().getName() + "> ");
        buf.append(super.toString());
        buf.append(" name ");
        buf.append(getName());
        buf.append(" type ");
        buf.append(getType());
        buf.append(" info ");
        buf.append(getInfo());
        return buf.toString();
    }

    @Override
    public ServiceInfo getInfo()
    {
        return _info;
    }

}
