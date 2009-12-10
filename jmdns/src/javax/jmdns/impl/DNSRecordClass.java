/**
 *
 */
package javax.jmdns.impl;

/**
 * DNS Record Class
 *
 * @version %I%, %G%
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSRecordClass
{
    /**
     * Final Static Internet
     */
    CLASS_IN("in", 1),
    /**
     * CSNET
     */
    CLASS_CS("cs", 2),
    /**
     * CHAOS
     */
    CLASS_CH("ch", 3),
    /**
     * Hesiod
     */
    CLASS_HS("hs", 4),
    /**
     * Used in DNS UPDATE [RFC 2136]
     */
    CLASS_NONE("none", 254),
    /**
     * Not a DNS class, but a DNS query class, meaning "all classes"
     */
    CLASS_ANY("any", 255);

    /**
     * Multicast DNS uses the bottom 15 bits to identify the record class...
     */
    final static int CLASS_MASK = 0x7FFF;

    /**
     * ... and the top bit indicates that all other cached records are now invalid
     */
    final static int CLASS_UNIQUE = 0x8000;

    /**
     *
     */
    public final static boolean UNIQUE = true;

    /**
     *
     */
    public final static boolean NOT_UNIQUE = false;

    String _externalName;

    int _index;

    DNSRecordClass(String name, int index)
    {
        _externalName = name;
        _index = index;
    }

    /**
     * Return the string representation of this type
     *
     * @return String
     */
    public String externalName()
    {
        return _externalName;
    }

    /**
     * Return the numeric value of this type
     *
     * @return String
     */
    public int indexValue()
    {
        return _index;
    }

    /**
     * @param name
     * @return class for name
     */
    public static DNSRecordClass classForName(String name)
    {
        if (name != null)
        {
            String aName = name.toLowerCase();
            for (DNSRecordClass aType : DNSRecordClass.values())
            {
                if (aType._externalName.equals(aName))
                    return aType;
            }
        }
        return null;
    }

    /**
     * @param index
     * @return class for name
     */
    public static DNSRecordClass classForIndex(int index)
    {
        int maskedIndex = index & CLASS_MASK;
        for (DNSRecordClass aType : DNSRecordClass.values())
        {
            if (aType._index == maskedIndex)
                return aType;
        }
        return null;
    }

    /**
     * @param classIndex
     * @return true it the record is unique
     */
    public static boolean isUnique(int classIndex)
    {
        return (classIndex & CLASS_UNIQUE) != 0;
    }

    @Override
    public String toString()
    {
        return this.name() + " index " + this.indexValue();
    }

}
