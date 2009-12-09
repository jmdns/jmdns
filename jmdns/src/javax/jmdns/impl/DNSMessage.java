/**
 *
 */
package javax.jmdns.impl;

/**
 *
 */
public abstract class DNSMessage
{

    protected int _id;
    protected int _flags;

    protected int _off;
    protected int _len;
    protected byte _data[];

    /**
     *
     */
    protected DNSMessage()
    {
        super();
    }

    /**
     * @return the id
     */
    public int getId()
    {
        return this._id;
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
     * @return the flags
     */
    public int getFlags()
    {
        return this._flags;
    }

    /**
     * @param flags
     *            the flags to set
     */
    public void setFlags(int flags)
    {
        this._flags = flags;
    }

}
