//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns;

import java.util.EventObject;

public abstract class ServiceEvent extends EventObject
{

    public ServiceEvent(Object source)
    {
        super(source);
        // TODO Auto-generated constructor stub
    }

    /**
     * Returns the JmDNS instance which originated the event.
     */
    public abstract JmDNS getDNS();

    /**
     * Returns the fully qualified type of the service.
     */
    public abstract String getType();

    /**
     * Returns the instance name of the service.
     * Always returns null, if the event is sent to a service type listener.
     */
    public abstract String getName();

    /**
     * Returns the service info record, or null if the service could not be
     * resolved.
     * Always returns null, if the event is sent to a service type listener.
     */
    /**
     * @see javax.jmdns.ServiceEvent#getInfo()
     */
    public abstract ServiceInfo getInfo();
}