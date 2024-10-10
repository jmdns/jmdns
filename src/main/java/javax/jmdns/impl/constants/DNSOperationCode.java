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
/**
 *
 */
package javax.jmdns.impl.constants;

/**
 * DNS operation code.
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSOperationCode {
    /**
     * Query [RFC1035]
     */
    Query("Query", 0),
    /**
     * IQuery (Inverse Query, Obsolete) [RFC3425]
     */
    IQuery("Inverse Query", 1),
    /**
     * Status [RFC1035]
     */
    Status("Status", 2),
    /**
     * Unassigned
     */
    Unassigned("Unassigned", 3),
    /**
     * Notify [RFC1996]
     */
    Notify("Notify", 4),
    /**
     * Update [RFC2136]
     */
    Update("Update", 5);

    /**
     * DNS RCode types are encoded on the last 4 bits
     */
    static final int OpCode_MASK = 0x7800;

    private final String externalName;

    private final int indexValue;

    DNSOperationCode(String name, int index) {
        externalName = name;
        indexValue = index;
    }

    /**
     * Return the string representation of this type
     *
     * @return String
     */
    public String externalName() {
        return externalName;
    }

    /**
     * Return the numeric value of this type
     * 
     * @return String
     */
    public int indexValue() {
        return indexValue;
    }

    /**
     * @param flags the flags to check
     * @return label
     */
    public static DNSOperationCode operationCodeForFlags(int flags) {
        int maskedIndex = (flags & OpCode_MASK) >> 11;
        for (DNSOperationCode operationCode : DNSOperationCode.values()) {
            if (operationCode.indexValue == maskedIndex) return operationCode;
        }
        return Unassigned;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}