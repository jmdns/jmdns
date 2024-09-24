/**
 *
 */
package javax.jmdns.impl.constants;

/**
 * DNS option code.
 * 
 * @author Arthur van Hoff, Pierre Frisch, Rick Blair
 */
public enum DNSOptionCode {

    /**
     * Token
     */
    Unknown("Unknown", 65535),
    /**
     * Long-Lived Queries Option [<a href="http://files.dns-sd.org/draft-sekar-dns-llq.txt">...</a>]
     */
    LLQ("LLQ", 1),
    /**
     * Update Leases Option [<a href="http://files.dns-sd.org/draft-sekar-dns-ul.txt">...</a>]
     */
    UL("UL", 2),
    /**
     * Name Server Identifier Option [RFC5001]
     */
    NSID("NSID", 3),
    /**
     * Owner Option [draft-cheshire-edns0-owner-option]
     */
    Owner("Owner", 4);

    private final String _externalName;

    private final int    _index;

    DNSOptionCode(String name, int index) {
        _externalName = name;
        _index = index;
    }

    /**
     * Return the string representation of this type
     * 
     * @return String
     */
    public String externalName() {
        return _externalName;
    }

    /**
     * Return the numeric value of this type
     * 
     * @return String
     */
    public int indexValue() {
        return _index;
    }

    /**
     * @param optioncode the option code
     * @return label
     */
    public static DNSOptionCode resultCodeForFlags(int optioncode) {
        for (DNSOptionCode aCode : DNSOptionCode.values()) {
            if (aCode._index == optioncode) return aCode;
        }
        return Unknown;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}