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
 * DNS record
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
abstract class DNSRecord extends DNSEntry
{
    int ttl;
    long created;

    /**
     * Create a DNSRecord with a name, type, clazz, and ttl.
     */
    DNSRecord(String name, int type, int clazz, int ttl)
    {
	super(name, type, clazz);
	this.ttl = ttl;
	this.created = System.currentTimeMillis();
    }

    /**
     * True if this record is the same as some other record.
     */
    public boolean equals(Object other)
    {
	return (other instanceof DNSRecord) && sameAs((DNSRecord)other);
    }

    /**
     * True if this record is the same as some other record.
     */
    boolean sameAs(DNSRecord other)
    {
	return super.equals(other) && sameValue((DNSRecord)other);
    }

    /**
     * True if this record has the same value as some other record.
     */
    abstract boolean sameValue(DNSRecord other);

    /**
     * True if this record is suppressed by the answers in a message.
     */
    boolean suppressedBy(DNSIncoming msg)
    {
	for (int i = msg.numAnswers ; i-- > 0 ;) {
	    if (suppressedBy((DNSRecord)msg.answers.elementAt(i))) {
		return true;
	    }
	}
	return false;
    }

    /**
     * True if this record would be supressed by an answer.
     * This is the case if this record would not have a
     * significantly longer TTL.
     */
    boolean suppressedBy(DNSRecord other)
    {
	if (sameAs(other) && (other.ttl > ttl / 2)) {
	    return true;
	}
	return false;
    }

    /**
     * Get the expiration time of this record.
     */
    long getExpirationTime(int percent)
    {
	return created + (percent * ttl * 10L);
    }

    /**
     * Get the remaining TTL for this record.
     */
    int getRemainingTTL(long now)
    {
	return (int)Math.max(0, (getExpirationTime(100) - now) / 1000);
    }

    /**
     * Check if the record is expired.
     */
    boolean isExpired(long now)
    {
	return getExpirationTime(100) <= now;
    }

    /**
     * Check if the record is stale, ie it has outlived
     * more than half of its TTL.
     */
    boolean isStale(long now)
    {
	return getExpirationTime(50) <= now;
    }

    /**
     * Reset the TTL of a record. This avoids having to
     * update the entire record in the cache.
     */
    void resetTTL(DNSRecord other)
    {
	created = other.created;
	ttl = other.ttl;
    }

    /**
     * Write this record into an outgoing message.
     */
    abstract void write(DNSOutgoing out) throws IOException;

    /**
     * Address record.
     */
    static class Address extends DNSRecord
    {
	int addr;

	Address(String name, int type, int clazz, int ttl, int addr)
	{
	    super(name, type, clazz, ttl);
	    this.addr = addr;
	}
	void write(DNSOutgoing out) throws IOException
	{
	    out.writeInt(addr);
	}
	boolean sameValue(DNSRecord other)
	{
	    return (addr == ((Address)other).addr);
	}
	InetAddress getInetAddress()
	{
	    try {
		return InetAddress.getByName(getAddress());
	    } catch (UnknownHostException e) {
		// should not happen
		e.printStackTrace();
		return null;
	    }
	}
	String getAddress()
	{
	    return ((addr >> 24) & 0xFF) + "." + ((addr >> 16) & 0xFF) + "." + ((addr >> 8) & 0xFF) + "." + (addr & 0xFF);
	}
	public String toString()
	{
	    return toString(getAddress());
	}
    }

    /**
     * Pointer record.
     */
    static class Pointer extends DNSRecord
    {
	String alias;

	Pointer(String name, int type, int clazz, int ttl, String alias)
	{
	    super(name, type, clazz, ttl);
	    this.alias = alias;
	}
	void write(DNSOutgoing out) throws IOException
	{
	    out.writeName(alias);
	}
	boolean sameValue(DNSRecord other)
	{
	    return alias.equals(((Pointer)other).alias);
	}
	public String toString()
	{
	    return toString(alias);
	}
    }

    static class Text extends DNSRecord
    {
	byte text[];

	Text(String name, int type, int clazz, int ttl, byte text[])
	{
	    super(name, type, clazz, ttl);
	    this.text = text;
	}
	void write(DNSOutgoing out) throws IOException
	{
	    out.writeBytes(text, 0, text.length);
	}
	boolean sameValue(DNSRecord other)
	{
	    Text txt = (Text)other;
	    if (txt.text.length != text.length) {
		return false;
	    }
	    for (int i = text.length ; i-- > 0 ;) {
		if (txt.text[i] != text[i]) {
		    return false;
		}
	    }
	    return true;
	}
	public String toString()
	{
	    return toString((text.length > 10) ? new String(text, 0, 7) + "..." : new String(text));
	}
    }

    /**
     * Service record.
     */
    static class Service extends DNSRecord
    {
	int priority;
	int weight;
	int port;
	String server;

	Service(String name, int type, int clazz, int ttl, int priority, int weight, int port, String server)
	{
	    super(name, type, clazz, ttl);
	    this.priority = priority;
	    this.weight = weight;
	    this.port = port;
	    this.server = server;
	}
	void write(DNSOutgoing out) throws IOException
	{
	    out.writeShort(priority);
	    out.writeShort(weight);
	    out.writeShort(port);
	    out.writeName(server);
	}
	boolean sameValue(DNSRecord other)
	{
	    Service s = (Service)other;
	    return (priority == s.priority) && (weight == s.weight) && (port == s.port) && server.equals(s.server);
	}
	public String toString()
	{
	    return toString(server + ":" + port);
	}
    }

    public String toString(String other)
    {
	return toString("record", ttl + "/" + getRemainingTTL(System.currentTimeMillis())  + "," + other);
    }
}

