// /Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

import javax.jmdns.impl.JmDNSImpl;

/**
 * mDNS implementation in Java.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Rick Blair, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Scott Lewis, Scott Cytacki
 */
public abstract class JmDNS implements Closeable {
    /**
     * The version of JmDNS.
     */
    public static final String VERSION = "3.2.3";

    /**
     * Create an instance of JmDNS.<br/>
     * <b>Note:</b> This is a convenience method. The preferred constructor is {@link #create(InetAddress, String)}.<br/>
     * Check that your platform correctly handle the default localhost IP address and the local hostname. In doubt use the explicit constructor.<br/>
     * This call is equivalent to <code>create(null, null)</code>.
     *
     * @see #create(InetAddress, String)
     * @return jmDNS instance
     * @throws IOException
     *             if an exception occurs during the socket creation
     */
    public static JmDNS create() throws IOException {
        return new JmDNSImpl(null, null);
    }

    /**
     * Create an instance of JmDNS and bind it to a specific network interface given its IP-address.<br/>
     * <b>Note:</b> This is a convenience method. The preferred constructor is {@link #create(InetAddress, String)}.<br/>
     * Check that your platform correctly handle the default localhost IP address and the local hostname. In doubt use the explicit constructor.<br/>
     * This call is equivalent to <code>create(addr, null)</code>.
     *
     * @see #create(InetAddress, String)
     * @param addr
     *            IP address to bind to.
     * @return jmDNS instance
     * @throws IOException
     *             if an exception occurs during the socket creation
     */
    public static JmDNS create(final InetAddress addr) throws IOException {
        return new JmDNSImpl(addr, null);
    }

    /**
     * Create an instance of JmDNS.<br/>
     * <b>Note:</b> This is a convenience method. The preferred constructor is {@link #create(InetAddress, String)}.<br/>
     * Check that your platform correctly handle the default localhost IP address and the local hostname. In doubt use the explicit constructor.<br/>
     * This call is equivalent to <code>create(null, name)</code>.
     *
     * @see #create(InetAddress, String)
     * @param name
     *            name of the newly created JmDNS
     * @return jmDNS instance
     * @throws IOException
     *             if an exception occurs during the socket creation
     */
    public static JmDNS create(final String name) throws IOException {
        return new JmDNSImpl(null, name);
    }

    /**
     * Create an instance of JmDNS and bind it to a specific network interface given its IP-address.<br/>
     * If <code>addr</code> parameter is null this method will try to resolve to a local IP address of the machine using a network discovery:
     * <ol>
     * <li>Check the system property <code>net.mdns.interface</code></li>
     * <li>Check the JVM local host</li>
     * <li>Use the {@link JmmDNS.NetworkTopologyDiscovery} to find a valid network interface and IP.</li>
     * <li>In the last resort bind to the loopback address. This is non functional in most cases.</li>
     * </ol>
     * If <code>name</code> parameter is null this method will try to resolve hostname following these steps:
     * <ol>
     * <li>Get the hostname from the InetAdress obtained before.</li>
     * <li>If the hostname is a reverse lookup default to <code>computer</code></li>
     * <li>If the name contains <code>'.'</code> replace them by <code>'-'</code></li>
     * <li>Add <code>.local.</code> at the end of the name.</li>
     * </ol>
     * <p>
     * <b>Note:</b> If you need to use a custom {@link JmmDNS.NetworkTopologyDiscovery} it must be setup before any call to this method. This is done by setting up a {@link JmmDNS.NetworkTopologyDiscovery.Factory.ClassDelegate} and installing it
     * using {@link JmmDNS.NetworkTopologyDiscovery.Factory#setClassDelegate(JmmDNS.NetworkTopologyDiscovery.Factory.ClassDelegate)}. This must be done before creating a {@link JmDNS} or {@link JmmDNS} instance.
     * </p>
     *
     * @param addr
     *            IP address to bind to.
     * @param name
     *            name of the newly created JmDNS
     * @return jmDNS instance
     * @throws IOException
     *             if an exception occurs during the socket creation
     */
    public static JmDNS create(final InetAddress addr, final String name) throws IOException {
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
    public abstract ServiceInfo getServiceInfo(String type, String name, long timeout);

    /**
     * Get service information. If the information is not cached, the method will block until updated information is received.
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.
     *
     * @param type
     *            fully qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new new information is received.
     * @return null if the service information cannot be obtained
     */
    public abstract ServiceInfo getServiceInfo(String type, String name, boolean persistent);

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
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new new information is received.
     * @return null if the service information cannot be obtained
     */
    public abstract ServiceInfo getServiceInfo(String type, String name, boolean persistent, long timeout);

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
     * <p/>
     * Usage note: Do not call this method from the AWT event dispatcher thread. You will make the user interface unresponsive.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new new information is received.
     */
    public abstract void requestServiceInfo(String type, String name, boolean persistent);

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
    public abstract void requestServiceInfo(String type, String name, long timeout);

    /**
     * Request service information. The information about the service is requested and the ServiceListener.resolveService method is called as soon as it is available.
     *
     * @param type
     *            full qualified service type, such as <code>_http._tcp.local.</code> .
     * @param name
     *            unqualified service name, such as <code>foobar</code> .
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new new information is received.
     * @param timeout
     *            timeout in milliseconds
     */
    public abstract void requestServiceInfo(String type, String name, boolean persistent, long timeout);

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
     * Register a service. The service is registered for access by other jmdns clients. The name of the service may be changed to make it unique.<br>
     * Note that the given {@code ServiceInfo} is bound to this {@code JmDNS} instance, and should not be reused for any other {@linkplain #registerService(ServiceInfo)}.
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
     * @return <code>true</code> if the type or subtype was added, <code>false</code> if the type was already registered.
     */
    public abstract boolean registerServiceType(String type);

    /**
     * List Services and serviceTypes. Debugging Only
     *
     * @deprecated since 3.2.2
     */
    @Deprecated
    public abstract void printServices();

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @return An array of service instance.
     */
    public abstract ServiceInfo[] list(String type);

    /**
     * Returns a list of service infos of the specified type.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 6s.
     * @return An array of service instance.
     */
    public abstract ServiceInfo[] list(String type, long timeout);

    /**
     * Returns a list of service infos of the specified type sorted by subtype. Any service that do not register a subtype is listed in the empty subtype section.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @return A dictionary of service info by subtypes.
     */
    public abstract Map<String, ServiceInfo[]> listBySubtype(String type);

    /**
     * Returns a list of service infos of the specified type sorted by subtype. Any service that do not register a subtype is listed in the empty subtype section.
     *
     * @param type
     *            Service type name, such as <code>_http._tcp.local.</code>.
     * @param timeout
     *            timeout in milliseconds. Typical timeout should be 6s.
     * @return A dictionary of service info by subtypes.
     */
    public abstract Map<String, ServiceInfo[]> listBySubtype(String type, long timeout);

}
