///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parse an incoming DNS message into its components.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Werner Randelshofer, Pierre Frisch, Daniel Bobbert
 */
public final class DNSIncoming extends DNSMessage
{
    private static Logger logger = Logger.getLogger(DNSIncoming.class.getName());

    // This is a hack to handle a bug in the BonjourConformanceTest
    // It is sending out target strings that don't follow the "domain name"
    // format.
    public static boolean USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET = true;

    // Implementation note: This vector should be immutable.
    // If a client of DNSIncoming changes the contents of this vector,
    // we get undesired results. To fix this, we have to migrate to
    // the Collections API of Java 1.2. i.e we replace Vector by List.
    // final static Vector EMPTY = new Vector();

    private DatagramPacket _packet;

    private int _numQuestions;
    int _numAnswers;
    private int _numAuthorities;
    private int _numAdditionals;
    private long _receivedTime;

    private List<DNSQuestion> _questions;
    List<DNSRecord> _answers;

    /**
     * Parse a message from a datagram packet.
     */
    DNSIncoming(DatagramPacket packet) throws IOException
    {
        this._packet = packet;
        InetAddress source = packet.getAddress();
        this._data = packet.getData();
        this._len = packet.getLength();
        this._off = packet.getOffset();
        this._questions = Collections.emptyList();
        this._answers = Collections.emptyList();
        this._receivedTime = System.currentTimeMillis();

        try
        {
            _id = readUnsignedShort();
            _flags = readUnsignedShort();
            _numQuestions = readUnsignedShort();
            _numAnswers = readUnsignedShort();
            _numAuthorities = readUnsignedShort();
            _numAdditionals = readUnsignedShort();

            // parse questions
            if (_numQuestions > 0)
            {
                _questions = Collections.synchronizedList(new ArrayList<DNSQuestion>(_numQuestions));
                for (int i = 0; i < _numQuestions; i++)
                {
                    DNSQuestion question = new DNSQuestion(readName(), readUnsignedShort(), readUnsignedShort());
                    _questions.add(question);
                }
            }

            // parse answers
            int n = _numAnswers + _numAuthorities + _numAdditionals;
            if (n > 0)
            {
                _answers = Collections.synchronizedList(new ArrayList<DNSRecord>(n));
                for (int i = 0; i < n; i++)
                {
                    String domain = readName();
                    int type = readUnsignedShort();
                    int clazz = readUnsignedShort();
                    int ttl = readInt();
                    int len = readUnsignedShort();
                    int end = _off + len;
                    DNSRecord rec = null;

                    switch (type)
                    {
                        case DNSConstants.TYPE_A: // IPv4
                        case DNSConstants.TYPE_AAAA: // IPv6 FIXME [PJYF Oct 14 2004] This has not been tested
                            rec = new DNSRecord.Address(domain, type, clazz, ttl, readBytes(_off, len));
                            break;
                        case DNSConstants.TYPE_CNAME:
                        case DNSConstants.TYPE_PTR:
                            String service = "";
                            try
                            {
                                service = readName();
                            }
                            catch (IOException e)
                            {
                                // there was a problem reading the service name
                                e.printStackTrace();
                            }
                            rec = new DNSRecord.Pointer(domain, type, clazz, ttl, service);
                            break;
                        case DNSConstants.TYPE_TXT:
                            rec = new DNSRecord.Text(domain, type, clazz, ttl, readBytes(_off, len));
                            break;
                        case DNSConstants.TYPE_SRV:
                            int priority = readUnsignedShort();
                            int weight = readUnsignedShort();
                            int port = readUnsignedShort();
                            String target = "";
                            try
                            {
                                // This is a hack to handle a bug in the BonjourConformanceTest
                                // It is sending out target strings that don't follow the "domain name"
                                // format.

                                if (USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET)
                                {
                                    target = readName();
                                }
                                else
                                {
                                    target = readNonNameString();
                                }
                            }
                            catch (IOException e)
                            {
                                // this can happen if the type of the label
                                // cannot be handled.
                                // down below the offset gets advanced to the end
                                // of the record
                                e.printStackTrace();
                            }
                            rec = new DNSRecord.Service(domain, type, clazz, ttl, priority, weight, port, target);
                            break;
                        case DNSConstants.TYPE_HINFO:
                            // Maybe we should do something with those
                            break;
                        default:
                            logger.finer("DNSIncoming() unknown type:" + type);
                            break;
                    }

                    if (rec != null)
                    {
                        rec.setRecordSource(source);
                        // Add a record, if we were able to create one.
                        _answers.add(rec);
                    }
                    else
                    {
                        // Addjust the numbers for the skipped record
                        if (_answers.size() < _numAnswers)
                        {
                            _numAnswers--;
                        }
                        else
                        {
                            if (_answers.size() < _numAnswers + _numAuthorities)
                            {
                                _numAuthorities--;
                            }
                            else
                            {
                                if (_answers.size() < _numAnswers + _numAuthorities + _numAdditionals)
                                {
                                    _numAdditionals--;
                                }
                            }
                        }
                    }
                    _off = end;
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Level.WARNING, "DNSIncoming() dump " + print(true) + "\n exception ", e);
            throw e;
        }
    }

    /**
     * Check if the message is a query.
     *
     * @return <code>true</code> if the message is a query, <code>false</code> otherwise.
     */
    boolean isQuery()
    {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is truncated.
     *
     * @return <code>true</code> if the message is truncated, <code>false</code> otherwise.
     */
    public boolean isTruncated()
    {
        return (_flags & DNSConstants.FLAGS_TC) != 0;
    }

    /**
     * Check if the message is a response.
     *
     * @return <code>true</code> if the message is a response, <code>false</code> otherwise.
     */
    boolean isResponse()
    {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_RESPONSE;
    }

    private int get(int off) throws IOException
    {
        if ((off < 0) || (off >= _len))
        {
            throw new IOException("parser error: offset=" + off);
        }
        return _data[off] & 0xFF;
    }

    private int readUnsignedShort() throws IOException
    {
        return (get(_off++) << 8) + get(_off++);
    }

    private int readInt() throws IOException
    {
        return (readUnsignedShort() << 16) + readUnsignedShort();
    }

    /**
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    private byte[] readBytes(int off, int len) throws IOException
    {
        byte bytes[] = new byte[len];
        System.arraycopy(_data, off, bytes, 0, len);
        return bytes;
    }

    private void readUTF(StringBuffer buf, int off, int len) throws IOException
    {
        int offset = off;
        for (int end = offset + len; offset < end;)
        {
            int ch = get(offset++);
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
                    // 110x xxxx 10xx xxxx
                    ch = ((ch & 0x1F) << 6) | (get(offset++) & 0x3F);
                    break;
                case 14:
                    // 1110 xxxx 10xx xxxx 10xx xxxx
                    ch = ((ch & 0x0f) << 12) | ((get(offset++) & 0x3F) << 6) | (get(offset++) & 0x3F);
                    break;
                default:
                    // 10xx xxxx, 1111 xxxx
                    ch = ((ch & 0x3F) << 4) | (get(offset++) & 0x0f);
                    break;
            }
            buf.append((char) ch);
        }
    }

    private String readNonNameString() throws IOException
    {
        StringBuffer buf = new StringBuffer();
        int off = this._off;
        int len = get(off++);
        readUTF(buf, off, len);

        return buf.toString();
    }

    private String readName() throws IOException
    {
        StringBuffer buf = new StringBuffer();
        int off = this._off;
        int next = -1;
        int first = off;

        while (true)
        {
            int len = get(off++);
            if (len == 0)
            {
                break;
            }
            switch (len & 0xC0)
            {
                case 0x00:
                    // buf.append("[" + off + "]");
                    readUTF(buf, off, len);
                    off += len;
                    buf.append('.');
                    break;
                case 0xC0:
                    // buf.append("<" + (off - 1) + ">");
                    if (next < 0)
                    {
                        next = off + 1;
                    }
                    off = ((len & 0x3F) << 8) | get(off++);
                    if (off >= first)
                    {
                        throw new IOException("bad domain name: possible circular name detected." + " name start: "
                                + first + " bad offset: 0x" + Integer.toHexString(off));
                    }
                    first = off;
                    break;
                default:
                    throw new IOException("unsupported dns label type: '" + Integer.toHexString(len & 0xC0) + "' at "
                            + (off - 1));
            }
        }
        this._off = (next >= 0) ? next : off;
        return buf.toString();
    }

    /**
     * Debugging.
     */
    String print(boolean dump)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(toString() + "\n");
        for (Iterator<DNSQuestion> iterator = _questions.iterator(); iterator.hasNext();)
        {
            buf.append("    ques:" + iterator.next() + "\n");
        }
        int count = 0;
        for (Iterator<DNSRecord> iterator = _answers.iterator(); iterator.hasNext(); count++)
        {
            if (count < _numAnswers)
            {
                buf.append("    answ:");
            }
            else
            {
                if (count < _numAnswers + _numAuthorities)
                {
                    buf.append("    auth:");
                }
                else
                {
                    buf.append("    addi:");
                }
            }
            buf.append(iterator.next() + "\n");
        }
        if (dump)
        {
            for (int off = 0, len = _packet.getLength(); off < len; off += 32)
            {
                int n = Math.min(32, len - off);
                if (off < 10)
                {
                    buf.append(' ');
                }
                if (off < 100)
                {
                    buf.append(' ');
                }
                buf.append(off);
                buf.append(':');
                for (int i = 0; i < n; i++)
                {
                    if ((i % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(Integer.toHexString((_data[off + i] & 0xF0) >> 4));
                    buf.append(Integer.toHexString((_data[off + i] & 0x0F) >> 0));
                }
                buf.append("\n");
                buf.append("    ");
                for (int i = 0; i < n; i++)
                {
                    if ((i % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(' ');
                    int ch = _data[off + i] & 0xFF;
                    buf.append(((ch > ' ') && (ch < 127)) ? (char) ch : '.');
                }
                buf.append("\n");

                // limit message size
                if (off + 32 >= 256)
                {
                    buf.append("....\n");
                    break;
                }
            }
        }
        return buf.toString();
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(isQuery() ? "dns[query," : "dns[response,");
        if (_packet.getAddress() != null)
        {
            buf.append(_packet.getAddress().getHostAddress());
        }
        buf.append(':');
        buf.append(_packet.getPort());
        buf.append(",len=");
        buf.append(_packet.getLength());
        buf.append(",id=0x");
        buf.append(Integer.toHexString(_id));
        if (_flags != 0)
        {
            buf.append(",flags=0x");
            buf.append(Integer.toHexString(_flags));
            if ((_flags & DNSConstants.FLAGS_QR_RESPONSE) != 0)
            {
                buf.append(":r");
            }
            if ((_flags & DNSConstants.FLAGS_AA) != 0)
            {
                buf.append(":aa");
            }
            if ((_flags & DNSConstants.FLAGS_TC) != 0)
            {
                buf.append(":tc");
            }
        }
        if (_numQuestions > 0)
        {
            buf.append(",questions=");
            buf.append(_numQuestions);
        }
        if (_numAnswers > 0)
        {
            buf.append(",answers=");
            buf.append(_numAnswers);
        }
        if (_numAuthorities > 0)
        {
            buf.append(",authorities=");
            buf.append(_numAuthorities);
        }
        if (_numAdditionals > 0)
        {
            buf.append(",additionals=");
            buf.append(_numAdditionals);
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Appends answers to this Incoming.
     *
     * @throws IllegalArgumentException
     *             If not a query or if Truncated.
     */
    void append(DNSIncoming that)
    {
        if (this.isQuery() && this.isTruncated() && that.isQuery())
        {
            if (that._numQuestions > 0)
            {
                if (Collections.EMPTY_LIST.equals(this._questions))
                    this._questions = Collections.synchronizedList(new ArrayList<DNSQuestion>(that._numQuestions));

                this._questions.addAll(that._questions);
                this._numQuestions += that._numQuestions;
            }

            if (Collections.EMPTY_LIST.equals(_answers))
            {
                _answers = Collections.synchronizedList(new ArrayList<DNSRecord>());
            }

            if (that._numAnswers > 0)
            {
                this._answers.addAll(this._numAnswers, that._answers.subList(0, that._numAnswers));
                this._numAnswers += that._numAnswers;
            }
            if (that._numAuthorities > 0)
            {
                this._answers.addAll(this._numAnswers + this._numAuthorities, that._answers.subList(that._numAnswers,
                        that._numAnswers + that._numAuthorities));
                this._numAuthorities += that._numAuthorities;
            }
            if (that._numAdditionals > 0)
            {
                this._answers.addAll(that._answers.subList(that._numAnswers + that._numAuthorities, that._numAnswers
                        + that._numAuthorities + that._numAdditionals));
                this._numAdditionals += that._numAdditionals;
            }
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    public int elapseSinceArrival()
    {
        return (int) (System.currentTimeMillis() - _receivedTime);
    }

    public List<DNSQuestion> getQuestions()
    {
        return _questions;
    }

    public List<DNSRecord> getAnswers()
    {
        return _answers;
    }
}
