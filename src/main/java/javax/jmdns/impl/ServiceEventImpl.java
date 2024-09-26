// /Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import java.io.Serial;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

/**
 * ServiceEvent.
 *
 * @author Werner Randelshofer, Rick Blair
 */
public class ServiceEventImpl extends ServiceEvent {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 7107973622016897488L;
    // private static Logger logger = LoggerFactory.getLogger(ServiceEvent.class);
    /**
     * The type name of the service.
     */
    private final String      _type;
    /**
     * The instance name of the service. Or null, if the event was fired to a service type listener.
     */
    private final String      _name;
    /**
     * The service info record, or null if the service could be resolved. This is also null, if the event was fired to a service type listener.
     */
    private final ServiceInfo _info;

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
     *            the service info record, or null if the service could be resolved.
     */
    public ServiceEventImpl(JmDNSImpl jmDNS, String type, String name, ServiceInfo info) {
        super(jmDNS);
        this._type = type;
        this._name = name;
        this._info = info;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceEvent#getDNS()
     */
    @Override
    public JmDNS getDNS() {
        return (JmDNS) getSource();
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceEvent#getType()
     */
    @Override
    public String getType() {
        return _type;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceEvent#getName()
     */
    @Override
    public String getName() {
        return _name;
    }

    /*
     * (non-Javadoc)
     * @see java.util.EventObject#toString()
     */
    @Override
    public String toString() {
        return '[' +
                this.getClass().getSimpleName() + '@' + System.identityHashCode(this) +
                "\n\tname: '" + this.getName() +
                "' type: '" + this.getType() +
                "' info: '" + this.getInfo() +
                "']";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceEvent#getInfo()
     */
    @Override
    public ServiceInfo getInfo() {
        return _info;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.ServiceEvent#clone()
     */
    @Override
    public ServiceEventImpl clone() {
        ServiceInfoImpl newInfo = new ServiceInfoImpl(this.getInfo());
        return new ServiceEventImpl((JmDNSImpl) this.getDNS(), this.getType(), this.getName(), newInfo);
    }

}