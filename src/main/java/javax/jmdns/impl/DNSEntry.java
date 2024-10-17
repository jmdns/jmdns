/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package javax.jmdns.impl;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.jmdns.ServiceInfo.Fields;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * DNS entry with a name, type, and class. This is the base class for questions and records.
 *
 * @author Arthur van Hoff, Pierre Frisch, Rick Blair
 */
public abstract class DNSEntry {
    // private static Logger logger = LoggerFactory.getLogger(DNSEntry.class);
    private final String         _key;

    private final String         _name;

    private final String         _type;

    private final DNSRecordType  _recordType;

    private final DNSRecordClass _dnsClass;

    private final boolean        _unique;

    final Map<Fields, String>    _qualifiedNameMap;

    /**
     * Create an entry.
     */
    DNSEntry(String name, DNSRecordType recordType, DNSRecordClass recordClass, boolean unique) {
        _name = name;
        // _key = (name != null ? name.trim().toLowerCase() : null);
        _recordType = recordType;
        _dnsClass = recordClass;
        _unique = unique;
        _qualifiedNameMap = ServiceTypeDecoder.decodeQualifiedNameMapForType(this.getName());
        String domain = _qualifiedNameMap.get(Fields.Domain);
        String protocol = _qualifiedNameMap.get(Fields.Protocol);
        String application = _qualifiedNameMap.get(Fields.Application);
        String instance = _qualifiedNameMap.get(Fields.Instance).toLowerCase();
        _type = buildType(application, protocol, domain);
        _key = (!instance.isEmpty() ? instance + "." + _type : _type).toLowerCase();
    }

    private String buildType(String application, String protocol, String domain) {
        StringBuilder type = new StringBuilder();

        if (application != null && !application.isEmpty()) {
            type.append('_').append(application).append('.');
        }

        if (protocol != null && !protocol.isEmpty()) {
            type.append('_').append(protocol).append('.');
        }

        if (domain != null && !domain.isEmpty()) {
            type.append(domain).append('.');
        }

        if (type.length() == 0) {
            return ".";
        }

        return type.toString();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof DNSEntry) {
            DNSEntry other = (DNSEntry) obj;
            result = this.getKey().equals(other.getKey()) && this.getRecordType().equals(other.getRecordType()) && this.getRecordClass() == other.getRecordClass();
        }
        return result;
    }

    /**
     * Check if two entries have exactly the same name, type, and class.
     *
     * @param entry
     * @return <code>true</code> if the two entries have are for the same record, <code>false</code> otherwise
     */
    public boolean isSameEntry(DNSEntry entry) {
        return this.getKey().equals(entry.getKey()) && this.matchRecordType(entry.getRecordType()) && this.matchRecordClass(entry.getRecordClass());
    }

    /**
     * Check if two entries have the same subtype.
     *
     * @param other
     * @return <code>true</code> if the two entries have are for the same subtype, <code>false</code> otherwise
     */
    public boolean sameSubtype(DNSEntry other) {
        return this.getSubtype().equals(other.getSubtype());
    }

    /**
     * Check if the requested record class match the current record class
     *
     * @param recordClass
     * @return <code>true</code> if the two entries have compatible class, <code>false</code> otherwise
     */
    public boolean matchRecordClass(DNSRecordClass recordClass) {
        return (DNSRecordClass.CLASS_ANY == recordClass) || (DNSRecordClass.CLASS_ANY == this.getRecordClass()) || this.getRecordClass().equals(recordClass);
    }

    /**
     * Check if the requested record type match the current record type
     *
     * @param recordType
     * @return <code>true</code> if the two entries have compatible type, <code>false</code> otherwise
     */
    public boolean matchRecordType(DNSRecordType recordType) {
        return this.getRecordType().equals(recordType);
    }

    /**
     * Returns the subtype of this entry
     *
     * @return subtype of this entry
     */
    public String getSubtype() {
        String subtype = this.getQualifiedNameMap().get(Fields.Subtype);
        return (subtype != null ? subtype : "");
    }

    /**
     * Returns the name of this entry
     *
     * @return name of this entry
     */
    public String getName() {
        return (_name != null ? _name : "");
    }

    /**
     * @return the type
     */
    public String getType() {
        return (_type != null ? _type : "");
    }

    /**
     * Returns the key for this entry. The key is the lower case name.
     *
     * @return key for this entry
     */
    public String getKey() {
        return (_key != null ? _key : "");
    }

    /**
     * @return record type
     */
    public DNSRecordType getRecordType() {
        return (_recordType != null ? _recordType : DNSRecordType.TYPE_IGNORE);
    }

    /**
     * @return record class
     */
    public DNSRecordClass getRecordClass() {
        return (_dnsClass != null ? _dnsClass : DNSRecordClass.CLASS_UNKNOWN);
    }

    /**
     * @return true if unique
     */
    public boolean isUnique() {
        return _unique;
    }

    public Map<Fields, String> getQualifiedNameMap() {
        return Collections.unmodifiableMap(_qualifiedNameMap);
    }

    public boolean isServicesDiscoveryMetaQuery() {
        return _qualifiedNameMap.get(Fields.Application).equals("dns-sd") && _qualifiedNameMap.get(Fields.Instance).equals("_services");
    }

    public boolean isDomainDiscoveryQuery() {
        // b._dns-sd._udp.<domain>.
        // db._dns-sd._udp.<domain>.
        // r._dns-sd._udp.<domain>.
        // dr._dns-sd._udp.<domain>.
        // lb._dns-sd._udp.<domain>.

        if (_qualifiedNameMap.get(Fields.Application).equals("dns-sd")) {
            String name = _qualifiedNameMap.get(Fields.Instance);
            return "b".equals(name) || "db".equals(name) || "r".equals(name) || "dr".equals(name) || "lb".equals(name);
        }
        return false;
    }

    public boolean isReverseLookup() {
        return this.isV4ReverseLookup() || this.isV6ReverseLookup();
    }

    public boolean isV4ReverseLookup() {
        return _qualifiedNameMap.get(Fields.Domain).endsWith("in-addr.arpa");
    }

    public boolean isV6ReverseLookup() {
        return _qualifiedNameMap.get(Fields.Domain).endsWith("ip6.arpa");
    }

    /**
     * Check if the record is stale, i.e. it has outlived more than half of its TTL.
     *
     * @param now
     *            update date
     * @return <code>true</code> is the record is stale, <code>false</code> otherwise.
     */
    public abstract boolean isStale(long now);

    /**
     * Check if the record is expired.
     *
     * @param now
     *            update date
     * @return <code>true</code> is the record is expired, <code>false</code> otherwise.
     */
    public abstract boolean isExpired(long now);

    /**
     * Check that 2 entries are of the same class.
     *
     * @param entry
     * @return <code>true</code> is the two class are the same, <code>false</code> otherwise.
     */
    public boolean isSameRecordClass(DNSEntry entry) {
        return (entry != null) && (entry.getRecordClass() == this.getRecordClass());
    }

    /**
     * Check that 2 entries are of the same type.
     *
     * @param entry
     * @return <code>true</code> is the two type are the same, <code>false</code> otherwise.
     */
    public boolean isSameType(DNSEntry entry) {
        return (entry != null) && (entry.getRecordType() == this.getRecordType());
    }

    /**
     * @param dout
     * @exception IOException
     */
    protected void toByteArray(DataOutputStream dout) throws IOException {
        dout.write(this.getName().getBytes(StandardCharsets.UTF_8));
        dout.writeShort(this.getRecordType().indexValue());
        dout.writeShort(this.getRecordClass().indexValue());
    }

    /**
     * Creates a byte array representation of this record. This is needed for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @return byte array representation
     */
    protected byte[] toByteArray() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            this.toByteArray(dout);
            dout.close();
            return bout.toByteArray();
        } catch (IOException e) {
            throw new InternalError();
        }
    }

    /**
     * Does a lexicographic comparison of the byte array representation of this record and that record. This is needed for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
     *
     * @param that
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    public int compareTo(DNSEntry that) {
        byte[] thisBytes = this.toByteArray();
        byte[] thatBytes = that.toByteArray();
        for (int i = 0, n = Math.min(thisBytes.length, thatBytes.length); i < n; i++) {
            if (thisBytes[i] > thatBytes[i]) {
                return 1;
            } else if (thisBytes[i] < thatBytes[i]) {
                return -1;
            }
        }
        return thisBytes.length - thatBytes.length;
    }

    /**
     * Overriden, to return a value which is consistent with the value returned by equals(Object).
     */
    @Override
    public int hashCode() {
        return this.getKey().hashCode() + this.getRecordType().indexValue() + this.getRecordClass().indexValue();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(200);
        sb.append('[').append(this.getClass().getSimpleName()).append('@').append(System.identityHashCode(this));
        sb.append(" type: ").append(this.getRecordType());
        sb.append(", class: ").append(this.getRecordClass());
        sb.append((_unique ? "-unique," : ","));
        sb.append(" name: ").append( _name);
        this.toString(sb);
        sb.append(']');

        return sb.toString();
    }

    /**
     * @param sb
     */
    protected void toString(final StringBuilder sb) {
        // Stub
    }

}
