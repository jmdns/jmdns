// Copyright (C) 2002  Strangeberry Inc.
// @(#)DNSIncoming.java, 1.22, 11/29/2002
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

package com.strangeberry.jmdns;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Parse an incoming DNS message into its components.
 *
 * @author	Arthur van Hoff
 * @version 	1.18, 10/14/2002
 */
final class DNSIncoming extends DNSConstants
{
    final static Vector EMPTY = new Vector();
    
    DatagramPacket packet;
    int off;
    int len;
    byte data[];

    int id;
    int flags;
    int numQuestions;
    int numAnswers;
    int numAuthorities;
    int numAdditionals;

    Vector questions;
    Vector answers;

    /**
     * Parse a message from a datagram packet.
     */
    DNSIncoming(DatagramPacket packet) throws IOException
    {
	this.packet = packet;
	this.data = packet.getData();
	this.len = packet.getLength();
	this.off = packet.getOffset();
	this.questions = EMPTY;
	this.answers = EMPTY;

	try {
	    id = readUnsignedShort();
	    flags = readUnsignedShort();
	    numQuestions = readUnsignedShort();
	    numAnswers = readUnsignedShort();
	    numAuthorities = readUnsignedShort();
	    numAdditionals = readUnsignedShort();

	    // parse questions
	    if (numQuestions > 0) {
		questions = new Vector(numQuestions);
		for (int i = 0 ; i < numQuestions ; i++) {
		    DNSQuestion question = new DNSQuestion(readName(), readUnsignedShort(), readUnsignedShort());
		    questions.add(question);
		}
	    }

	    // parse answers
	    int n = numAnswers + numAuthorities + numAdditionals;
	    if (n > 0) {
		answers = new Vector(n);
		for (int i = 0 ; i < n ; i++) {
		    String domain = readName();
		    int type = readUnsignedShort();
		    int clazz = readUnsignedShort();
		    int ttl = readInt();
		    int len = readUnsignedShort();
		    int end = off + len;
		    DNSRecord rec = null;

		    switch (type) {
		      case TYPE_A:
			rec = new DNSRecord.Address(domain, type, clazz, ttl, readInt());
			break;
		      case TYPE_CNAME:
		      case TYPE_PTR:
			rec = new DNSRecord.Pointer(domain, type, clazz, ttl, readName());
			break;
		      case TYPE_TXT:
			rec = new DNSRecord.Text(domain, type, clazz, ttl, readBytes(off, len));
			break;
		      case TYPE_SRV:
			rec = new DNSRecord.Service(domain, type, clazz, ttl,
				    readUnsignedShort(), readUnsignedShort(), readUnsignedShort(), readName());
			break;
		    }
		    if (rec != null) {
			answers.add(rec);
		    }
		    off = end;
		}
	    }
	} catch (IOException e) {
	    print(true);
	    throw e;
	} 
    }

    /**
     * Check if the message is a query.
     */
    boolean isQuery()
    {
	return (flags & FLAGS_QR_MASK) == FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is a response.
     */
    boolean isResponse()
    {
	return (flags & FLAGS_QR_MASK) == FLAGS_QR_RESPONSE;
    }

    int get(int off) throws IOException
    {
	if ((off < 0) || (off >= len)) {
	    throw new IOException("parser error: offset=" + off);
	}
	return data[off] & 0xFF;
    }

    int readUnsignedShort() throws IOException
    {
	return (get(off++) << 8) + get(off++);
    }

    int readInt() throws IOException
    {
	return (readUnsignedShort() << 16) + readUnsignedShort();
    }

    byte[] readBytes(int off, int len) throws IOException
    {
	byte bytes[] = new byte[len];
	System.arraycopy(data, off, bytes, 0, len);
	return bytes;
    }

    void readUTF(StringBuffer buf, int off, int len) throws IOException
    {
	for (int end = off + len ; off < end ; ) {
	    int ch = get(off++);
	    switch (ch >> 4) {
	      case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
		// 0xxxxxxx
		break;
	      case 12: case 13:
		// 110x xxxx   10xx xxxx
		ch = ((ch & 0x1F) << 6) | (get(off++) & 0x3F);
		break;
	      case 14:
		// 1110 xxxx  10xx xxxx  10xx xxxx
		ch = ((ch & 0x0f) << 12) | ((get(off++) & 0x3F) << 6) | (get(off++) & 0x3F);
		break;
	      default:
		// 10xx xxxx,  1111 xxxx
		ch = ((ch & 0x3F) << 4) | (get(off++) & 0x0f);
		break;
	    }
	    buf.append((char)ch);
	}
    }

    String readName() throws IOException
    {
	StringBuffer buf = new StringBuffer();
	int off = this.off;
	int next = -1;

	while (true) {
	    int len = get(off++);
	    if (len == 0) {
		break;
	    }
	    switch (len & 0xC0) {
	      case 0x00:
		//buf.append("[" + off + "]");
		readUTF(buf, off, len);
		off += len;
		buf.append('.');
		break;
	      case 0xC0:
		//buf.append("<" + (off - 1) + ">");
		if (next < 0) {
		    next = off + 1;
		}
		off = ((len & 0x3F) << 8) | get(off++);
		break;
	      default:
		throw new IOException("bad domain name: '" + buf + "' at " + off);
	    }
	}
	this.off = (next >= 0) ? next : off;
	return buf.toString();
    }

    /**
     * Debugging.
     */
    void print(boolean dump)
    {
	System.out.println(toString());
	for (Enumeration e = questions.elements() ; e.hasMoreElements() ;) {
	    System.out.println("    " + e.nextElement());
	}
	for (Enumeration e = answers.elements() ; e.hasMoreElements() ;) {
	    System.out.println("    " + e.nextElement());
	}
	if (dump) {
	    for (int off = 0, len = packet.getLength() ; off < len ; off += 32) {
		int n = Math.min(32, len - off);
		if (off < 10) {
		    System.out.print(' ');
		}
		if (off < 100) {
		    System.out.print(' ');
		}
		System.out.print(off);
		System.out.print(':');
		for (int i = 0 ; i < n ; i++) {
		    if ((i % 8) == 0) {
			System.out.print(' ');
		    }
		    System.out.print(Integer.toHexString((data[off + i] & 0xF0) >> 4));
		    System.out.print(Integer.toHexString((data[off + i] & 0x0F) >> 0));
		}
		System.out.println();
		System.out.print("    ");
		for (int i = 0 ; i < n ; i++) {
		    if ((i % 8) == 0) {
			System.out.print(' ');
		    }
		    System.out.print(' ');
		    int ch = data[off + i] & 0xFF;
		    System.out.print(((ch > ' ') && (ch < 127)) ? (char)ch : '.');
		}
		System.out.println();

		// limit message size
		if (off+32 >= 256) {
		    System.out.println("....");
		    break;
		}
	    }
	}
    }

    public String toString()
    {
	StringBuffer buf = new StringBuffer();
	buf.append(isQuery() ? "dns[query," : "dns[response,");
	buf.append(packet.getAddress().getHostAddress());
	buf.append(':');
	buf.append(packet.getPort());
	buf.append(",len=" + packet.getLength());
	buf.append(",id=0x" + Integer.toHexString(id));
	if (flags != 0) {
	    buf.append(",flags=0x" + Integer.toHexString(id));
	    if ((flags & FLAGS_QR_RESPONSE) != 0) {
		buf.append(":r");
	    }
	    if ((flags & FLAGS_AA) != 0) {
		buf.append(":aa");
	    }
	    if ((flags & FLAGS_TC) != 0) {
		buf.append(":rc");
	    }
	}
	if (numQuestions > 0) {
	    buf.append(",questions=" + numQuestions);
	}
	if (numAnswers > 0) {
	    buf.append(",answers=" + numAnswers);
	}
	if (numAuthorities > 0) {
	    buf.append(",authorities=" + numAuthorities);
	}
	if (numAdditionals > 0) {
	    buf.append(",additionals=" + numAdditionals);
	}
	buf.append("]");
	return buf.toString();
    }
}
