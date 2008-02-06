//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSRecord.Pointer;
import javax.jmdns.impl.DNSRecord.Service;
import javax.jmdns.impl.DNSRecord.Text;

/**
 * JmDNS service information.
 *
 * @version %I%, %G%
 * @author	Arthur van Hoff, Jeff Sonstein, Werner Randelshofer
 */
public class ServiceInfoImpl extends ServiceInfo implements DNSListener
{
    private static Logger logger = Logger.getLogger(ServiceInfoImpl.class.getName());
    JmDNSImpl dns;
    
    // State machine
    /**
     * The state of this service info.
     * This is used only for services announced by JmDNS.
     * <p/>
     * For proper handling of concurrency, this variable must be
     * changed only using methods advanceState(), revertState() and cancel().
     */
    private DNSState state = DNSState.PROBING_1;

    /**
     * Task associated to this service info.
     * Possible tasks are JmDNS.Prober, JmDNS.Announcer, JmDNS.Responder,
     * JmDNS.Canceler.
     */
    TimerTask task;

    String type;
    private String name;
    String server;
    int port;
    int weight;
    int priority;
    byte text[];
    Hashtable props;
    InetAddress addr;

    /**
     * @see javax.jmdns.ServiceInfo#create(String, String, int, String)
     */
    public ServiceInfoImpl(String type, String name, int port, String text)
    {
        this(type, name, port, 0, 0, text);
    }
    
    /**
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, String)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, String text)
    {
        this(type, name, port, weight, priority, (byte[]) null);
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            writeUTF(out, text);
            byte [] data = out.toByteArray();
            this.text = new byte[data.length + 1];
            this.text[0] = (byte) data.length;
            System.arraycopy(data, 0, this.text, 1, data.length);
        }
        catch (IOException e)
        {
            throw new RuntimeException("unexpected exception: " + e);
        }
    }

    /**
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, Hashtable)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, Hashtable props)
    {
        this(type, name, port, weight, priority, new byte[0]);
        if (props != null)
        {
            try
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                for (Enumeration e = props.keys(); e.hasMoreElements();)
                {
                    String key = (String) e.nextElement();
                    Object val = props.get(key);
                    ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
                    writeUTF(out2, key);
                    if (val instanceof String)
                    {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    }
                    else
                    {
                        if (val instanceof byte[])
                        {
                            out2.write('=');
                            byte[] bval = (byte[]) val;
                            out2.write(bval, 0, bval.length);
                        }
                        else
                        {
                            if (val != NO_VALUE)
                            {
                                throw new IllegalArgumentException("invalid property value: " + val);
                            }
                        }
                    }
                    byte data[] = out2.toByteArray();
                    out.write(data.length);
                    out.write(data, 0, data.length);
                }
                this.text = out.toByteArray();
            }
            catch (IOException e)
            {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
    }

    /**
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, byte[])
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, byte text[])
    {
        this.type = type;
        this.name = name;
        this.port = port;
        this.weight = weight;
        this.priority = priority;
        this.text = text;
    }

    /**
     * Construct a service record during service discovery.
     */
    ServiceInfoImpl(String type, String name)
    {
        if (!type.endsWith("."))
        {
            throw new IllegalArgumentException("type must be fully qualified DNS name ending in '.': " + type);
        }

        this.type = type;
        this.name = name;
    }

    /**
     * During recovery we need to duplicate service info to reregister them
     */
    ServiceInfoImpl(ServiceInfoImpl info)
    {
        if (info != null)
        {
            this.type = info.type;
            this.name = info.name;
            this.port = info.port;
            this.weight = info.weight;
            this.priority = info.priority;
            this.text = info.text;
        }
    }

    /**
     * @see javax.jmdns.ServiceInfo#getType()
     */
    public String getType()
    {
        return type;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the service instance name.
     *
     * @param name unqualified service instance name, such as <code>foobar</code>
     */
    void setName(String name)
    {
        this.name = name;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getQualifiedName()
     */
    public String getQualifiedName()
    {
        return name + "." + type;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getServer()
     */
    public String getServer()
    {
        return server;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getHostAddress()
     */
    public String getHostAddress()
    {
        return (addr != null ? addr.getHostAddress() : "");
    }

    public InetAddress getAddress()
    {
        return addr;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getInetAddress()
     */
    public InetAddress getInetAddress()
    {
        return addr;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPort()
     */
    public int getPort()
    {
        return port;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPriority()
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getWeight()
     */
    public int getWeight()
    {
        return weight;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getTextBytes()
     */
    public byte[] getTextBytes()
    {
        return text;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getTextString()
     */
    public String getTextString()
    {
        if ((text == null) || (text.length == 0) || ((text.length == 1) && (text[0] == 0)))
        {
            return null;
        }
        return readUTF(text, 0, text.length);
    }

    /**
     * @see javax.jmdns.ServiceInfo#getURL()
     */
    public String getURL()
    {
        return getURL("http");
    }

    /**
     * @see javax.jmdns.ServiceInfo#getURL(java.lang.String)
     */
    public String getURL(String protocol)
    {
        String url = protocol + "://" + getHostAddress() + ":" + getPort();
        String path = getPropertyString("path");
        if (path != null)
        {
            if (path.indexOf("://") >= 0)
            {
                url = path;
            }
            else
            {
                url += path.startsWith("/") ? path : "/" + path;
            }
        }
        return url;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPropertyBytes(java.lang.String)
     */
    public synchronized byte[] getPropertyBytes(String name)
    {
        return (byte[]) getProperties().get(name);
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPropertyString(java.lang.String)
     */
    public synchronized String getPropertyString(String name)
    {
        byte data[] = (byte[]) getProperties().get(name);
        if (data == null)
        {
            return null;
        }
        if (data == NO_VALUE)
        {
            return "true";
        }
        return readUTF(data, 0, data.length);
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPropertyNames()
     */
    public Enumeration getPropertyNames()
    {
        Hashtable props = getProperties();
        return (props != null) ? props.keys() : new Vector().elements();
    }

    /**
     * Write a UTF string with a length to a stream.
     */
    void writeUTF(OutputStream out, String str) throws IOException
    {
        for (int i = 0, len = str.length(); i < len; i++)
        {
            int c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F))
            {
                out.write(c);
            }
            else
            {
                if (c > 0x07FF)
                {
                    out.write(0xE0 | ((c >> 12) & 0x0F));
                    out.write(0x80 | ((c >> 6) & 0x3F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
                else
                {
                    out.write(0xC0 | ((c >> 6) & 0x1F));
                    out.write(0x80 | ((c >> 0) & 0x3F));
                }
            }
        }
    }

    /**
     * Read data bytes as a UTF stream.
     */
    String readUTF(byte data[], int off, int len)
    {
        StringBuffer buf = new StringBuffer();
        for (int end = off + len; off < end;)
        {
            int ch = data[off++] & 0xFF;
            switch (ch >> 4)
            {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    break;
                case 12:
                case 13:
                    if (off >= len)
                    {
                        return null;
                    }
                    // 110x xxxx   10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (data[off++] & 0x3F);
                    break;
                case 14:
                    if (off + 2 >= len)
                    {
                        return null;
                    }
                    // 1110 xxxx  10xx xxxx  10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((data[off++] & 0x3F) << 6) | (data[off++] & 0x3F);
                    break;
                default:
                    if (off + 1 >= len)
                    {
                        return null;
                    }
                    // 10xx xxxx,  1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (data[off++] & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
        return buf.toString();
    }

    synchronized Hashtable getProperties()
    {
        if ((props == null) && (text != null))
        {
            Hashtable props = new Hashtable();
            int off = 0;
            while (off < text.length)
            {
                // length of the next key value pair
                int len = text[off++] & 0xFF;
                if ((len == 0) || (off + len > text.length))
                {
                    props.clear();
                    break;
                }
                // look for the '='
                int i = 0;
                for (; (i < len) && (text[off + i] != '='); i++)
                {
                    ;
                }

                // get the property name
                String name = readUTF(text, off, i);
                if (name == null)
                {
                    props.clear();
                    break;
                }
                if (i == len)
                {
                    props.put(name, NO_VALUE);
                }
                else
                {
                    byte value[] = new byte[len - ++i];
                    System.arraycopy(text, off + i, value, 0, len - i);
                    props.put(name, value);
                    off += len;
                }
            }
            this.props = props;
        }
        return props;
    }
    
    /**
     * JmDNS callback to update a DNS record.
     */
    public void updateRecord(JmDNSImpl jmdns, long now, DNSRecord rec)
    {
        if ((rec != null) && !rec.isExpired(now))
        {
            switch (rec.type)
            {
                case DNSConstants.TYPE_A:		// IPv4
                case DNSConstants.TYPE_AAAA:	// IPv6 FIXME [PJYF Oct 14 2004] This has not been tested
                    if (rec.name.equals(server))
                    {
                        addr = ((DNSRecord.Address) rec).getAddress();

                    }
                    break;
                case DNSConstants.TYPE_SRV:
                    if (rec.name.equals(getQualifiedName()))
                    {
                        DNSRecord.Service srv = (DNSRecord.Service) rec;
                        server = srv.server;
                        port = srv.port;
                        weight = srv.weight;
                        priority = srv.priority;
                        addr = null;
                        // changed to use getCache() instead - jeffs
                        // updateRecord(jmdns, now, (DNSRecord)jmdns.cache.get(server, TYPE_A, CLASS_IN));
                        updateRecord(jmdns, now, (DNSRecord) jmdns.getCache().get(server, DNSConstants.TYPE_A, DNSConstants.CLASS_IN));
                    }
                    break;
                case DNSConstants.TYPE_TXT:
                    if (rec.name.equals(getQualifiedName()))
                    {
                        DNSRecord.Text txt = (DNSRecord.Text) rec;
                        text = txt.text;
                    }
                    break;
            }
            // Future Design Pattern
            // This is done, to notify the wait loop in method
            // JmDNS.getServiceInfo(type, name, timeout);
            if (hasData() && dns != null)
            {
                dns.handleServiceResolved(this);
                dns = null;
            }
            synchronized (this)
            {
                notifyAll();
            }
        }
    }

    /**
     * Returns true if the service info is filled with data.
     */
    boolean hasData()
    {
        return server != null && addr != null && text != null;
    }
    
    
    // State machine
    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void advanceState()
    {
        state = state.advance();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void revertState()
    {
        state = state.revert();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void cancel()
    {
        state = DNSState.CANCELED;
        notifyAll();
    }

    /**
     * Returns the current state of this info.
     */
    DNSState getState()
    {
        return state;
    }


    public int hashCode()
    {
        return getQualifiedName().hashCode();
    }

    public boolean equals(Object obj)
    {
        return (obj instanceof ServiceInfoImpl) && getQualifiedName().equals(((ServiceInfoImpl) obj).getQualifiedName());
    }

    public String getNiceTextString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0, len = text.length; i < len; i++)
        {
            if (i >= 20)
            {
                buf.append("...");
                break;
            }
            int ch = text[i] & 0xFF;
            if ((ch < ' ') || (ch > 127))
            {
                buf.append("\\0");
                buf.append(Integer.toString(ch, 8));
            }
            else
            {
                buf.append((char) ch);
            }
        }
        return buf.toString();
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("service[");
        buf.append(getQualifiedName());
        buf.append(',');
        buf.append(getAddress());
        buf.append(':');
        buf.append(port);
        buf.append(',');
        buf.append(getNiceTextString());
        buf.append(']');
        return buf.toString();
    }

	void addAnswers(DNSOutgoing out, int ttl, HostInfo localHost) throws IOException
    {
        out.addAnswer(new Pointer(type, DNSConstants.TYPE_PTR, DNSConstants.CLASS_IN, ttl,
                getQualifiedName()), 0);
        out.addAnswer(new Service(getQualifiedName(), DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN|DNSConstants.CLASS_UNIQUE,
                ttl, priority, weight, port, localHost.getName()), 0);
        out.addAnswer(new Text(getQualifiedName(), DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN|DNSConstants.CLASS_UNIQUE,
                ttl, text), 0);
    }
}
