//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;

/**
 * An outgoing DNS message.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Rick Blair, Werner Randelshofer
 */
public final class DNSOutgoing extends DNSMessage
{
    /**
     * This can be used to turn off domain name compression. This was helpful for tracking problems interacting with other mdns implementations.
     */
    public static boolean USE_DOMAIN_NAME_COMPRESSION = true;

    private static Logger logger = Logger.getLogger(DNSOutgoing.class.getName());

    private Map<String, Integer> _names;

    byte[] _data;

    int _off;

    int _len;

    /**
     * Create an outgoing multicast query or response.
     *
     * @param flags
     */
    public DNSOutgoing(int flags)
    {
        this(flags, true, DNSConstants.MAX_MSG_TYPICAL);
    }

    /**
     * Create an outgoing query or response.
     *
     * @param flags
     * @param multicast
     */
    public DNSOutgoing(int flags, boolean multicast)
    {
        this(flags, multicast, DNSConstants.MAX_MSG_TYPICAL);
    }

    /**
     * Create an outgoing query or response.
     *
     * @param flags
     * @param multicast
     * @param senderUDPPayload
     *            The sender's UDP payload size is the number of bytes of the largest UDP payload that can be reassembled and delivered in the sender's network stack.
     */
    public DNSOutgoing(int flags, boolean multicast, int senderUDPPayload)
    {
        super(flags, 0, multicast);
        _names = new Hashtable<String, Integer>();
        _data = new byte[senderUDPPayload];
        _off = 12;
    }

    /**
     * Add a question to the message.
     *
     * @param rec
     * @throws IOException
     */
    public void addQuestion(DNSQuestion rec) throws IOException
    {
        if (this.getNumberOfAnswers() > 0 || this.getNumberOfAuthorities() > 0 || this.getNumberOfAdditionals() > 0)
        {
            throw new IllegalStateException("Questions must be added before answers");
        }
        _questions.add(rec);
        this.writeQuestion(rec);
    }

    /**
     * Add an answer if it is not suppressed.
     *
     * @param in
     * @param rec
     * @throws IOException
     */
    public void addAnswer(DNSIncoming in, DNSRecord rec) throws IOException
    {
        if (this.getNumberOfAuthorities() > 0 || this.getNumberOfAdditionals() > 0)
        {
            throw new IllegalStateException("Answers must be added before authorities and additionals");
        }
        if (!rec.suppressedBy(in))
        {
            this.addAnswer(rec, 0);
        }
    }

    /**
     * Add an additional answer to the record. Omit if there is no room.
     */
    void addAdditionalAnswer(DNSIncoming in, DNSRecord rec) throws IOException
    {
        if ((_off < _data.length - 200) && !rec.suppressedBy(in))
        {
            _additionals.add(rec);
            this.writeRecord(rec, 0);
        }
    }

    /**
     * Add an answer to the message.
     *
     * @param rec
     * @param now
     * @throws IOException
     */
    public void addAnswer(DNSRecord rec, long now) throws IOException
    {
        if (this.getNumberOfAuthorities() > 0 || this.getNumberOfAdditionals() > 0)
        {
            throw new IllegalStateException("Questions must be added before answers");
        }
        if (rec != null)
        {
            if ((now == 0) || !rec.isExpired(now))
            {
                _answers.add(rec);
                this.writeRecord(rec, now);
            }
        }
    }

    /**
     * Add an authorative answer to the message.
     *
     * @param rec
     * @throws IOException
     */
    public void addAuthorativeAnswer(DNSRecord rec) throws IOException
    {
        if (this.getNumberOfAdditionals() > 0)
        {
            throw new IllegalStateException("Authorative answers must be added before additional answers");
        }
        _authoritativeAnswers.add(rec);
        this.writeRecord(rec, 0);

        // VERIFY:

    }

    void writeByte(int value) throws IOException
    {
        if (_off >= _data.length)
        {
            throw new IOException("buffer full");
        }
        _data[_off++] = (byte) value;
    }

    void writeBytes(String str, int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++)
        {
            writeByte(str.charAt(off + i));
        }
    }

    void writeBytes(byte data[]) throws IOException
    {
        if (data != null)
        {
            writeBytes(data, 0, data.length);
        }
    }

    void writeBytes(byte data[], int off, int len) throws IOException
    {
        for (int i = 0; i < len; i++)
        {
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
        for (int i = 0; i < len; i++)
        {
            int ch = str.charAt(off + i);
            if ((ch >= 0x0001) && (ch <= 0x007F))
            {
                utflen += 1;
            }
            else
            {
                if (ch > 0x07FF)
                {
                    utflen += 3;
                }
                else
                {
                    utflen += 2;
                }
            }
        }
        // write utf length
        writeByte(utflen);
        // write utf data
        for (int i = 0; i < len; i++)
        {
            int ch = str.charAt(off + i);
            if ((ch >= 0x0001) && (ch <= 0x007F))
            {
                writeByte(ch);
            }
            else
            {
                if (ch > 0x07FF)
                {
                    writeByte(0xE0 | ((ch >> 12) & 0x0F));
                    writeByte(0x80 | ((ch >> 6) & 0x3F));
                    writeByte(0x80 | ((ch >> 0) & 0x3F));
                }
                else
                {
                    writeByte(0xC0 | ((ch >> 6) & 0x1F));
                    writeByte(0x80 | ((ch >> 0) & 0x3F));
                }
            }
        }
    }

    void writeName(String name) throws IOException
    {
        writeName(name, true);
    }

    void writeName(String name, boolean useCompression) throws IOException
    {
        String aName = name;
        while (true)
        {
            int n = aName.indexOf('.');
            if (n < 0)
            {
                n = aName.length();
            }
            if (n <= 0)
            {
                writeByte(0);
                return;
            }
            if (useCompression && USE_DOMAIN_NAME_COMPRESSION)
            {
                Integer offset = _names.get(aName);
                if (offset != null)
                {
                    int val = offset.intValue();

                    if (val > _off)
                    {
                        logger.log(Level.WARNING, "DNSOutgoing writeName failed val=" + val + " name=" + aName);
                    }

                    writeByte((val >> 8) | 0xC0);
                    writeByte(val & 0xFF);
                    return;
                }
                _names.put(aName, Integer.valueOf(_off));
            }
            writeUTF(aName, 0, n);
            aName = aName.substring(n);
            if (aName.startsWith("."))
            {
                aName = aName.substring(1);
            }
        }
    }

    void writeQuestion(DNSQuestion question) throws IOException
    {
        writeName(question.getName());
        writeShort(question.getRecordType().indexValue());
        writeShort(question.getRecordClass().indexValue());
    }

    void writeRecord(DNSRecord rec, long now) throws IOException
    {
        int save = _off;
        try
        {
            writeName(rec.getName());
            writeShort(rec.getRecordType().indexValue());
            writeShort(rec.getRecordClass().indexValue() | ((rec.isUnique() && this.isMulticast()) ? DNSRecordClass.CLASS_UNIQUE : 0));
            writeInt((now == 0) ? rec.getTTL() : rec.getRemainingTTL(now));
            writeShort(0);
            int start = _off;
            rec.write(this);
            int len = _off - start;
            _data[start - 2] = (byte) (len >> 8);
            _data[start - 1] = (byte) (len & 0xFF);
        }
        catch (IOException e)
        {
            _off = save;
            throw e;
        }
    }

    /**
     * Finish the message before sending it off.
     *
     * @throws IOException
     */
    void finish() throws IOException
    {
        int save = _off;
        _off = 0;

        writeShort(_multicast ? 0 : _id);
        writeShort(_flags);
        writeShort(this.getNumberOfQuestions());
        writeShort(this.getNumberOfAnswers());
        writeShort(this.getNumberOfAuthorities());
        writeShort(this.getNumberOfAdditionals());
        _off = save;
    }

    @Override
    public boolean isQuery()
    {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_QUERY;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(isQuery() ? "dns[query:" : "dns[response:");
        buf.append(" id=0x");
        buf.append(Integer.toHexString(_id));
        if (_flags != 0)
        {
            buf.append(", flags=0x");
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
        if (this.getNumberOfQuestions() > 0)
        {
            buf.append(", questions=");
            buf.append(this.getNumberOfQuestions());
        }
        if (this.getNumberOfAnswers() > 0)
        {
            buf.append(", answers=");
            buf.append(this.getNumberOfAnswers());
        }
        if (this.getNumberOfAuthorities() > 0)
        {
            buf.append(", authorities=");
            buf.append(this.getNumberOfAuthorities());
        }
        if (this.getNumberOfAdditionals() > 0)
        {
            buf.append(", additionals=");
            buf.append(this.getNumberOfAdditionals());
        }
        buf.append(",\nnames=" + _names);
        buf.append(",\nauthorativeAnswers=" + _authoritativeAnswers);

        buf.append("]");
        return buf.toString();
    }

    /**
     * Debugging.
     */
    String print(boolean dump)
    {
        StringBuffer buf = new StringBuffer();
        buf.append(this.print());
        if (dump)
        {
            for (int off = 0, len = _data.length; off < len; off += 32)
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

}
