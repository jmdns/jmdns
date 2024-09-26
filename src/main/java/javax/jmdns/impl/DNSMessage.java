/**
 *
 */
package javax.jmdns.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jmdns.impl.constants.DNSConstants;

/**
 * DNSMessage define a DNS message either incoming or outgoing.
 *
 * @author Werner Randelshofer, Rick Blair, Pierre Frisch
 */
public abstract class DNSMessage {

    private int                       _id;

    boolean                           _multicast;

    private int                       _flags;

    protected final List<DNSQuestion> _questions;

    protected final List<DNSRecord>   _answers;

    protected final List<DNSRecord>   _authoritativeAnswers;

    protected final List<DNSRecord>   _additionals;

    /**
     * @param flags
     * @param id
     * @param multicast
     */
    protected DNSMessage(int flags, int id, boolean multicast) {
        super();
        _flags = flags;
        _id = id;
        _multicast = multicast;
        _questions = Collections.synchronizedList(new LinkedList<>());
        _answers = Collections.synchronizedList(new LinkedList<>());
        _authoritativeAnswers = Collections.synchronizedList(new LinkedList<>());
        _additionals = Collections.synchronizedList(new LinkedList<>());
    }

    /**
     * @return message id
     */
    public int getId() {
        return (_multicast ? 0 : _id);
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id) {
        this._id = id;
    }

    /**
     * @return message flags
     */
    public int getFlags() {
        return _flags;
    }

    /**
     * @param flags
     *            the flags to set
     */
    public void setFlags(int flags) {
        this._flags = flags;
    }

    /**
     * @return true if multicast
     */
    public boolean isMulticast() {
        return _multicast;
    }

    /**
     * @return list of questions
     */
    public Collection<? extends DNSQuestion> getQuestions() {
        return _questions;
    }

    /**
     * @return number of questions in the message
     */
    public int getNumberOfQuestions() {
        return this.getQuestions().size();
    }

    public List<DNSRecord> getAllAnswers() {
        List<DNSRecord> aList = new ArrayList<>(_answers.size() + _authoritativeAnswers.size() + _additionals.size());
        aList.addAll(_answers);
        aList.addAll(_authoritativeAnswers);
        aList.addAll(_additionals);
        return aList;
    }

    /**
     * @return list of answers
     */
    public Collection<? extends DNSRecord> getAnswers() {
        return _answers;
    }

    /**
     * @return number of answers in the message
     */
    public int getNumberOfAnswers() {
        return this.getAnswers().size();
    }

    /**
     * @return list of authorities
     */
    public Collection<? extends DNSRecord> getAuthorities() {
        return _authoritativeAnswers;
    }

    /**
     * @return number of authorities in the message
     */
    public int getNumberOfAuthorities() {
        return this.getAuthorities().size();
    }

    /**
     * @return list of additional answers
     */
    public Collection<? extends DNSRecord> getAdditionals() {
        return _additionals;
    }

    /**
     * @return number of additional in the message
     */
    public int getNumberOfAdditionals() {
        return this.getAdditionals().size();
    }

    /**
     * Check is the response code is valid<br/>
     * The only valid value is zero all other values signify an error and the message must be ignored.
     *
     * @return true if the message has a valid response code.
     */
    public boolean isValidResponseCode() {
        return (_flags & DNSConstants.FLAGS_RCODE) == 0;
    }

    /**
     * Returns the operation code value. Currently only standard query 0 is valid.
     *
     * @return The operation code value.
     */
    public int getOperationCode() {
        return (_flags & DNSConstants.FLAGS_OPCODE) >> 11;
    }

    /**
     * Check if the message is truncated.
     *
     * @return true if the message was truncated
     */
    public boolean isTruncated() {
        return (_flags & DNSConstants.FLAGS_TC) != 0;
    }

    /**
     * Check if the message is an authoritative answer.
     *
     * @return true if the message is an authoritative answer
     */
    public boolean isAuthoritativeAnswer() {
        return (_flags & DNSConstants.FLAGS_AA) != 0;
    }

    /**
     * Check if the message is a query.
     *
     * @return true is the message is a query
     */
    public boolean isQuery() {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is a response.
     *
     * @return true is the message is a response
     */
    public boolean isResponse() {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_RESPONSE;
    }

    /**
     * Check if the message is empty
     *
     * @return true is the message is empty
     */
    public boolean isEmpty() {
        return (this.getNumberOfQuestions() + this.getNumberOfAnswers() + this.getNumberOfAuthorities() + this.getNumberOfAdditionals()) == 0;
    }

    /**
     * Debugging.
     */
    protected String print() {
        final StringBuilder sb = new StringBuilder(200);
        // statistics, print out the number of DNS entries
        appendStatistics(sb, "questions", _questions);
        appendStatistics(sb, "answers", _answers);
        appendStatistics(sb, "authorities", _authoritativeAnswers);
        appendStatistics(sb, "additionals", _additionals);
        // list all DNSEntries
        appendDNSEntries(sb, "questions", _questions);
        appendDNSEntries(sb, "answers", _answers);
        appendDNSEntries(sb, "authorities", _authoritativeAnswers);
        appendDNSEntries(sb, "additionals", _additionals);
        return sb.toString();
    }

    private <T extends DNSEntry> void appendStatistics(StringBuilder sb, String name, List<T> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        sb.append(", ");
        sb.append(name);
        sb.append("=");
        sb.append(entries.size());
    }

    private <T extends DNSEntry> void appendDNSEntries(StringBuilder sb, String name, List<T> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        sb.append("\n");
        sb.append(name);
        sb.append(":");
        for (DNSEntry entry : entries) {
            sb.append("\n\t");
            sb.append(entry);
        }
    }

    /**
     * Debugging.
     *
     * @param data
     * @return data dump
     */
    protected String print(byte[] data) {
        final StringBuilder sb = new StringBuilder(4000);
        for (int off = 0, len = data.length; off < len; off += 32) {
            int n = Math.min(32, len - off);
            if (off < 0x10) {
                sb.append(' ');
            }
            if (off < 0x100) {
                sb.append(' ');
            }
            if (off < 0x1000) {
                sb.append(' ');
            }
            sb.append(Integer.toHexString(off));
            sb.append(':');
            int index;
            for (index = 0; index < n; index++) {
                if ((index % 8) == 0) {
                    sb.append(' ');
                }
                sb.append(Integer.toHexString((data[off + index] & 0xF0) >> 4));
                sb.append(Integer.toHexString((data[off + index] & 0x0F)));
            }
            // for incomplete lines
            if (index < 32) {
                for (int i = index; i < 32; i++) {
                    if ((i % 8) == 0) {
                        sb.append(' ');
                    }
                    sb.append("  ");
                }
            }
            sb.append("    ");
            for (index = 0; index < n; index++) {
                if ((index % 8) == 0) {
                    sb.append(' ');
                }
                int ch = data[off + index] & 0xFF;
                sb.append(((ch > ' ') && (ch < 127)) ? (char) ch : '.');
            }
            sb.append("\n");

            // limit message size
            if (off + 32 >= 2048) {
                sb.append("....\n");
                break;
            }
        }
        return sb.toString();
    }

}