// %Z%%M%, %I%, %G%
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package javax.jmdns;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * JmDNS service information.
 *
 * @author	Arthur van Hoff, Jeff Sonstein
 * @version 	%I%, %G%
 */
public class ServiceInfo extends JmDNS.Listener
{
    public final static byte[] NO_VALUE = new byte[0];
    
    String type;
    String name;
    String server;
    int port;
    int weight;
    int priority;
    byte text[];
    Hashtable props;
    InetAddress addr;

    /**
     * Construct a service description for registrating with JmDNS.
     * @param type fully qualified service type name
     * @param name fully qualified service name
     * @param port the local port on which the service runs
     * @param text string describing the service
     */
     // removed following historical artifact 28 MAR 2004 jeffs
     // @param addr the address to which the service is bound
    public ServiceInfo(String type, String name, int port, String text)
    {
	this(type, name, port, 0, 0, text);
    }

    /**
     * Construct a service description for registrating with JmDNS.
     * @param type fully qualified service type name
     * @param name fully qualified service name
     * @param port the local port on which the service runs
     * @param weight weight of the service
     * @param priority priority of the service
     * @param text string describing the service
     */
    public ServiceInfo(String type, String name, int port, int weight, int priority, String text)
    {
	this(type, name, port, weight, priority, (byte[])null);
	try {
	    ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
	    writeUTF(out, text);
	    this.text = out.toByteArray();
	} catch (IOException e) {
	    throw new RuntimeException("unexpected exception: " + e);
	}
    }

    /**
     * Construct a service description for registrating with JmDNS. The properties hashtable must
     * map property names to either Strings or byte arrays describing the property values.
     * @param type fully qualified service type name
     * @param name fully qualified service name
     * @param port the local port on which the service runs
     * @param weight weight of the service
     * @param priority priority of the service
     * @param props properties describing the service
     */
    public ServiceInfo(String type, String name, int port, int weight, int priority, Hashtable props)
    {
	this(type, name, port, weight, priority, new byte[0]);
	if (props != null) {
	    try {
		ByteArrayOutputStream out = new ByteArrayOutputStream(256);
		for (Enumeration e = props.keys() ; e.hasMoreElements() ;) {
		    String key = (String)e.nextElement();
		    Object val = props.get(key);
		    ByteArrayOutputStream out2 = new ByteArrayOutputStream(100);
		    writeUTF(out2, key);
		    if (val instanceof String) {
			out2.write('=');
			writeUTF(out2, (String)val);
		    } else if (val instanceof byte[]) {
			out2.write('=');
			byte[] bval = (byte[])val;
			out2.write(bval, 0, bval.length);
		    } else if (val != NO_VALUE) {
			throw new IllegalArgumentException("invalid property value: " + val);
		    }
		    byte data[] = out2.toByteArray();
		    out.write(data.length);
		    out.write(data, 0, data.length);
		}
		this.text = out.toByteArray();
	    } catch (IOException e) {
		throw new RuntimeException("unexpected exception: " + e);
	    }
	}
    }

    /**
     * Construct a service description for registrating with JmDNS.
     * @param type fully qualified service type name
     * @param name fully qualified service name
     * @param port the local port on which the service runs
     * @param weight weight of the service
     * @param priority priority of the service
     * @param text bytes describing the service
     */
    public ServiceInfo(String type, String name, int port, int weight, int priority, byte text[])
    {
	this.type = type;
	this.name = name;
	this.port = port;
	this.weight = weight;
	this.priority = priority;
	this.text = text;
    }

    /**
     * Construct a serive record during service discovery.
     */
    ServiceInfo(String type, String name)
    {
	if (!type.endsWith(".")) {
	    throw new IllegalArgumentException("type must be fully qualified DNS name ending in '.': " + type);
	}
	if (name.endsWith(".")) {
	    if (!name.endsWith("." + type)) {
		throw new IllegalArgumentException("service name has the wrong type: name=" + name + ", type=" + type);
	    }
	} else {
	    name = name + "." + type;
	}

	this.type = type;
	this.name = name;
    }

    /**
     * Fully qualified service type name, such as <code>_http&#46;_tcp.local&#46;</code>.
     */
    public String getType()
    {
	return type;
    }

    /**
     * Service name, such as <code>foobar</code>.
     */
    public String getName()
    {
	if ((type != null) && name.endsWith("." + type)) {
	    return name.substring(0, name.length() - (type.length() + 1));
	}
	return name;
    }

    /**
     * Get the name of the server.
     */
    public String getServer()
    {
	return server;
    }

    /**
     * Get the host address of the service (ie X.X.X.X).
     */
    public String getAddress()
    {
	byte data[] = getInetAddress().getAddress();
	return (data[0] & 0xFF) + "." + (data[1] & 0xFF) + "." + (data[2] & 0xFF) + "." + (data[3] & 0xFF);
    }

    /**
     * Get the InetAddress of the service.
     */
    public InetAddress getInetAddress()
    {
	return addr;
    }

    /**
     * Get the port for the service.
     */
    public int getPort()
    {
	return port;
    }

    /**
     * Get the priority of the service.
     */
    public int getPriority()
    {
	return priority;
    }
    /**
     * Get the weight of the service.
     */
    public int getWeight()
    {
	return weight;
    }

    /**
     * Get the text for the serivce as raw bytes.
     */
    public byte[] getTextBytes()
    {
	return text;
    }

    /**
     * Get the text for the service. This will interpret the text bytes
     * as a UTF8 encoded string. Will return null if the bytes are not
     * a valid UTF8 encoded string.
     */
    public String getTextString()
    {
	if ((text == null) || (text.length == 0) || ((text.length == 1) && (text[0] == 0))) {
	    return null;
	}
	return readUTF(text, 0, text.length);
    }

    /**
     * Get the URL for this service. An http URL is created by
     * combining the addres, port, and path properties.
     */
    public String getURL()
    {
	return getURL("http");
    }

    /**
     * Get the URL for this service. An URL is created by
     * combining the protocol, addres, port, and path properties.
     */
    public String getURL(String protocol)
    {
	String url = protocol + "://" + getAddress() + ":" + getPort();
	String path = getPropertyString("path");
	if (path != null) {
	    if (path.indexOf("://") >= 0) {
		url = path;
	    } else {
		url += path.startsWith("/") ? path : "/" + path;
	    }
	}
	return url;
    }

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found or the text data could not be decoded correctly.
     */
    public synchronized byte[] getPropertyBytes(String name)
    {
	return (byte [])getProperties().get(name);
    }

    /**
     * Get a property of the service. This involves decoding the
     * text bytes into a property list. Returns null if the property
     * is not found, the text data could not be decoded correctly, or
     * the resulting bytes are not a valid UTF8 string.
     */
    public synchronized String getPropertyString(String name)
    {
	byte data[] = (byte [])getProperties().get(name);
	if (data == null) {
	    return null;
	}
	if (data == NO_VALUE) {
	    return "true";
	}
	return readUTF(data, 0, data.length);
    }

    /**
     * Enumeration of the property names.
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
	for (int i = 0, len = str.length() ; i < len ; i++) {
	    int c = str.charAt(i);
	    if ((c >= 0x0001) && (c <= 0x007F)) {
		out.write(c);
	    } else if (c > 0x07FF) {
		out.write(0xE0 | ((c >> 12) & 0x0F));
		out.write(0x80 | ((c >>  6) & 0x3F));
		out.write(0x80 | ((c >>  0) & 0x3F));
	    } else {
		out.write(0xC0 | ((c >>  6) & 0x1F));
		out.write(0x80 | ((c >>  0) & 0x3F));
	    }
	}
    }

    /**
     * Read data bytes as a UTF stream.
     */
    String readUTF(byte data[], int off, int len)
    {
	StringBuffer buf = new StringBuffer();
	for (int end = off + len ; off < end ; ) {
	    int ch = data[off++] & 0xFF;
	    switch (ch >> 4) {
	      case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
		// 0xxxxxxx
		break;
	      case 12: case 13:
		if (off >= len) {
		    return null;
		}
		// 110x xxxx   10xx xxxx
		ch = ((ch & 0x1F) << 6) | (data[off++] & 0x3F);
		break;
	      case 14:
		if (off + 2 >= len) {
		    return null;
		}
		// 1110 xxxx  10xx xxxx  10xx xxxx
		ch = ((ch & 0x0f) << 12) | ((data[off++] & 0x3F) << 6) | (data[off++] & 0x3F);
		break;
	      default:
		if (off + 1 >= len) {
		    return null;
		}
		// 10xx xxxx,  1111 xxxx
		ch = ((ch & 0x3F) << 4) | (data[off++] & 0x0f);
		break;
	    }
	    buf.append((char)ch);
	}
	return buf.toString();
    }

    synchronized Hashtable getProperties()
    {
	if ((props == null) && (text != null)) {
	    Hashtable props = new Hashtable();
	    int off = 0;
	    while (off < text.length) {
		// length of the next key value pair
		int len = text[off++] & 0xFF;
		if ((len == 0) || (off + len > text.length)) {
		    props.clear();
		    break;
		}
		// look for the '='
		int i = 0;
		for (; (i < len) && (text[off + i] != '=') ; i++);

		// get the property name
		String name = readUTF(text, off, i);
		if (name == null) {
		    props.clear();
		    break;
		}
		if (i == len) {
		    props.put(name, NO_VALUE);
		} else {
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
    void updateRecord(JmDNS jmdns, long now, DNSRecord rec)
    {
	if ((rec != null) && !rec.isExpired(now)) {
	    switch (rec.type) {
	      case DNSConstants.TYPE_A:
		if (rec.name.equals(server)) {
		    addr = ((DNSRecord.Address)rec).getInetAddress();
		}
		break;
	      case DNSConstants.TYPE_SRV:
		if (rec.name.equals(name)) {
		    DNSRecord.Service srv = (DNSRecord.Service)rec;
		    server = srv.server;
		    port = srv.port;
		    weight = srv.weight;
		    priority = srv.priority;
		    addr = null;
		    // changed to use getCache() instead - jeffs
		    // updateRecord(jmdns, now, (DNSRecord)jmdns.cache.get(server, TYPE_A, CLASS_IN));
		    updateRecord(jmdns, now, (DNSRecord)jmdns.getCache().get(server, DNSConstants.TYPE_A, DNSConstants.CLASS_IN));
		}
		break;
	      case DNSConstants.TYPE_TXT:
		if (rec.name.equals(name)) {
		    DNSRecord.Text txt = (DNSRecord.Text)rec;
		    text = txt.text;
		}
		break;
	    }
	}
    }

    /**
     * Update the server information from the cache, send out
     * repeated DNS queries for updated information.
     */
    boolean request(JmDNS jmdns, long timeout)
    {
	long now = System.currentTimeMillis();
	int delay = 200;
	long next = now + delay;
	long last = now + timeout;
	try {
	    jmdns.addListener(this, new DNSQuestion(name, DNSConstants.TYPE_ANY, DNSConstants.CLASS_IN));
	    while (server == null || addr == null || text == null) {
		// check if timeout was reached
		if (last <= now) {
		    return false;
		}
		// check if we need to send out another request
		if (next <= now) {
		    DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_QUERY);
		    out.addQuestion(new DNSQuestion(name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN));
		    out.addQuestion(new DNSQuestion(name, DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN));
		    if (server != null) {
			out.addQuestion(new DNSQuestion(server, DNSConstants.TYPE_A, DNSConstants.CLASS_IN));
		    }
		    // changed to use getCache() instead - jeffs
		    // out.addAnswer((DNSRecord)jmdns.cache.get(name, TYPE_SRV, CLASS_IN), now);
		    out.addAnswer((DNSRecord)jmdns.getCache().get(name, DNSConstants.TYPE_SRV, DNSConstants.CLASS_IN), now);
		    // out.addAnswer((DNSRecord)jmdns.cache.get(name, TYPE_TXT, CLASS_IN), now);
		    out.addAnswer((DNSRecord)jmdns.getCache().get(name, DNSConstants.TYPE_TXT, DNSConstants.CLASS_IN), now);
		    if (server != null) {
			// out.addAnswer((DNSRecord)jmdns.cache.get(server, TYPE_A, CLASS_IN), now);
			out.addAnswer((DNSRecord)jmdns.getCache().get(server, DNSConstants.TYPE_A, DNSConstants.CLASS_IN), now);
		    }
		    jmdns.send(out);

		    next = now + delay;
		    delay = delay * 2;
		}
		// wait for an update or a timeout
		synchronized (jmdns) {
		    jmdns.wait(Math.min(next, last) - now);
		}
		now = System.currentTimeMillis();
	    }
	    return true;
	} catch (IOException e) {
	    return false;
	} catch (InterruptedException e) {
	    return false;
	} finally {
	    jmdns.removeListener(this);
	}
    }

    public int hashCode()
    {
	return name.hashCode();
    }

    public boolean equals(Object obj)
    {
	return (obj instanceof ServiceInfo) && name.equals(((ServiceInfo)obj).name);
    }

    public String getNiceTextString()
    {
	StringBuffer buf = new StringBuffer();
	for (int i = 0, len = text.length ; i < len ; i++) {
	    if (i >= 20) {
		buf.append("...");
		break;
	    }
	    int ch = text[i] & 0xFF;
	    if ((ch < ' ') || (ch > 127)) {
		buf.append("\\0");
		buf.append(Integer.toString(ch, 8));
	    } else {
		buf.append((char)ch);
	    }
	}
	return buf.toString();
    }

    public String toString()
    {
	StringBuffer buf = new StringBuffer();
	buf.append("service[");
	buf.append(name);
	buf.append(',');
	buf.append(getAddress());
	buf.append(':');
	buf.append(port);
	buf.append(',');
	buf.append(getNiceTextString());
	buf.append(']');
	return buf.toString();
    }
}
