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
 * An outgoing DNS message.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
final class DNSOutgoing {
    int id;
    int flags;
    boolean multicast;
    int numQuestions;
    int numAnswers;
    int numAuthorities;
    int numAdditionals;
    Hashtable names;

    byte data[];
    int off;
    int len;

    /**
     * Create an outgoing multicast query or response.
     */
    DNSOutgoing(int flags)
    {
	this(flags, true);
    }

    /**
     * Create an outgoing query or response.
     */
    DNSOutgoing(int flags, boolean multicast)
    {
	this.flags = flags;
	this.multicast = multicast;
	names = new Hashtable();
	data = new byte[DNSConstants.MAX_MSG_TYPICAL];
	off = 12;
    }

    /**
     * Add a question to the message.
     */
    void addQuestion(DNSQuestion rec) throws IOException
    {
	numQuestions++;
	writeQuestion(rec);
    }

    /**
     * Add an answer if it is not suppressed.
     */
    void addAnswer(DNSIncoming in, DNSRecord rec) throws IOException
    {
	if (!rec.suppressedBy(in)) {
	    addAnswer(rec, 0);
	}
    }

    /**
     * Add an additional answer to the record. Omit if there is no room.
     */
    void addAdditionalAnswer(DNSIncoming in, DNSRecord rec) throws IOException
    {
	if ((off < DNSConstants.MAX_MSG_TYPICAL - 200) && !rec.suppressedBy(in)) {
	    writeRecord(rec, 0);
	    numAdditionals++;
	}
    }

    /**
     * Add an answer to the message.
     */
    void addAnswer(DNSRecord rec, long now) throws IOException
    {
	if (rec != null) {
	    if ((now == 0) || !rec.isExpired(now)) {
		writeRecord(rec, now);
		numAnswers++;
	    }
	}
    }

    /**
     * Add an authoritive answer to the message.
     */
    void addAuthorativeAnswer(DNSRecord rec) throws IOException
    {
	writeRecord(rec, 0);
	numAuthorities++;
    }

    void writeByte(int value) throws IOException
    {
	if (off >= data.length) {
	    throw new IOException("buffer full");
	}
	data[off++] = (byte)value;
    }

    void writeBytes(String str, int off, int len) throws IOException
    {
	for (int i = 0 ; i < len ; i++) {
	    writeByte(str.charAt(off + i));
	}
    }

    void writeBytes(byte data[], int off, int len) throws IOException
    {
	for (int i = 0 ; i < len ; i++) {
	    writeByte(data[off + i]);
	}
    }

    void writeShort(int value) throws IOException
    {
	writeByte(value >> 8);
	writeByte(value);
    }

    void writeInt(int value) throws IOException
    {
	writeShort(value >> 16);
	writeShort(value);
    }

    void writeUTF(String str, int off, int len) throws IOException
    {
	// compute utf length
	int utflen = 0;
	for (int i = 0 ; i < len ; i++) {
	    int ch = str.charAt(off + i);
	    if ((ch >= 0x0001) && (ch <= 0x007F)) {
		utflen += 1;
	    } else if (ch > 0x07FF) {
		utflen += 3;
	    } else {
		utflen += 2;
	    }
	}
	// write utf length
	writeByte(utflen);
	// write utf data
	for (int i = 0 ; i < len ; i++) {
	    int ch = str.charAt(off + i);
	    if ((ch >= 0x0001) && (ch <= 0x007F)) {
		writeByte(ch);
	    } else if (ch > 0x07FF) {
		writeByte(0xE0 | ((ch >> 12) & 0x0F));
		writeByte(0x80 | ((ch >>  6) & 0x3F));
		writeByte(0x80 | ((ch >>  0) & 0x3F));
	    } else {
		writeByte(0xC0 | ((ch >>  6) & 0x1F));
		writeByte(0x80 | ((ch >>  0) & 0x3F));
	    }
	}
    }

    void writeName(String name) throws IOException
    {
	while (true) {
	    int n = name.indexOf('.');
	    if (n < 0) {
		n = name.length();
	    }
	    if (n <= 0) {
		writeByte(0);
		return;
	    }
	    Integer offset = (Integer)names.get(name);
	    if (offset != null) {
		int val = offset.intValue();
		writeByte((val >> 8) | 0xC0);
		writeByte(val);
		return;
	    }
	    names.put(name, new Integer(off));
	    writeUTF(name, 0, n);
	    name = name.substring(n);
	    if (name.startsWith(".")) {
		name = name.substring(1);
	    }
	}
    }

    void writeQuestion(DNSQuestion question) throws IOException
    {
	writeName(question.name);
	writeShort(question.type);
	writeShort(question.clazz);
    }

    void writeRecord(DNSRecord rec, long now) throws IOException
    {
	int save = off;
	try {
	    writeName(rec.name);
	    writeShort(rec.type);
	    writeShort(rec.clazz | ((rec.unique && multicast) ? DNSConstants.CLASS_UNIQUE : 0));
	    writeInt((now == 0) ? rec.ttl : rec.getRemainingTTL(now));
	    writeShort(0);
	    int start = off;
	    rec.write(this);
	    int len = off - start;
	    data[start-2] = (byte)(len >> 8);
	    data[start-1] = (byte)(len & 0xFF);
	} catch (IOException e) {
	    off = save;
	    throw e;
	}
    }

    /**
     * Finish the message before sending it off.
     */
    void finish() throws IOException
    {
	int save = off;
	off = 0;

	writeShort(multicast ? 0 : id);
	writeShort(flags);
	writeShort(numQuestions);
	writeShort(numAnswers);
	writeShort(numAuthorities);
	writeShort(numAdditionals);
	off = save;
    }
}
