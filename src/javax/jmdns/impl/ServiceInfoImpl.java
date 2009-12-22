//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimerTask;
import java.util.Vector;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSRecord.Pointer;
import javax.jmdns.impl.DNSRecord.Service;
import javax.jmdns.impl.DNSRecord.Text;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;

/**
 * JmDNS service information.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer
 */
public class ServiceInfoImpl extends ServiceInfo implements DNSListener
{
    // private static Logger logger = Logger.getLogger(ServiceInfoImpl.class.getName());
    private JmDNSImpl _dns;

    // State machine
    /**
     * The state of this service info. This is used only for services announced by JmDNS.
     * <p/>
     * For proper handling of concurrency, this variable must be changed only using methods advanceState(),
     * revertState() and cancel().
     */
    private volatile DNSState _state = DNSState.PROBING_1;

    /**
     * Task associated to this service info. Possible tasks are JmDNS.Prober, JmDNS.Announcer, JmDNS.Responder,
     * JmDNS.Canceler.
     */
    private TimerTask _task;

    private String _type;
    private String _name;
    private String _server;
    private int _port;
    private int _weight;
    private int _priority;
    private byte _text[];
    private Map<String, Object> _props;
    private InetAddress _addr;

    /**
     * @param type
     * @param name
     * @param port
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, String)
     */
    public ServiceInfoImpl(String type, String name, int port, String text)
    {
        this(type, name, port, 0, 0, text);
    }

    /**
     * @param type
     * @param name
     * @param port
     * @param weight
     * @param priority
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, String)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, String text)
    {
        this(type, name, port, weight, priority, (byte[]) null);
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            writeUTF(out, text);
            byte[] data = out.toByteArray();
            this.setText(new byte[data.length + 1]);
            this.getText()[0] = (byte) data.length;
            System.arraycopy(data, 0, this.getText(), 1, data.length);
        }
        catch (IOException e)
        {
            throw new RuntimeException("unexpected exception: " + e);
        }
    }

    /**
     * @param type
     * @param name
     * @param port
     * @param weight
     * @param priority
     * @param props
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, Map)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, Map<String, ?> props)
    {
        this(type, name, port, weight, priority, new byte[0]);
        if (props != null)
        {
            try
            {
                ByteArrayOutputStream out = new ByteArrayOutputStream(256);
                for (String key : props.keySet())
                {
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
                this.setText(out.toByteArray());
            }
            catch (IOException e)
            {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
    }

    /**
     * @param type
     * @param name
     * @param port
     * @param weight
     * @param priority
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, byte[])
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, byte text[])
    {
        this._type = type;
        this._name = name;
        this._port = port;
        this._weight = weight;
        this._priority = priority;
        this.setText(text);
    }

    /**
     * Construct a service record during service discovery.
     *
     * @param type
     * @param name
     */
    ServiceInfoImpl(String type, String name)
    {
        if (!type.endsWith("."))
        {
            throw new IllegalArgumentException("type must be fully qualified DNS name ending in '.': " + type);
        }

        this._type = type;
        this._name = name;
    }

    /**
     * During recovery we need to duplicate service info to reregister them
     */
    ServiceInfoImpl(ServiceInfo info)
    {
        if (info != null)
        {
            this._type = info.getType();
            this._name = info.getName();
            this._port = info.getPort();
            this._weight = info.getWeight();
            this._priority = info.getPriority();
            this.setText(info.getTextBytes());
        }
    }

    /**
     * @see javax.jmdns.ServiceInfo#getType()
     */
    @Override
    public String getType()
    {
        return _type;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getName()
     */
    @Override
    public String getName()
    {
        return _name;
    }

    /**
     * Sets the service instance name.
     *
     * @param name
     *            unqualified service instance name, such as <code>foobar</code>
     */
    void setName(String name)
    {
        this._name = name;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getQualifiedName()
     */
    @Override
    public String getQualifiedName()
    {
        return _name + "." + _type;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getServer()
     */
    @Override
    public String getServer()
    {
        return _server;
    }

    /**
     * @param server
     *            the server to set
     */
    void setServer(String server)
    {
        this._server = server;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getHostAddress()
     */
    @Override
    public String getHostAddress()
    {
        return (_addr != null ? _addr.getHostAddress() : "");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getAddress()
     */
    @Override
    public InetAddress getAddress()
    {
        return _addr;
    }

    /**
     * @param addr
     *            the addr to set
     */
    void setAddress(InetAddress addr)
    {
        this._addr = addr;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getInetAddress()
     */
    @Override
    public InetAddress getInetAddress()
    {
        return _addr;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPort()
     */
    @Override
    public int getPort()
    {
        return _port;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getPriority()
     */
    @Override
    public int getPriority()
    {
        return _priority;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getWeight()
     */
    @Override
    public int getWeight()
    {
        return _weight;
    }

    /**
     * @see javax.jmdns.ServiceInfo#getTextBytes()
     */
    @Override
    public byte[] getTextBytes()
    {
        return getText();
    }

    /**
     * @see javax.jmdns.ServiceInfo#getTextString()
     */
    @Override
    public String getTextString()
    {
        if ((getText() == null) || (getText().length == 0) || ((getText().length == 1) && (getText()[0] == 0)))
        {
            return null;
        }
        return readUTF(getText(), 0, getText().length);
    }

    /**
     * @see javax.jmdns.ServiceInfo#getURL()
     */
    @Override
    public String getURL()
    {
        return getURL("http");
    }

    /**
     * @see javax.jmdns.ServiceInfo#getURL(java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getPropertyBytes(java.lang.String)
     */
    @Override
    public synchronized byte[] getPropertyBytes(String name)
    {
        return (byte[]) getProperties().get(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getPropertyString(java.lang.String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getPropertyNames()
     */
    @Override
    public Enumeration<String> getPropertyNames()
    {
        Map<String, Object> properties = getProperties();
        Collection<String> names = (properties != null ? properties.keySet() : Collections.<String> emptySet());
        return new Vector<String>(names).elements();
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
        int offset = off;
        StringBuffer buf = new StringBuffer();
        for (int end = offset + len; offset < end;)
        {
            int ch = data[offset++] & 0xFF;
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
                    if (offset >= len)
                    {
                        return null;
                    }
                    // 110x xxxx 10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (data[offset++] & 0x3F);
                    break;
                case 14:
                    if (offset + 2 >= len)
                    {
                        return null;
                    }
                    // 1110 xxxx 10xx xxxx 10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((data[offset++] & 0x3F) << 6) | (data[offset++] & 0x3F);
                    break;
                default:
                    if (offset + 1 >= len)
                    {
                        return null;
                    }
                    // 10xx xxxx, 1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (data[offset++] & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
        return buf.toString();
    }

    synchronized Map<String, Object> getProperties()
    {
        if ((_props == null) && (getText() != null))
        {
            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            int off = 0;
            while (off < getText().length)
            {
                // length of the next key value pair
                int len = getText()[off++] & 0xFF;
                if ((len == 0) || (off + len > getText().length))
                {
                    properties.clear();
                    break;
                }
                // look for the '='
                int i = 0;
                for (; (i < len) && (getText()[off + i] != '='); i++)
                {
                    /* Stub */
                }

                // get the property name
                String name = readUTF(getText(), off, i);
                if (name == null)
                {
                    properties.clear();
                    break;
                }
                if (i == len)
                {
                    properties.put(name, NO_VALUE);
                }
                else
                {
                    byte value[] = new byte[len - ++i];
                    System.arraycopy(getText(), off + i, value, 0, len - i);
                    properties.put(name, value);
                    off += len;
                }
            }
            this._props = properties;
        }
        return _props;
    }

    /**
     * JmDNS callback to update a DNS record.
     *
     * @param dnsCache
     * @param now
     * @param rec
     */
    public void updateRecord(DNSCache dnsCache, long now, DNSEntry rec)
    {
        if ((rec != null) && !rec.isExpired(now))
        {
            switch (rec.getRecordType())
            {
                case TYPE_A: // IPv4
                case TYPE_AAAA: // IPv6 FIXME [PJYF Oct 14 2004]
                    // This has not been tested
                    if (rec._name.equals(_server))
                    {
                        _addr = ((DNSRecord.Address) rec).getAddress();

                    }
                    break;
                case TYPE_SRV:
                    if (rec._name.equals(getQualifiedName()))
                    {
                        DNSRecord.Service srv = (DNSRecord.Service) rec;
                        _server = srv._server;
                        _port = srv._port;
                        _weight = srv._weight;
                        _priority = srv._priority;
                        _addr = null;
                        // changed to use getCache() instead - jeffs
                        // updateRecord(jmdns, now,
                        // (DNSRecord)jmdns.cache.get(server, TYPE_A,
                        // CLASS_IN));
                        // updateRecord(jmdns, now, (DNSRecord) jmdns.getCache().get(_server, DNSConstants.TYPE_A,
                        // DNSConstants.CLASS_IN));
                        updateRecord(dnsCache, now, dnsCache.getDNSEntry(_server, DNSRecordType.TYPE_A,
                                DNSRecordClass.CLASS_IN));
                    }
                    break;
                case TYPE_TXT:
                    if (rec._name.equals(getQualifiedName()))
                    {
                        DNSRecord.Text txt = (DNSRecord.Text) rec;
                        setText(txt._text);
                    }
                    break;
                default:
                    break;
            }
            // Future Design Pattern
            // This is done, to notify the wait loop in method
            // JmDNS.getServiceInfo(type, name, timeout);
            if (hasData() && getDns() != null)
            {
                getDns().handleServiceResolved(this);
                setDns(null);
            }
            synchronized (this)
            {
                notifyAll();
            }
        }
    }

    // State machine
    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    public synchronized void advanceState()
    {
        _state = _state.advance();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void revertState()
    {
        _state = _state.revert();
        notifyAll();
    }

    /**
     * Sets the state and notifies all objects that wait on the ServiceInfo.
     */
    synchronized void cancel()
    {
        _state = DNSState.CANCELED;
        notifyAll();
    }

    /**
     * Returns the current state of this info.
     *
     * @return current state
     */
    public DNSState getState()
    {
        return _state;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return getQualifiedName().hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof ServiceInfoImpl)
                && getQualifiedName().equals(((ServiceInfoImpl) obj).getQualifiedName());
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getNiceTextString()
     */
    @Override
    public String getNiceTextString()
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0, len = getText().length; i < len; i++)
        {
            if (i >= 20)
            {
                buf.append("...");
                break;
            }
            int ch = getText()[i] & 0xFF;
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

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("service[");
        buf.append(getQualifiedName());
        buf.append(',');
        buf.append(getAddress());
        buf.append(':');
        buf.append(_port);
        buf.append(',');
        buf.append(getNiceTextString());
        buf.append(']');
        return buf.toString();
    }

    public void addAnswers(DNSOutgoing out, int ttl, HostInfo localHost) throws IOException
    {
        out.addAnswer(new Pointer(_type, DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE,
                ttl, getQualifiedName()), 0);
        out.addAnswer(new Service(getQualifiedName(), DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN,
                DNSRecordClass.UNIQUE, ttl, _priority, _weight, _port, localHost.getName()), 0);
        out.addAnswer(new Text(getQualifiedName(), DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN,
                DNSRecordClass.UNIQUE, ttl, getText()), 0);
    }

    public void setTask(TimerTask task)
    {
        this._task = task;
    }

    public TimerTask getTask()
    {
        return _task;
    }

    public void setText(byte[] text)
    {
        this._text = text;
    }

    public byte[] getText()
    {
        return _text;
    }

    public void setDns(JmDNSImpl dns)
    {
        this._dns = dns;
    }

    public JmDNSImpl getDns()
    {
        return _dns;
    }
}
