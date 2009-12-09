//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL


package javax.jmdns.impl;


/**
 * DNS entry with a name, type, and class. This is the base
 * class for questions and records.
 *
 * @version %I%, %G%
 * @author	Arthur van Hoff, Pierre Frisch, Rick Blair
 */
public class DNSEntry
{
    // private static Logger logger = Logger.getLogger(DNSEntry.class.getName());
    String _key;
    String _name;
    int _type;
    int _clazz;
    boolean _unique;

    /**
     * Create an entry.
     */
    DNSEntry(String name, int type, int clazz)
    {
        this._key = name.toLowerCase();
        this._name = name;
        this._type = type;
        this._clazz = clazz & DNSConstants.CLASS_MASK;
        this._unique = (clazz & DNSConstants.CLASS_UNIQUE) != 0;
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DNSEntry)
        {
            DNSEntry other = (DNSEntry) obj;
            return _name.equals(other._name) && _type == other._type && _clazz == other._clazz;
        }
        return false;
    }

    public String getName()
    {
        return _name;
    }

    public int getType()
    {
        return _type;
    }

    /**
     * Overriden, to return a value which is consistent with the value returned
     * by equals(Object).
     */
    @Override
    public int hashCode()
    {
        return _name.hashCode() + _type + _clazz;
    }

    /**
     * Get a string given a clazz.
     */
    static String getClazz(int clazz)
    {
        switch (clazz & DNSConstants.CLASS_MASK)
        {
            case DNSConstants.CLASS_IN:
                return "in";
            case DNSConstants.CLASS_CS:
                return "cs";
            case DNSConstants.CLASS_CH:
                return "ch";
            case DNSConstants.CLASS_HS:
                return "hs";
            case DNSConstants.CLASS_NONE:
                return "none";
            case DNSConstants.CLASS_ANY:
                return "any";
            default:
                return "?";
        }
    }

    /**
     * Get a string given a type.
     */
    static String getType(int type)
    {
        switch (type)
        {
            case DNSConstants.TYPE_A:
                return "a";
            case DNSConstants.TYPE_AAAA:
                return "aaaa";
            case DNSConstants.TYPE_NS:
                return "ns";
            case DNSConstants.TYPE_MD:
                return "md";
            case DNSConstants.TYPE_MF:
                return "mf";
            case DNSConstants.TYPE_CNAME:
                return "cname";
            case DNSConstants.TYPE_SOA:
                return "soa";
            case DNSConstants.TYPE_MB:
                return "mb";
            case DNSConstants.TYPE_MG:
                return "mg";
            case DNSConstants.TYPE_MR:
                return "mr";
            case DNSConstants.TYPE_NULL:
                return "null";
            case DNSConstants.TYPE_WKS:
                return "wks";
            case DNSConstants.TYPE_PTR:
                return "ptr";
            case DNSConstants.TYPE_HINFO:
                return "hinfo";
            case DNSConstants.TYPE_MINFO:
                return "minfo";
            case DNSConstants.TYPE_MX:
                return "mx";
            case DNSConstants.TYPE_TXT:
                return "txt";
            case DNSConstants.TYPE_SRV:
                return "srv";
            case DNSConstants.TYPE_ANY:
                return "any";
            default:
                return "?";
        }
    }

    public String toString(String hdr, String other)
    {
        return hdr + "[" + getType(_type) + "," + getClazz(_clazz) + (_unique ? "-unique," : ",") + _name + ((other != null) ? "," + other + "]" : "]");
    }
}
