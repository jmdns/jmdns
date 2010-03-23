//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
package javax.jmdns;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Map;

import javax.jmdns.impl.ServiceInfoImpl;

/**
 *
 */
public abstract class ServiceInfo
{
    public final static byte[] NO_VALUE = new byte[0];

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type
     *            fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     * @param port
     *            the local port on which the service runs
     * @param text
     *            string describing the service
     * @return new service info
     */
    public static ServiceInfo create(String type, String name, int port, String text)
    {
        return new ServiceInfoImpl(type, name, port, text);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type
     *            fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     * @param port
     *            the local port on which the service runs
     * @param weight
     *            weight of the service
     * @param priority
     *            priority of the service
     * @param text
     *            string describing the service
     * @return new service info
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, String text)
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, text);
    }

    /**
     * Construct a service description for registrating with JmDNS. The properties hashtable must map property names to either Strings or byte arrays describing the property values.
     *
     * @param type
     *            fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     * @param port
     *            the local port on which the service runs
     * @param weight
     *            weight of the service
     * @param priority
     *            priority of the service
     * @param props
     *            properties describing the service
     * @return new service info
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, Map<String, ?> props)
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, props);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type
     *            fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     * @param port
     *            the local port on which the service runs
     * @param weight
     *            weight of the service
     * @param priority
     *            priority of the service
     * @param text
     *            bytes describing the service
     * @return new service info
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, byte text[])
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, text);
    }

    /**
     * Returns true if the service info is filled with data.
     *
     * @return <code>true</code> if the service info has data, <code>false</code> otherwise.
     */
    public abstract boolean hasData();

    /**
     * Fully qualified service type name, such as <code>_http._tcp.local.</code>
     *
     * @return service type name
     */
    public abstract String getType();

    /**
     * Unqualified service instance name, such as <code>foobar</code> .
     *
     * @return service name
     */
    public abstract String getName();

    /**
     * Fully qualified service name, such as <code>foobar._http._tcp.local.</code> .
     *
     * @return qualified service name
     */
    public abstract String getQualifiedName();

    /**
     * Get the name of the server.
     *
     * @return server name
     */
    public abstract String getServer();

    /**
     * Get the host address of the service (ie X.X.X.X).
     *
     * @return host IP address
     */
    public abstract String getHostAddress();

    /**
     * Get the host address of the service.
     *
     * @return host Internet address
     */
    public abstract InetAddress getAddress();

    /**
     * Get the InetAddress of the service.
     *
     * @return Internet address
     */
    public abstract InetAddress getInetAddress();

    /**
     * Get the port for the service.
     *
     * @return service port
     */
    public abstract int getPort();

    /**
     * Get the priority of the service.
     *
     * @return service priority
     */
    public abstract int getPriority();

    /**
     * Get the weight of the service.
     *
     * @return service weight
     */
    public abstract int getWeight();

    /**
     * Get the text for the service as raw bytes.
     *
     * @return raw service text
     */
    public abstract byte[] getTextBytes();

    /**
     * Get the text for the service. This will interpret the text bytes as a UTF8 encoded string. Will return null if the bytes are not a valid UTF8 encoded string.
     *
     * @return service text
     */
    public abstract String getTextString();

    /**
     * Get the URL for this service. An http URL is created by combining the address, port, and path properties.
     *
     * @return service URL
     */
    public abstract String getURL();

    /**
     * Get the URL for this service. An URL is created by combining the protocol, address, port, and path properties.
     *
     * @param protocol
     *            requested protocol
     * @return service URL
     */
    public abstract String getURL(String protocol);

    /**
     * Get a property of the service. This involves decoding the text bytes into a property list. Returns null if the property is not found or the text data could not be decoded correctly.
     *
     * @param name
     *            property name
     * @return raw property text
     */
    public abstract byte[] getPropertyBytes(String name);

    /**
     * Get a property of the service. This involves decoding the text bytes into a property list. Returns null if the property is not found, the text data could not be decoded correctly, or the resulting bytes are not a valid UTF8 string.
     *
     * @param name
     *            property name
     * @return property text
     */
    public abstract String getPropertyString(String name);

    /**
     * Enumeration of the property names.
     *
     * @return property name enumeration
     */
    public abstract Enumeration<String> getPropertyNames();

    /**
     * Returns a description of the service info suitable for printing.
     *
     * @return service info description
     */
    public abstract String getNiceTextString();

}
