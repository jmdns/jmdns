//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL

package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * DNS entry with a name, type, and class. This is the base class for questions and records.
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Pierre Frisch, Rick Blair
 */
public abstract class DNSEntry
{
    // private static Logger logger = Logger.getLogger(DNSEntry.class.getName());
    String _key;

    String _name;

    DNSRecordType _type;

    DNSRecordClass _dnsClass;

    boolean _unique;

    /**
     * Create an entry.
     */
    DNSEntry(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique)
    {
        _name = name;
        _key = (name != null ? name.trim().toLowerCase() : null);
        _type = type;
        _dnsClass = recordClass;
        _unique = unique;
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     */
    @Override
    public boolean equals(Object obj)
    {
        boolean result = false;
        if (obj instanceof DNSEntry)
        {
            DNSEntry other = (DNSEntry) obj;
            result = this.getKey().equals(other.getKey()) && this.getRecordType().equals(other.getRecordType())
                    && _dnsClass == other.getRecordClass();
        }
        return result;
    }

    /**
     * Returns teh name of this entry
     *
     * @return name of this entry
     */
    public String getName()
    {
        return (_name != null ? _name : "");
    }

    /**
     * Returns the key for this entry. The key is the lower case name.
     *
     * @return key for this entry
     */
    public String getKey()
    {
        return (_key != null ? _key : "");
    }

    /**
     * @param value
     */
    public void setName(String value)
    {
        _key = (value != null ? value.trim().toLowerCase() : null);
        _name = value;
    }

    /**
     * @return record type
     */
    public DNSRecordType getRecordType()
    {
        return (_type != null ? _type : DNSRecordType.TYPE_IGNORE);
    }

    /**
     * @return record class
     */
    public DNSRecordClass getRecordClass()
    {
        return _dnsClass;
    }

    /**
     * @return true if unique
     */
    public boolean isUnique()
    {
        return _unique;
    }

    /**
     * Check if the record is expired.
     */
    abstract boolean isExpired(long now);

    /**
     * @param dout
     * @throws IOException
     */
    protected void toByteArray(DataOutputStream dout) throws IOException
    {
        dout.write(this.getName().getBytes("UTF8"));
        dout.writeShort(this.getRecordType().indexValue());
        dout.writeShort(this.getRecordClass().indexValue());
    }

    /**
     * Creates a byte array representation of this record. This is needed for tie-break tests according to
     * draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @return byte array representation
     */
    protected byte[] toByteArray()
    {
        try
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            this.toByteArray(dout);
            dout.close();
            return bout.toByteArray();
        }
        catch (IOException e)
        {
            throw new InternalError();
        }
    }

    /**
     * Does a lexicographic comparison of the byte array representation of this record and that record. This is needed
     * for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @param that
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     */
    public int compareTo(DNSEntry that)
    {
        byte[] thisBytes = this.toByteArray();
        byte[] thatBytes = that.toByteArray();
        for (int i = 0, n = Math.min(thisBytes.length, thatBytes.length); i < n; i++)
        {
            if (thisBytes[i] > thatBytes[i])
            {
                return 1;
            }
            else if (thisBytes[i] < thatBytes[i])
            {
                return -1;
            }
        }
        return thisBytes.length - thatBytes.length;
    }

    /**
     * Overriden, to return a value which is consistent with the value returned by equals(Object).
     */
    @Override
    public int hashCode()
    {
        return _name.hashCode() + this.getRecordType().indexValue() + this.getRecordClass().indexValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder aLog = new StringBuilder();
        aLog.append("[ " + this.getClass().getName());
        aLog.append(" [type: " + this.getRecordType());
        aLog.append(", class: " + this.getRecordClass());
        aLog.append((_unique ? "-unique," : ","));
        aLog.append(" name: " + _name);
        this.toString(aLog);
        aLog.append(" ]");
        return aLog.toString();
    }

    /**
     * @param aLog
     */
    public void toString(StringBuilder aLog)
    {
        aLog.append(super.toString());
    }

    public String toString(String hdr, String other)
    {
        return hdr + "[" + this.getRecordType() + "," + this.getRecordClass() + (_unique ? "-unique," : ",") + _name
                + ((other != null) ? "," + other + "]" : "]");
    }
}
