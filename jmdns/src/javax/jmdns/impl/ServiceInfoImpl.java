//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.DNSRecord.Pointer;
import javax.jmdns.impl.DNSRecord.Service;
import javax.jmdns.impl.DNSRecord.Text;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSState;
import javax.jmdns.impl.tasks.DNSTask;

/**
 * JmDNS service information.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer
 */
public class ServiceInfoImpl extends ServiceInfo implements DNSListener, Cloneable, DNSStatefulObject
{
    // private static Logger logger = Logger.getLogger(ServiceInfoImpl.class.getName());

    private String _type;
    private String _name;
    private String _server;
    private int _port;
    private int _weight;
    private int _priority;
    private byte _text[];
    private Map<String, byte[]> _props;
    private InetAddress _addr;

    private boolean _persistent;
    private boolean _needTextAnnouncing;

    private final ServiceInfoState _state;

    private final static class ServiceInfoState extends DNSStatefulObject.DefaultImplementation
    {

        private final ServiceInfoImpl _info;

        /**
         * @param info
         */
        public ServiceInfoState(ServiceInfoImpl info)
        {
            super();
            _info = info;
        }

        @Override
        protected void setTask(DNSTask task)
        {
            super.setTask(task);
            if ((this._task == null) && _info.needTextAnnouncing())
            {
                synchronized (this)
                {
                    if ((this._task == null) && _info.needTextAnnouncing())
                    {
                        if (this._state.isAnnounced())
                        {
                            this._state = DNSState.ANNOUNCING_1;
                            if (this.getDns() != null)
                            {
                                this.getDns().startAnnouncer();
                            }
                        }
                        else
                        {
                            _info.setNeedTextAnnouncing(false);
                        }
                    }
                }
            }
        }

        @Override
        public void setDns(JmDNSImpl dns)
        {
            super.setDns(dns);
        }

    }

    /**
     * @param type
     * @param name
     * @param port
     * @param weight
     * @param priority
     * @param persistent
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, String)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, boolean persistent, String text)
    {
        this(type, name, port, weight, priority, persistent, (byte[]) null);
        _server = text;
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
            writeUTF(out, text);
            this._text = out.toByteArray();
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
     * @param persistent
     * @param props
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, Map)
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, boolean persistent, Map<String, ?> props)
    {
        this(type, name, port, weight, priority, persistent, textFromProperties(props));
    }

    /**
     * @param type
     * @param name
     * @param port
     * @param weight
     * @param priority
     * @param persistent
     * @param text
     * @see javax.jmdns.ServiceInfo#create(String, String, int, int, int, byte[])
     */
    public ServiceInfoImpl(String type, String name, int port, int weight, int priority, boolean persistent, byte text[])
    {
        this._type = type;
        this._name = name;
        this._port = port;
        this._weight = weight;
        this._priority = priority;
        this._text = text;
        this.setNeedTextAnnouncing(false);
        this._state = new ServiceInfoState(this);
        this._persistent = persistent;
    }

    /**
     * During recovery we need to duplicate service info to reregister them
     *
     * @param info
     */
    public ServiceInfoImpl(ServiceInfo info)
    {
        if (info != null)
        {
            this._type = info.getType();
            this._name = info.getName();
            this._port = info.getPort();
            this._weight = info.getWeight();
            this._priority = info.getPriority();
            this._text = info.getTextBytes();
            this._persistent = info.isPersistent();
            this._addr = info.getAddress();
        }
        this._state = new ServiceInfoState(this);
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
        return (_name != null ? _name.toLowerCase() + "." : "") + (_type != null ? _type.toLowerCase() : "");
    }

    /**
     * @see javax.jmdns.ServiceInfo#getServer()
     */
    @Override
    public String getServer()
    {
        return (_server != null ? _server : "");
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

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getTextString()
     */
    @Deprecated
    @Override
    public String getTextString()
    {
        Map<String, byte[]> properties = this.getProperties();
        for (String key : properties.keySet())
        {
            byte[] value = properties.get(key);
            if ((value != null) && (value.length > 0))
            {
                return key + "=" + new String(value);
            }
            return key;
        }
        return "";
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
        return this.getProperties().get(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getPropertyString(java.lang.String)
     */
    @Override
    public synchronized String getPropertyString(String name)
    {
        byte data[] = this.getProperties().get(name);
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
        Map<String, byte[]> properties = getProperties();
        Collection<String> names = (properties != null ? properties.keySet() : Collections.<String> emptySet());
        return new Vector<String>(names).elements();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getDomain()
     */
    @Override
    public String getDomain()
    {
        String protocol = getProtocol();
        int start = _type.indexOf(protocol) + protocol.length() + 1;
        int end = _type.length() - 1;
        return _type.substring(start, end);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#getProtocol()
     */
    @Override
    public String getProtocol()
    {
        int start = _type.lastIndexOf("._") + 2;
        int end = _type.indexOf('.', start);
        return _type.substring(start, end);
    }

    /**
     * Write a UTF string with a length to a stream.
     */
    static void writeUTF(OutputStream out, String str) throws IOException
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

    synchronized Map<String, byte[]> getProperties()
    {
        if ((_props == null) && (getText() != null))
        {
            Hashtable<String, byte[]> properties = new Hashtable<String, byte[]>();
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
        return (_props != null ? _props : Collections.<String, byte[]> emptyMap());
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
        if ((rec instanceof DNSRecord) && !rec.isExpired(now))
        {
            boolean serviceUpdated = false;
            switch (rec.getRecordType())
            {
                case TYPE_A: // IPv4
                case TYPE_AAAA: // IPv6
                    if (rec.getName().equalsIgnoreCase(this.getServer()))
                    {
                        _addr = ((DNSRecord.Address) rec).getAddress();
                        serviceUpdated = true;
                    }
                    break;
                case TYPE_SRV:
                    if (rec.getName().equalsIgnoreCase(this.getQualifiedName()))
                    {
                        DNSRecord.Service srv = (DNSRecord.Service) rec;
                        boolean serverChanged = (_server == null) || !_server.equalsIgnoreCase(srv.getServer());
                        _server = srv.getServer();
                        _port = srv.getPort();
                        _weight = srv.getWeight();
                        _priority = srv.getPriority();
                        if (serverChanged)
                        {
                            _addr = null;
                            this.updateRecord(dnsCache, now, dnsCache.getDNSEntry(_server, DNSRecordType.TYPE_A, DNSRecordClass.CLASS_IN));
                            this.updateRecord(dnsCache, now, dnsCache.getDNSEntry(_server, DNSRecordType.TYPE_AAAA, DNSRecordClass.CLASS_IN));
                            // We do not want to trigger the listener in this case as it will be triggered if the
                        }
                        else
                        {
                            serviceUpdated = true;
                        }
                    }
                    break;
                case TYPE_TXT:
                    if (rec.getName().equalsIgnoreCase(this.getQualifiedName()))
                    {
                        DNSRecord.Text txt = (DNSRecord.Text) rec;
                        _text = txt._text;
                        serviceUpdated = true;
                    }
                    break;
                case TYPE_PTR:
                    // FIXME [PJYF June 9 2010] We need to do something here
                    break;
                default:
                    break;
            }
            if (serviceUpdated && this.hasData())
            {
                JmDNSImpl dns = this.getDns();
                if (dns != null)
                {
                    dns.handleServiceResolved(((DNSRecord) rec).getServiceEvent(dns));
                }
            }
            // This is done, to notify the wait loop in method JmDNS.waitForInfoData(ServiceInfo info, int timeout);
            synchronized (this)
            {
                this.notifyAll();
            }
        }
    }

    /**
     * Returns true if the service info is filled with data.
     *
     * @return <code>true</code> if the service info has data, <code>false</code> otherwise.
     */
    @Override
    public synchronized boolean hasData()
    {
        return this.getServer() != null && this.getAddress() != null && this.getTextBytes() != null && this.getTextBytes().length > 0;
        // return this.getServer() != null && (this.getAddress() != null || (this.getTextBytes() != null && this.getTextBytes().length > 0));
    }

    // State machine

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#advanceState()
     */
    @Override
    public boolean advanceState()
    {
        return _state.advanceState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#revertState()
     */
    @Override
    public boolean revertState()
    {
        return _state.revertState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#cancel()
     */
    @Override
    public boolean cancelState()
    {
        return _state.cancelState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#recover()
     */
    @Override
    public boolean recoverState()
    {
        return this._state.recoverState();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#removeAssociationWithTask(javax.jmdns.impl.tasks.DNSTask)
     */
    @Override
    public void removeAssociationWithTask(DNSTask task)
    {
        _state.removeAssociationWithTask(task);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#associateWithTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
     */
    @Override
    public void associateWithTask(DNSTask task, DNSState state)
    {
        _state.associateWithTask(task, state);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAssociatedWithTask(javax.jmdns.impl.tasks.DNSTask, javax.jmdns.impl.constants.DNSState)
     */
    @Override
    public boolean isAssociatedWithTask(DNSTask task, DNSState state)
    {
        return _state.isAssociatedWithTask(task, state);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isProbing()
     */
    @Override
    public boolean isProbing()
    {
        return _state.isProbing();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAnnouncing()
     */
    @Override
    public boolean isAnnouncing()
    {
        return _state.isAnnouncing();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isAnnounced()
     */
    @Override
    public boolean isAnnounced()
    {
        return _state.isAnnounced();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isCanceling()
     */
    @Override
    public boolean isCanceling()
    {
        return this._state.isCanceling();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#isCanceled()
     */
    @Override
    public boolean isCanceled()
    {
        return _state.isCanceled();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#waitForAnnounced(long)
     */
    @Override
    public boolean waitForAnnounced(long timeout)
    {
        return _state.waitForAnnounced(timeout);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.impl.DNSStatefulObject#waitForCanceled(long)
     */
    @Override
    public boolean waitForCanceled(long timeout)
    {
        return _state.waitForCanceled(timeout);
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
        return (obj instanceof ServiceInfoImpl) && getQualifiedName().equals(((ServiceInfoImpl) obj).getQualifiedName());
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
        for (int i = 0, len = this.getText().length; i < len; i++)
        {
            if (i >= 200)
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
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        return new ServiceInfoImpl(_type, _name, _port, _weight, _priority, _persistent, _text);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("[" + this.getClass().getSimpleName() + "@" + System.identityHashCode(this) + " ");
        buf.append("name: '");
        buf.append(this.getQualifiedName());
        buf.append("' address: '");
        buf.append(this.getAddress());
        buf.append(':');
        buf.append(this.getPort());
        buf.append("' status: '");
        buf.append(_state.toString());
        buf.append(this.isPersistent() ? "' is persistent," : "',");
        buf.append(" has ");
        buf.append(this.hasData() ? "" : "NO ");
        buf.append("data");
        if (this.getText().length > 0)
        {
            buf.append("\n");
            buf.append(this.getNiceTextString());
        }
        buf.append(']');
        return buf.toString();
    }

    public Collection<DNSRecord> answers(int ttl, HostInfo localHost)
    {
        List<DNSRecord> list = new ArrayList<DNSRecord>();
        list.add(new Pointer(this.getType(), DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, ttl, this.getQualifiedName()));
        list.add(new Service(this.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, ttl, _priority, _weight, _port, localHost.getName()));
        list.add(new Text(this.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, ttl, this.getText()));
        return list;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#setText(byte[])
     */
    @Override
    public void setText(byte[] text) throws IllegalStateException
    {
        synchronized (this)
        {
            this._text = text;
            this.setNeedTextAnnouncing(true);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#setText(java.util.Map)
     */
    @Override
    public void setText(Map<String, ?> props) throws IllegalStateException
    {
        this.setText(textFromProperties(props));
    }

    /**
     * This is used internally by the framework
     *
     * @param text
     */
    void _setText(byte[] text)
    {
        this._text = text;
    }

    private static byte[] textFromProperties(Map<String, ?> props)
    {
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
                    if (val == NO_VALUE)
                    {
                        // Skip
                    }
                    else if (val instanceof String)
                    {
                        out2.write('=');
                        writeUTF(out2, (String) val);
                    }
                    else if ((val instanceof byte[]) && (((byte[]) val).length > 0))
                    {
                        out2.write('=');
                        byte[] bval = (byte[]) val;
                        out2.write(bval, 0, bval.length);
                    }
                    else if (val != NO_VALUE)
                    {
                        throw new IllegalArgumentException("invalid property value: " + val);
                    }
                    byte data[] = out2.toByteArray();
                    if (data.length > 255)
                    {
                        new IOException("Cannot have individual values larger that 255 chars. Offending value: " + key + (val != NO_VALUE ? "=" + val : ""));
                    }
                    out.write((byte) data.length);
                    out.write(data, 0, data.length);
                }
                return out.toByteArray();
            }
            catch (IOException e)
            {
                throw new RuntimeException("unexpected exception: " + e);
            }
        }
        return new byte[0];
    }

    public byte[] getText()
    {
        return (this._text != null ? this._text : new byte[] {});
    }

    public void setDns(JmDNSImpl dns)
    {
        this._state.setDns(dns);
    }

    public JmDNSImpl getDns()
    {
        return this._state.getDns();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.jmdns.ServiceInfo#isPersistent()
     */
    @Override
    public boolean isPersistent()
    {
        return _persistent;
    }

    /**
     * @param needTextAnnouncing
     *            the needTextAnnouncing to set
     */
    public void setNeedTextAnnouncing(boolean needTextAnnouncing)
    {
        this._needTextAnnouncing = needTextAnnouncing;
        if (this._needTextAnnouncing)
        {
            _state.setTask(null);
        }
    }

    /**
     * @return the needTextAnnouncing
     */
    public boolean needTextAnnouncing()
    {
        return _needTextAnnouncing;
    }

}
