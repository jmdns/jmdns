//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
package javax.jmdns;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.jmdns.impl.ServiceInfoImpl;

public abstract class ServiceInfo
{
    public final static byte[] NO_VALUE = new byte[0];

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name unqualified service instance name, such as <code>foobar</code>
     * @param port the local port on which the service runs
     * @param text string describing the service
     */
    public static ServiceInfo create(String type, String name, int port, String text)
    {
        return new ServiceInfoImpl(type, name, port, text);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type     fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name, such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param text     string describing the service
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, String text)
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, text);
    }

    /**
     * Construct a service description for registrating with JmDNS. The properties hashtable must
     * map property names to either Strings or byte arrays describing the property values.
     *
     * @param type     fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name, such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param props    properties describing the service
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, Hashtable props)
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, props);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     *
     * @param type     fully qualified service type name, such as <code>_http._tcp.local.</code>.
     * @param name     unqualified service instance name, such as <code>foobar</code>
     * @param port     the local port on which the service runs
     * @param weight   weight of the service
     * @param priority priority of the service
     * @param text     bytes describing the service
     */
    public static ServiceInfo create(String type, String name, int port, int weight, int priority, byte text[])
    {
        return new ServiceInfoImpl(type, name, port, weight, priority, text);
    }

    /**
     * Fully qualified service type name, such as <code>_http._tcp.local.</code> .
     */
    public abstract String getType();

    /**
     * Unqualified service instance name, such as <code>foobar</code> .
     */
    public abstract String getName();

    /**
     * Fully qualified service name, such as <code>foobar._http._tcp.local.</code> .
     */
    public abstract String getQualifiedName();

    /**
     * Get the name of the server.
     */
    public abstract String getServer();

    /**
     * Get the host address of the service (ie X.X.X.X).
     */
    public abstract String getHostAddress();

    public abstract InetAddress getAddress();

    /**
     * Get the InetAddress of the service.
     */
    public abstract InetAddress getInetAddress();

    /**
     * Get the port for the service.
     */
    public abstract int getPort();

    /**
     * Get the priority of the service.
     */
    public abstract int getPriority();

    /**
     * Get the weight of the service.
     */
    public abstract int getWeight();

    /**
     * Get the text for the serivce as raw bytes.
     */
    public abstract byte[] getTextBytes();

    /**
     * Get the text for the service. This will interpret the text bytes
     * as a UTF8 encoded string. Will return null if the bytes are not
     * a valid UTF8 encoded string.
     */
    public abstract String getTextString();

    /**
     * Get the URL for this service. An http URL is created by
     * combining the address, port, and path properties.
     */
    public abstract String getURL();

    /**
     * Get the URL for this service. An URL is created by
     * combining the protocol, address, port, and path properties.
     */
    public abstract String getURL(String protocol);

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found or the text data could not be decoded correctly.
     */
    public abstract byte[] getPropertyBytes(String name);

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found, the text data could not be decoded correctly, or
     * the resulting bytes are not a valid UTF8 string.
     */
    public abstract String getPropertyString(String name);

    /**
     * Enumeration of the property names.
     */
    public abstract Enumeration getPropertyNames();

    public abstract String getNiceTextString();

}
