///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.impl.JmDNSImpl;

/**
 * mDNS implementation in Java.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Rick Blair, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Scott Lewis, Scott Cytacki
 */
public abstract class JmDNS
{
    /**
     * The version of JmDNS.
     */
    public static String VERSION = "3.1.5";

    /**
     * Create an instance of JmDNS.
     *
     * @return jmDNS instance
     * @throws IOException
     */
    public static JmDNS create() throws IOException
    {
        return new JmDNSImpl(null, null);
    }

    /**
     * Create an instance of JmDNS and bind it to a specific network interface given its IP-address.
     *
     * @param addr
     *            IP address to bind to.
     * @return jmDNS instance
     * @throws IOException
     */
    public static JmDNS create(InetAddress addr) throws IOException
    {
        return new JmDNSImpl(addr, null);
    }

    /**
     * Create an instance of JmDNS.
     *
     * @param name
     *            name of the newly created JmDNS
     * @return jmDNS instance
     * @throws IOException
     */
    public static JmDNS create(String name) throws IOException
    {
        return new JmDNSImpl(null, name);
    }

    /**
     * Create an instance of JmDNS and bind it to a specific network interface given its IP-address.
     *
     * @param addr
     *            IP address to bind to.
     * @param name
     *            name of the newly created JmDNS
     * @return jmDNS instance
     * @throws IOException
     */
    public static JmDNS create(InetAddress addr, String name) throws IOException
    {
        return new JmDNSImpl(addr, name);
    }

    /**
     * Return the name of the JmDNS instance. This is an arbitrary string that is useful for distinguishing instances.
     *
     * @return name of the JmDNS
     */
    public abstract String getName();

    /**
     * Return the HostName associated with this JmDNS instance. Note: May not be the same as what started. The host name is subject to negotiation.
     *
     * @return Host name
     */
    public abstract String getHostName();

    /**
     * Return the address of the interface to which this instance of JmDNS is bound.
     *
     * @return Internet Address
     * @throws IOException
     */
    public abstract InetAddress getInterface() throws IOException;

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.
     *
     * @param type
     *            fully qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @return null if the service information cannot be obtained
     */
    public abstract ServiceInfo getServiceInfo(String type, String name);

    /**
     * Get service information. If the information is not cached, the method will block for the given timeout until updated information is received.
     * <p/>
     * Usage note: If you call this method from the AWT event dispatcher thread, use a small timeout, or you will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 5s.
     * @return null if the service information cannot be obtained
     */
    public abstract ServiceInfo getServiceInfo(String type, String name, int timeout);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     */
    public abstract void requestServiceInfo(String type, String name);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param timeout
     *            timeout in milliseconds
     */
    public abstract void requestServiceInfo(String type, String name, int timeout);

    /**
     * Listen for service types.
     *
     * @param listener
     *            listener for service types
     * @throws IOException
     */
    public abstract void addServiceTypeListener(ServiceTypeListener listener) throws IOException;

    /**
     * Remove listener for service types.
     *
     * @param listener
     *            listener for service types
     */
    public abstract void removeServiceTypeListener(ServiceTypeListener listener);

    /**
     * Listen for services of a given type. The type has to be a fully qualified type name such as <code>_http._tcp.local.</code>.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param listener
     *            listener for service updates
     */
    public abstract void addServiceListener(String type, ServiceListener listener);

    /**
     * Remove listener for services of a given type.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     * @param listener
     *            listener for service updates
     */
    public abstract void removeServiceListener(String type, ServiceListener listener);

    /**
     * Register a service. The service is registered for access by other jmdns clients. The name of the service may be changed to make it unique.
     *
     * @param info
     *            service info to register
     * @throws IOException
     */
    public abstract void registerService(ServiceInfo info) throws IOException;

    /**
     * Unregister a service. The service should have been registered.
     *
     * @param info
     *            service info to remove
     */
    public abstract void unregisterService(ServiceInfo info);

    /**
     * Unregister all services.
     */
    public abstract void unregisterAllServices();

    /**
     * Register a service type. If this service type was not already known, all service listeners will be notified of the new service type. Service types are automatically registered as they are discovered.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code>.
     */
    public abstract void registerServiceType(String type);

    /**
     * Close down jmdns. Release all resources and unregister all services.
     */
    public abstract void close();

    /**
     * List Services and serviceTypes. Debugging Only
     */
    public abstract void printServices();

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @return An array of service instance names.
     */
    public abstract ServiceInfo[] list(String type);

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 6s.
     * @return An array of service instance names.
     */
    public abstract ServiceInfo[] list(String type, int timeout);

}
