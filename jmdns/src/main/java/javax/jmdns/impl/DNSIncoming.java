///Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSLabel;
import javax.jmdns.impl.constants.DNSOptionCode;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;
import javax.jmdns.impl.constants.DNSResultCode;

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
    // It is sending out target strings that don't follow the "domain name" format.
    public static boolean USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET = true;

    public static class MessageInputStream extends ByteArrayInputStream
    {
        // FIXME [PJYF September 9 2010] Future design pattern convert this class to use a custom byte stream.

        /**
         * @param buffer
         * @param offset
         * @param length
         */
        public MessageInputStream(byte[] buffer, int offset, int length)
        {
            super(buffer, offset, length);
        }

    }

    private DatagramPacket _packet;

    private int _off;

    private int _len;

    private byte[] _data;

    private long _receivedTime;

    private int _senderUDPPayload;

    /**
     * Parse a message from a datagram packet.
     */
    DNSIncoming(DatagramPacket packet) throws IOException
    {
        super(0, 0, packet.getPort() == DNSConstants.MDNS_PORT);
        this._packet = packet;
        InetAddress source = packet.getAddress();
        this._data = packet.getData();
        this._len = packet.getLength();
        this._off = packet.getOffset();
        this._receivedTime = System.currentTimeMillis();
        this._senderUDPPayload = DNSConstants.MAX_MSG_TYPICAL;

        try
        {
            this.setId(this.readUnsignedShort());
            this.setFlags(this.readUnsignedShort());
            int numQuestions = readUnsignedShort();
            int numAnswers = readUnsignedShort();
            int numAuthorities = readUnsignedShort();
            int numAdditionals = readUnsignedShort();

            // parse questions
            if (numQuestions > 0)
            {
                for (int i = 0; i < numQuestions; i++)
                {
                    _questions.add(this.readQuestion());
                }
            }

            // parse answers
            if (numAnswers > 0)
            {
                for (int i = 0; i < numAnswers; i++)
                {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null)
                    {
                        // Add a record, if we were able to create one.
                        _answers.add(rec);
                    }
                }
            }

            if (numAuthorities > 0)
            {
                for (int i = 0; i < numAuthorities; i++)
                {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null)
                    {
                        // Add a record, if we were able to create one.
                        _authoritativeAnswers.add(rec);
                    }
                }
            }

            if (numAdditionals > 0)
            {
                for (int i = 0; i < numAdditionals; i++)
                {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null)
                    {
                        // Add a record, if we were able to create one.
                        _additionals.add(rec);
                    }
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Level.WARNING, "DNSIncoming() dump " + print(true) + "\n exception ", e);
            throw e;
        }
    }

    private DNSQuestion readQuestion() throws IOException
    {
        String domain = this.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(this.readUnsignedShort());
        int recordClassIndex = this.readUnsignedShort();
        DNSRecordClass recordClass = DNSRecordClass.classForIndex(recordClassIndex);
        boolean unique = recordClass.isUnique(recordClassIndex);
        return DNSQuestion.newQuestion(domain, type, recordClass, unique);
    }

    private DNSRecord readAnswer(InetAddress source) throws IOException
    {
        String domain = this.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(this.readUnsignedShort());
        int recordClassIndex = this.readUnsignedShort();
        DNSRecordClass recordClass = (type == DNSRecordType.TYPE_OPT ? DNSRecordClass.CLASS_UNKNOWN : DNSRecordClass.classForIndex(recordClassIndex));
        boolean unique = recordClass.isUnique(recordClassIndex);
        int ttl = this.readInt();
        int len = this.readUnsignedShort();
        int end = _off + len;
        DNSRecord rec = null;

        switch (type)
        {
            case TYPE_A: // IPv4
                rec = new DNSRecord.IPv4Address(domain, recordClass, unique, ttl, readBytes(_off, len));
                _off = _off + len;
                break;
            case TYPE_AAAA: // IPv6
                rec = new DNSRecord.IPv6Address(domain, recordClass, unique, ttl, readBytes(_off, len));
                _off = _off + len;
                break;
            case TYPE_CNAME:
            case TYPE_PTR:
                String service = "";
                try
                {
                    service = this.readName();
                }
                catch (IOException e)
                {
                    // there was a problem reading the service name
                    logger.log(Level.WARNING, "There was a problem reading the service name of the answer for domain:" + domain, e);
                }
                if (service.length() > 0)
                {
                    rec = new DNSRecord.Pointer(domain, recordClass, unique, ttl, service);
                }
                else
                {
                    logger.log(Level.WARNING, "There was a problem reading the service name of the answer for domain:" + domain);
                }
                break;
            case TYPE_TXT:
                rec = new DNSRecord.Text(domain, recordClass, unique, ttl, readBytes(_off, len));
                _off = _off + len;
                break;
            case TYPE_SRV:
                int priority = readUnsignedShort();
                int weight = readUnsignedShort();
                int port = readUnsignedShort();
                String target = "";
                try
                {
                    // This is a hack to handle a bug in the BonjourConformanceTest
                    // It is sending out target strings that don't follow the "domain name" format.

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
                    // this can happen if the type of the label cannot be handled.
                    // down below the offset gets advanced to the end of the record
                    logger.log(Level.WARNING, "There was a problem reading the label of the answer. This can happen if the type of the label cannot be handled." + this, e);
                }
                rec = new DNSRecord.Service(domain, recordClass, unique, ttl, priority, weight, port, target);
                break;
            case TYPE_HINFO:
                StringBuffer buf = new StringBuffer();
                this.readUTF(buf, _off, len);
                int index = buf.indexOf(" ");
                String cpu = (index > 0 ? buf.substring(0, index) : buf.toString()).trim();
                String os = (index > 0 ? buf.substring(index + 1) : "").trim();
                rec = new DNSRecord.HostInformation(domain, recordClass, unique, ttl, cpu, os);
                break;
            case TYPE_OPT:
                DNSResultCode extendedResultCode = DNSResultCode.resultCodeForFlags(this.getFlags(), ttl);
                int version = (ttl & 0x00ff0000) >> 16;
                if (version == 0)
                {
                    _senderUDPPayload = recordClassIndex;
                    while (_off < end)
                    {
                        // Read RDData
                        int optionCodeInt = 0;
                        DNSOptionCode optionCode = null;
                        if (end - _off >= 2)
                        {
                            optionCodeInt = this.readUnsignedShort();
                            optionCode = DNSOptionCode.resultCodeForFlags(optionCodeInt);
                        }
                        else
                        {
                            logger.log(Level.WARNING, "There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        int optionLength = 0;
                        if (end - _off >= 2)
                        {
                            optionLength = readUnsignedShort();
                        }
                        else
                        {
                            logger.log(Level.WARNING, "There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        byte[] optiondata = new byte[0];
                        if (end - _off >= optionLength)
                        {
                            optiondata = this.readBytes(_off, optionLength);
                            _off = _off + optionLength;
                        }
                        //
                        if (DNSOptionCode.Unknown == optionCode)
                        {
                            logger.log(Level.WARNING, "There was an OPT answer. Not currently handled. Option code: " + optionCodeInt + " data: " + this._hexString(optiondata));
                        }
                        else
                        {
                            // We should really do something with those options.
                            switch (optionCode)
                            {
                                case Owner:
                                    // Valid length values are 8, 14, 18 and 20
                                    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                    // |Opt|Len|V|S|Primary MAC|Wakeup MAC | Password |
                                    // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                    //
                                    int ownerVersion = 0;
                                    int ownerSequence = 0;
                                    byte[] ownerPrimaryMacAddress = null;
                                    byte[] ownerWakeupMacAddress = null;
                                    byte[] ownerPassword = null;
                                    try
                                    {
                                        ownerVersion = optiondata[0];
                                        ownerSequence = optiondata[1];
                                        ownerPrimaryMacAddress = new byte[] { optiondata[2], optiondata[3], optiondata[4], optiondata[5], optiondata[6], optiondata[7] };
                                        ownerWakeupMacAddress = ownerPrimaryMacAddress;
                                        if (optiondata.length > 8)
                                        {
                                            // We have a wakeupMacAddress.
                                            ownerWakeupMacAddress = new byte[] { optiondata[8], optiondata[9], optiondata[10], optiondata[11], optiondata[12], optiondata[13] };
                                        }
                                        if (optiondata.length == 18)
                                        {
                                            // We have a short password.
                                            ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17] };
                                        }
                                        if (optiondata.length == 22)
                                        {
                                            // We have a long password.
                                            ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17], optiondata[18], optiondata[19], optiondata[20], optiondata[21] };
                                        }
                                    }
                                    catch (Exception exception)
                                    {
                                        logger.warning("Malformed OPT answer. Option code: Owner data: " + this._hexString(optiondata));
                                    }
                                    logger.info("Unhandled Owner OPT version: " + ownerVersion + " sequence: " + ownerSequence + " MAC address: " + this._hexString(ownerPrimaryMacAddress)
                                            + (ownerWakeupMacAddress != ownerPrimaryMacAddress ? " wakeup MAC address: " + this._hexString(ownerWakeupMacAddress) : "")
                                            + (ownerPassword != null ? " password: " + this._hexString(ownerPassword) : ""));
                                    break;
                                case LLQ:
                                case NSID:
                                case UL:
                                case Unknown:
                                    logger.log(Level.INFO, "There was an OPT answer. Option code: " + optionCode + " data: " + this._hexString(optiondata));
                                    break;
                            }
                        }
                    }
                }
                else
                {
                    logger.log(Level.WARNING, "There was an OPT answer. Wrong version number: " + version + " result code: " + extendedResultCode);
                }
                break;
            default:
                if (logger.isLoggable(Level.FINER))
                {
                    logger.finer("DNSIncoming() unknown type:" + type);
                }
                _off = end;
                break;
        }
        if (rec != null)
        {
            rec.setRecordSource(source);
        }
        _off = end;
        return rec;
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
        return (this.get(_off++) << 8) | this.get(_off++);
    }

    private int readInt() throws IOException
    {
        return (this.readUnsignedShort() << 16) | this.readUnsignedShort();
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
        if (len > 0)
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

        this._off = this._off + len + 1;
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
            switch (DNSLabel.labelForByte(len))
            {
                case Standard:
                    // buf.append("[" + off + "]");
                    this.readUTF(buf, off, len);
                    off += len;
                    buf.append('.');
                    break;
                case Compressed:
                    // buf.append("<" + (off - 1) + ">");
                    if (next < 0)
                    {
                        next = off + 1;
                    }
                    off = (DNSLabel.labelValue(len) << 8) | this.get(off++);
                    if (off >= first)
                    {
                        throw new IOException("bad domain name: possible circular name detected." + " name start: " + first + " bad offset: 0x" + Integer.toHexString(off));
                    }
                    first = off;
                    break;
                case Extended:
                    // int extendedLabelClass = DNSLabel.labelValue(len);
                    logger.severe("Extended label are not currently supported.");
                    break;
                case Unknown:
                default:
                    throw new IOException("unsupported dns label type: '" + Integer.toHexString(len & 0xC0) + "' at " + (off - 1));
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
        StringBuilder buf = new StringBuilder();
        buf.append(this.print());
        if (dump)
        {
            for (int off = 0, len = _packet.getLength(); off < len; off += 32)
            {
                int n = Math.min(32, len - off);
                if (off < 0x10)
                {
                    buf.append(' ');
                }
                if (off < 0x100)
                {
                    buf.append(' ');
                }
                if (off < 0x1000)
                {
                    buf.append(' ');
                }
                buf.append(Integer.toHexString(off));
                buf.append(':');
                int index = 0;
                for (index = 0; index < n; index++)
                {
                    if ((index % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    buf.append(Integer.toHexString((_data[off + index] & 0xF0) >> 4));
                    buf.append(Integer.toHexString((_data[off + index] & 0x0F) >> 0));
                }
                // for incomplete lines
                if (index < 32)
                {
                    for (int i = index; i < 32; i++)
                    {
                        if ((i % 8) == 0)
                        {
                            buf.append(' ');
                        }
                        buf.append("  ");
                    }
                }
                buf.append("    ");
                for (index = 0; index < n; index++)
                {
                    if ((index % 8) == 0)
                    {
                        buf.append(' ');
                    }
                    int ch = _data[off + index] & 0xFF;
                    buf.append(((ch > ' ') && (ch < 127)) ? (char) ch : '.');
                }
                buf.append("\n");

                // limit message size
                if (off + 32 >= 2048)
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
        buf.append(", length=");
        buf.append(_packet.getLength());
        buf.append(", id=0x");
        buf.append(Integer.toHexString(this.getId()));
        if (this.getFlags() != 0)
        {
            buf.append(", flags=0x");
            buf.append(Integer.toHexString(this.getFlags()));
            if ((this.getFlags() & DNSConstants.FLAGS_QR_RESPONSE) != 0)
            {
                buf.append(":r");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_AA) != 0)
            {
                buf.append(":aa");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_TC) != 0)
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
        if (this.getNumberOfQuestions() > 0)
        {
            buf.append("\nquestions:");
            for (DNSQuestion question : _questions)
            {
                buf.append("\n\t" + question);
            }
        }
        if (this.getNumberOfAnswers() > 0)
        {
            buf.append("\nanswers:");
            for (DNSRecord record : _answers)
            {
                buf.append("\n\t" + record);
            }
        }
        if (this.getNumberOfAuthorities() > 0)
        {
            buf.append("\nauthorities:");
            for (DNSRecord record : _authoritativeAnswers)
            {
                buf.append("\n\t" + record);
            }
        }
        if (this.getNumberOfAdditionals() > 0)
        {
            buf.append("\nadditionals:");
            for (DNSRecord record : _additionals)
            {
                buf.append("\n\t" + record);
            }
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
            this._questions.addAll(that.getQuestions());
            this._answers.addAll(that.getAnswers());
            this._authoritativeAnswers.addAll(that.getAuthorities());
            this._additionals.addAll(that.getAdditionals());
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

    /**
     * This will return the default UDP payload except if an OPT record was found with a different size.
     *
     * @return the senderUDPPayload
     */
    public int getSenderUDPPayload()
    {
        return this._senderUDPPayload;
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Returns a hex-string for printing
     *
     * @param bytes
     *
     * @return Returns a hex-string which can be used within a SQL expression
     */
    private String _hexString(byte[] bytes)
    {

        StringBuilder result = new StringBuilder(2 * bytes.length);

        for (int i = 0; i < bytes.length; i++)
        {
            int b = bytes[i] & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }

}
