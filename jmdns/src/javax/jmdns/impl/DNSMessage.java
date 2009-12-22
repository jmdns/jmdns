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
 *
 */
public abstract class DNSMessage
{

    /**
     *
     */
    public final boolean MULTICAST = true;

    /**
     *
     */
    public final boolean UNICAST = false;

    // protected DatagramPacket _packet;
    // protected int _off;
    // protected int _len;
    // protected byte[] _data;

    int _id;

    boolean _multicast;

    int _flags;

    protected final List<DNSQuestion> _questions;

    protected final List<DNSRecord> _answers;

    protected final List<DNSRecord> _authoritativeAnswers;

    protected final List<DNSRecord> _additionals;

    /**
     * @param flags
     * @param id
     * @param multicast
     */
    protected DNSMessage(int flags, int id, boolean multicast)
    {
        super();
        _flags = flags;
        _id = id;
        _multicast = multicast;
        _questions = Collections.synchronizedList(new LinkedList<DNSQuestion>());
        _answers = Collections.synchronizedList(new LinkedList<DNSRecord>());
        _authoritativeAnswers = Collections.synchronizedList(new LinkedList<DNSRecord>());
        _additionals = Collections.synchronizedList(new LinkedList<DNSRecord>());
    }

    // public DatagramPacket getPacket() {
    // return _packet;
    // }
    //
    // public int getOffset() {
    // return _off;
    // }
    //
    // public int getLength() {
    // return _len;
    // }
    //
    // public byte[] getData() {
    // if ( _data == null ) _data = new byte[DNSConstants.MAX_MSG_TYPICAL];
    // return _data;
    // }

    /**
     * @return message id
     */
    public int getId()
    {
        return (_multicast ? 0 : _id);
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id)
    {
        this._id = id;
    }

    /**
     * @return message flags
     */
    public int getFlags()
    {
        return _flags;
    }

    /**
     * @param flags
     *            the flags to set
     */
    public void setFlags(int flags)
    {
        this._flags = flags;
    }

    /**
     * @return true if multicast
     */
    public boolean isMulticast()
    {
        return _multicast;
    }

    /**
     * @return list of questions
     */
    public Collection<? extends DNSQuestion> getQuestions()
    {
        return _questions;
    }

    /**
     * @return number of questions in the message
     */
    public int getNumberOfQuestions()
    {
        return this.getQuestions().size();
    }

    public Collection<? extends DNSRecord> getAllAnswers()
    {
        List<DNSRecord> aList = new ArrayList<DNSRecord>(_answers.size() + _authoritativeAnswers.size()
                + _additionals.size());
        aList.addAll(_answers);
        aList.addAll(_authoritativeAnswers);
        aList.addAll(_additionals);
        return aList;
    }

    /**
     * @return list of answers
     */
    public Collection<? extends DNSRecord> getAnswers()
    {
        return _answers;
    }

    /**
     * @return number of answers in the message
     */
    public int getNumberOfAnswers()
    {
        return this.getAnswers().size();
    }

    /**
     * @return list of authorities
     */
    public Collection<? extends DNSRecord> getAuthorities()
    {
        return _authoritativeAnswers;
    }

    /**
     * @return number of authorities in the message
     */
    public int getNumberOfAuthorities()
    {
        return this.getAuthorities().size();
    }

    /**
     * @return list of additional answers
     */
    public Collection<? extends DNSRecord> getAdditionals()
    {
        return _additionals;
    }

    /**
     * @return number of additional in the message
     */
    public int getNumberOfAdditionals()
    {
        return this.getAdditionals().size();
    }

    /**
     * Check if the message is truncated.
     *
     * @return true if the message was truncated
     */
    public boolean isTruncated()
    {
        return (_flags & DNSConstants.FLAGS_TC) != 0;
    }

    /**
     * Check if the message is a query.
     *
     * @return true is the message is a query
     */
    public boolean isQuery()
    {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_QUERY;
    }

    /**
     * Check if the message is a response.
     *
     * @return true is the message is a response
     */
    public boolean isResponse()
    {
        return (_flags & DNSConstants.FLAGS_QR_MASK) == DNSConstants.FLAGS_QR_RESPONSE;
    }

    /**
     * Check if the message is empty
     *
     * @return true is the message is empty
     */
    public boolean isEmpty()
    {
        return (this.getNumberOfQuestions() + this.getNumberOfAnswers() + this.getNumberOfAuthorities() + this
                .getNumberOfAdditionals()) == 0;
    }

}
