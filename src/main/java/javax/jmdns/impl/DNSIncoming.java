// /Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.constants.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse an incoming DNS message into its components.
 *
 * @author Arthur van Hoff, Werner Randelshofer, Pierre Frisch, Daniel Bobbert
 */
public final class DNSIncoming extends DNSMessage {
    private static Logger logger                                = LoggerFactory.getLogger(DNSIncoming.class);

    // This is a hack to handle a bug in the BonjourConformanceTest
    // It is sending out target strings that don't follow the "domain name" format.
    public static boolean USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET = true;

    public static class MessageInputStream extends ByteArrayInputStream {
        final Map<Integer, String> _names;

        public MessageInputStream(byte[] buffer, int length) {
            this(buffer, 0, length);
        }

        /**
         * @param buffer
         * @param offset
         * @param length
         */
        public MessageInputStream(byte[] buffer, int offset, int length) {
            super(buffer, offset, length);
            _names = new HashMap<Integer, String>();
        }

        public int readByte() {
            return this.read();
        }

        public int readUnsignedByte() {
            return (this.read() & 0xFF);
        }

        public int readUnsignedShort() {
            return (this.readUnsignedByte() << 8) | this.readUnsignedByte();
        }

        public int readInt() {
            return (this.readUnsignedShort() << 16) | this.readUnsignedShort();
        }

        public byte[] readBytes(int len) {
            byte bytes[] = new byte[len];
            this.read(bytes, 0, len);
            return bytes;
        }

        public String readUTF(int len) {
            final StringBuilder sb = new StringBuilder(len);
            for (int index = 0; index < len; index++) {
                int ch = this.readUnsignedByte();
                switch (ch >> 4) {
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
                        ch = ((ch & 0x1F) << 6) | (this.readUnsignedByte() & 0x3F);
                        index++;
                        break;
                    case 14:
                        // 1110 xxxx 10xx xxxx 10xx xxxx
                        ch = ((ch & 0x0f) << 12) | ((this.readUnsignedByte() & 0x3F) << 6) | (this.readUnsignedByte() & 0x3F);
                        index++;
                        index++;
                        break;
                    default:
                        // 10xx xxxx, 1111 xxxx
                        ch = ((ch & 0x3F) << 4) | (this.readUnsignedByte() & 0x0f);
                        index++;
                        break;
                }
                sb.append((char) ch);
            }
            return sb.toString();
        }

        protected synchronized int peek() {
            return (pos < count) ? (buf[pos] & 0xff) : -1;
        }

        public String readName() {
            Map<Integer, Integer> names = new HashMap<Integer, Integer>();
            final StringBuilder sb = new StringBuilder();
            boolean finished = false;
            while (!finished) {
                int len = this.readUnsignedByte();
                if (len == 0) {
                    finished = true;
                    break;
                }
                switch (DNSLabel.labelForByte(len)) {
                    case Standard:
                        int startAt = sb.length();
                        int offset = pos - 1;
                        String label = this.readUTF(len);
                        sb.append(label).append(".");
                        names.put(offset, startAt);
                        break;
                    case Compressed:
                        int index = (DNSLabel.labelValue(len) << 8) | this.readUnsignedByte();
                        String compressedLabel = _names.get(index);
                        if (compressedLabel == null) {
                            logger.warn("Bad domain name: possible circular name detected. Bad offset: 0x{} at 0x{}",
                                    Integer.toHexString(index),
                                    Integer.toHexString(pos - 2)
                                    );
                            compressedLabel = "";
                        }
                        sb.append(compressedLabel);
                        finished = true;
                        break;
                    case Extended:
                        // int extendedLabelClass = DNSLabel.labelValue(len);
                        logger.debug("Extended label are not currently supported.");
                        break;
                    case Unknown:
                    default:
                        logger.warn("Unsupported DNS label type: '{}'", Integer.toHexString(len & 0xC0) );
                }
            }
            String name = sb.toString();
            names.forEach((index, startAt) -> _names.put(index, name.substring(startAt)));
            return name;
        }

        public String readNonNameString() {
            int len = this.readUnsignedByte();
            return this.readUTF(len);
        }

    }

    private final DatagramPacket     _packet;

    private final long               _receivedTime;

    private final MessageInputStream _messageInputStream;

    private int                      _senderUDPPayload;

    /**
     * Parse a message from a datagram packet.
     *
     * @param packet
     * @exception IOException
     */
    public DNSIncoming(DatagramPacket packet) throws IOException {
        super(0, 0, packet.getPort() == DNSConstants.MDNS_PORT);
        this._packet = packet;
        InetAddress source = packet.getAddress();
        this._messageInputStream = new MessageInputStream(packet.getData(), packet.getLength());
        this._receivedTime = System.currentTimeMillis();
        this._senderUDPPayload = DNSConstants.MAX_MSG_TYPICAL;

        try {
            this.setId(_messageInputStream.readUnsignedShort());
            this.setFlags(_messageInputStream.readUnsignedShort());
            if (this.getOperationCode() > 0) {
                throw new IOException("Received a message with a non standard operation code. Currently unsupported in the specification.");
            }
            int numQuestions = _messageInputStream.readUnsignedShort();
            int numAnswers = _messageInputStream.readUnsignedShort();
            int numAuthorities = _messageInputStream.readUnsignedShort();
            int numAdditionals = _messageInputStream.readUnsignedShort();

            logger.debug("DNSIncoming() questions:{} answers:{} authorities:{} additionals:{}",
                    numQuestions,
                    numAnswers,
                    numAuthorities,
                    numAdditionals
            );

            // We need some sanity checks
            // A question is at least 5 bytes and answer 11 so check what we have

            if ((numQuestions * 5 + (numAnswers + numAuthorities + numAdditionals) * 11) > packet.getLength()) {
                throw new IOException("questions:" + numQuestions + " answers:" + numAnswers + " authorities:" + numAuthorities + " additionals:" + numAdditionals);
            }

            // parse questions
            if (numQuestions > 0) {
                for (int i = 0; i < numQuestions; i++) {
                    _questions.add(this.readQuestion());
                }
            }

            // parse answers
            if (numAnswers > 0) {
                for (int i = 0; i < numAnswers; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _answers.add(rec);
                    }
                }
            }

            if (numAuthorities > 0) {
                for (int i = 0; i < numAuthorities; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _authoritativeAnswers.add(rec);
                    }
                }
            }

            if (numAdditionals > 0) {
                for (int i = 0; i < numAdditionals; i++) {
                    DNSRecord rec = this.readAnswer(source);
                    if (rec != null) {
                        // Add a record, if we were able to create one.
                        _additionals.add(rec);
                    }
                }
            }
            // We should have drained the entire stream by now
            if (_messageInputStream.available() > 0) {
                throw new IOException("Received a message with the wrong length.");
            }
        } catch (Exception e) {
            logger.warn("DNSIncoming() dump " + print(true) + "\n exception ", e);
            // This ugly but some JVM don't implement the cause on IOException
            IOException ioe = new IOException("DNSIncoming corrupted message");
            ioe.initCause(e);
            throw ioe;
        } finally {
            try {
                _messageInputStream.close();
            } catch (Exception e) { 
            	logger.warn("MessageInputStream close error");
            }
        }
    }

    private DNSIncoming(int flags, int id, boolean multicast, DatagramPacket packet, long receivedTime) {
        super(flags, id, multicast);
        this._packet = packet;
        this._messageInputStream = new MessageInputStream(packet.getData(), packet.getLength());
        this._receivedTime = receivedTime;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public DNSIncoming clone() {
        DNSIncoming in = new DNSIncoming(this.getFlags(), this.getId(), this.isMulticast(), this._packet, this._receivedTime);
        in._senderUDPPayload = this._senderUDPPayload;
        in._questions.addAll(this._questions);
        in._answers.addAll(this._answers);
        in._authoritativeAnswers.addAll(this._authoritativeAnswers);
        in._additionals.addAll(this._additionals);
        return in;
    }

    private DNSQuestion readQuestion() {
        String domain = _messageInputStream.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(_messageInputStream.readUnsignedShort());
        if (type == DNSRecordType.TYPE_IGNORE) {
            logger.warn("Could not find record type: {}", this.print(true));
        }
        int recordClassIndex = _messageInputStream.readUnsignedShort();
        DNSRecordClass recordClass = DNSRecordClass.classForIndex(recordClassIndex);
        boolean unique = recordClass.isUnique(recordClassIndex);
        return DNSQuestion.newQuestion(domain, type, recordClass, unique);
    }

    private DNSRecord readAnswer(InetAddress source) {
        String domain = _messageInputStream.readName();
        DNSRecordType type = DNSRecordType.typeForIndex(_messageInputStream.readUnsignedShort());
        if (type == DNSRecordType.TYPE_IGNORE) {
            logger.warn("Could not find record type. domain: {}\n{}", domain, this.print(true));
        }
        int recordClassIndex = _messageInputStream.readUnsignedShort();
        DNSRecordClass recordClass = (type == DNSRecordType.TYPE_OPT ? DNSRecordClass.CLASS_UNKNOWN : DNSRecordClass.classForIndex(recordClassIndex));
        if ((recordClass == DNSRecordClass.CLASS_UNKNOWN) && (type != DNSRecordType.TYPE_OPT)) {
            logger.warn("Could not find record class. domain: {} type: {}\n{}", domain, type, this.print(true));
        }
        boolean unique = recordClass.isUnique(recordClassIndex);
        int ttl = _messageInputStream.readInt();
        int len = _messageInputStream.readUnsignedShort();
        DNSRecord rec = null;

        switch (type) {
            case TYPE_A: // IPv4
                /*
                 * 2019-04-04
                 * This len check here is a workaround for a bug likely caused by the firmware in Grandstream door-bell camera.
                 * More details here:
                 * https://partnerconnect.grandstream.com/users/show_ticket/168746
                 * https://github.com/jmdns/jmdns/issues/186
                 * 
                 * The problem was camera GDS3710 was responding with DNS message which had Type A but len 2 rather than 4
                 * This caused CPU spinning and OOM issues.
                 * Here we simply drop the answer if it has invalid length.
                 */
                if (len == 4)
                    rec = new DNSRecord.IPv4Address(domain, recordClass, unique, ttl, _messageInputStream.readBytes(len));
                else
                    _messageInputStream.skip(len);
                break;
            case TYPE_AAAA: // IPv6
                byte[] incomingBytes =  _messageInputStream.readBytes(len);
                if (isIPv4MappedIPv6Address(incomingBytes)) {
                    logger.warn("AAAA record with IPv4-mapped address for {}", domain);
                } else {
                    rec = new DNSRecord.IPv6Address(domain, recordClass, unique, ttl, incomingBytes);
                }
                break;
            case TYPE_CNAME:
            case TYPE_PTR:
                String service = "";
                service = _messageInputStream.readName();
                if (service.length() > 0) {
                    rec = new DNSRecord.Pointer(domain, recordClass, unique, ttl, service);
                } else {
                    logger.warn("PTR record of class: {}, there was a problem reading the service name of the answer for domain: {}", recordClass, domain);
                }
                break;
            case TYPE_TXT:
                rec = new DNSRecord.Text(domain, recordClass, unique, ttl, _messageInputStream.readBytes(len));
                break;
            case TYPE_SRV:
                int priority = _messageInputStream.readUnsignedShort();
                int weight = _messageInputStream.readUnsignedShort();
                int port = _messageInputStream.readUnsignedShort();
                String target = "";
                // This is a hack to handle a bug in the BonjourConformanceTest
                // It is sending out target strings that don't follow the "domain name" format.
                if (USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET) {
                    target = _messageInputStream.readName();
                } else {
                    // [PJYF Nov 13 2010] Do we still need this? This looks really bad. All label are supposed to start by a length.
                    target = _messageInputStream.readNonNameString();
                }
                rec = new DNSRecord.Service(domain, recordClass, unique, ttl, priority, weight, port, target);
                break;
            case TYPE_HINFO:
                final StringBuilder sb = new StringBuilder();
                sb.append(_messageInputStream.readUTF(len));
                int index = sb.indexOf(" ");
                String cpu = (index > 0 ? sb.substring(0, index) : sb.toString()).trim();
                String os = (index > 0 ? sb.substring(index + 1) : "").trim();
                rec = new DNSRecord.HostInformation(domain, recordClass, unique, ttl, cpu, os);
                break;
            case TYPE_OPT:
                DNSResultCode extendedResultCode = DNSResultCode.resultCodeForFlags(this.getFlags(), ttl);
                int version = (ttl & 0x00ff0000) >> 16;
                if (version == 0) {
                    _senderUDPPayload = recordClassIndex;
                    while (_messageInputStream.available() > 0) {
                        // Read RDData
                        int optionCodeInt = 0;
                        DNSOptionCode optionCode = null;
                        if (_messageInputStream.available() >= 2) {
                            optionCodeInt = _messageInputStream.readUnsignedShort();
                            optionCode = DNSOptionCode.resultCodeForFlags(optionCodeInt);
                        } else {
                            logger.warn("There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        int optionLength = 0;
                        if (_messageInputStream.available() >= 2) {
                            optionLength = _messageInputStream.readUnsignedShort();
                        } else {
                            logger.warn("There was a problem reading the OPT record. Ignoring.");
                            break;
                        }
                        byte[] optiondata = new byte[0];
                        if (_messageInputStream.available() >= optionLength) {
                            optiondata = _messageInputStream.readBytes(optionLength);
                        }
                        //
                        // We should really do something with those options.
                        switch (optionCode) {
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
                                try {
                                    ownerVersion = optiondata[0];
                                    ownerSequence = optiondata[1];
                                    ownerPrimaryMacAddress = new byte[] { optiondata[2], optiondata[3], optiondata[4], optiondata[5], optiondata[6], optiondata[7] };
                                    ownerWakeupMacAddress = ownerPrimaryMacAddress;
                                    if (optiondata.length > 8) {
                                        // We have a wakeupMacAddress.
                                        ownerWakeupMacAddress = new byte[] { optiondata[8], optiondata[9], optiondata[10], optiondata[11], optiondata[12], optiondata[13] };
                                    }
                                    if (optiondata.length == 18) {
                                        // We have a short password.
                                        ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17] };
                                    }
                                    if (optiondata.length == 22) {
                                        // We have a long password.
                                        ownerPassword = new byte[] { optiondata[14], optiondata[15], optiondata[16], optiondata[17], optiondata[18], optiondata[19], optiondata[20], optiondata[21] };
                                    }
                                } catch (Exception exception) {
                                    logger.warn("Malformed OPT answer. Option code: Owner data: {}", this._hexString(optiondata));
                                }
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Unhandled Owner OPT version: {} sequence: {} MAC address: {} {}{} {}{}",
                                            ownerVersion,
                                            ownerSequence,
                                            this._hexString(ownerPrimaryMacAddress),
                                            (ownerWakeupMacAddress != ownerPrimaryMacAddress ? " wakeup MAC address: " : ""),
                                            (ownerWakeupMacAddress != ownerPrimaryMacAddress ? this._hexString(ownerWakeupMacAddress) : ""),
                                            (ownerPassword != null ? " password: ": ""),
                                            (ownerPassword != null ? this._hexString(ownerPassword) : "")
                                    );
                                }
                                break;
                            case LLQ:
                            case NSID:
                            case UL:
                                if (logger.isDebugEnabled()) {
                                    logger.debug("There was an OPT answer. Option code: {} data: {}", optionCode, this._hexString(optiondata));
                                }
                                break;
                            case Unknown:
                                if (optionCodeInt >= 65001 && optionCodeInt <= 65534) {
                                     // RFC 6891 defines this range as used for experimental/local purposes.
                                    logger.debug("There was an OPT answer using an experimental/local option code: {} data: {}", optionCodeInt, this._hexString(optiondata));
                                } else {
                                    logger.warn("There was an OPT answer. Not currently handled. Option code: {} data: {}", optionCodeInt, this._hexString(optiondata));
                                }
                                break;
                            default:
                                // This is to keep the compiler happy.
                                break;
                        }
                    }
                } else {
                    logger.warn("There was an OPT answer. Wrong version number: {} result code: {}", version, extendedResultCode);
                }
                break;
            default:
                    logger.debug("DNSIncoming() unknown type: {}", type);
                _messageInputStream.skip(len);
                break;
        }
        if (rec != null) {
            rec.setRecordSource(source);
        }
        return rec;
    }

    private boolean isIPv4MappedIPv6Address(byte[] addr) {
        return (addr[0] == 0x00) && (addr[1] == 0x00) &&
                (addr[2] == 0x00) && (addr[3] == 0x00) &&
                (addr[4] == 0x00) && (addr[5] == 0x00) &&
                (addr[6] == 0x00) && (addr[7] == 0x00) &&
                (addr[8] == 0x00) && (addr[9] == 0x00) &&
                (addr[10] == (byte) 0xff) &&
                (addr[11] == (byte) 0xff);
    }

    /**
     * Debugging.
     */
    String print(boolean dump) {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.print());
        if (dump) {
            byte[] data = new byte[_packet.getLength()];
            System.arraycopy(_packet.getData(), 0, data, 0, data.length);
            sb.append(this.print(data));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(isQuery() ? "dns[query," : "dns[response,");
        if (_packet.getAddress() != null) {
            sb.append(_packet.getAddress().getHostAddress());
        }
        sb.append(':');
        sb.append(_packet.getPort());
        sb.append(", length=");
        sb.append(_packet.getLength());
        sb.append(", id=0x");
        sb.append(Integer.toHexString(this.getId()));
        if (this.getFlags() != 0) {
            sb.append(", flags=0x");
            sb.append(Integer.toHexString(this.getFlags()));
            if ((this.getFlags() & DNSConstants.FLAGS_QR_RESPONSE) != 0) {
                sb.append(":r");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_AA) != 0) {
                sb.append(":aa");
            }
            if ((this.getFlags() & DNSConstants.FLAGS_TC) != 0) {
                sb.append(":tc");
            }
        }
        if (this.getNumberOfQuestions() > 0) {
            sb.append(", questions=");
            sb.append(this.getNumberOfQuestions());
        }
        if (this.getNumberOfAnswers() > 0) {
            sb.append(", answers=");
            sb.append(this.getNumberOfAnswers());
        }
        if (this.getNumberOfAuthorities() > 0) {
            sb.append(", authorities=");
            sb.append(this.getNumberOfAuthorities());
        }
        if (this.getNumberOfAdditionals() > 0) {
            sb.append(", additionals=");
            sb.append(this.getNumberOfAdditionals());
        }
        if (this.getNumberOfQuestions() > 0) {
            sb.append("\nquestions:");
            for (DNSQuestion question : _questions) {
                sb.append("\n\t");
                sb.append(question);
            }
        }
        if (this.getNumberOfAnswers() > 0) {
            sb.append("\nanswers:");
            for (DNSRecord record : _answers) {
                sb.append("\n\t");
                sb.append(record);
            }
        }
        if (this.getNumberOfAuthorities() > 0) {
            sb.append("\nauthorities:");
            for (DNSRecord record : _authoritativeAnswers) {
                sb.append("\n\t");
                sb.append(record);
            }
        }
        if (this.getNumberOfAdditionals() > 0) {
            sb.append("\nadditionals:");
            for (DNSRecord record : _additionals) {
                sb.append("\n\t");
                sb.append(record);
            }
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Appends answers to this Incoming.
     *
     * @exception IllegalArgumentException
     *                If not a query or if Truncated.
     */
    void append(DNSIncoming that) {
        if (this.isQuery() && this.isTruncated() && that.isQuery()) {
            this._questions.addAll(that.getQuestions());
            this._answers.addAll(that.getAnswers());
            this._authoritativeAnswers.addAll(that.getAuthorities());
            this._additionals.addAll(that.getAdditionals());
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int elapseSinceArrival() {
        return (int) (System.currentTimeMillis() - _receivedTime);
    }

    /**
     * This will return the default UDP payload except if an OPT record was found with a different size.
     *
     * @return the senderUDPPayload
     */
    public int getSenderUDPPayload() {
        return this._senderUDPPayload;
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    /**
     * Returns a hex-string for printing
     *
     * @param bytes
     * @return Returns a hex-string which can be used within a SQL expression
     */
    private String _hexString(byte[] bytes) {

        final StringBuilder result = new StringBuilder(2 * bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }

}
