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
package javax.jmdns.impl.constants;

/**
 * DNS label.
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSLabel {
    /**
     * This is unallocated.
     */
    Unknown("", 0x80),
    /**
     * Standard label [RFC 1035]
     */
    Standard("standard label", 0x00),
    /**
     * Compressed label [RFC 1035]
     */
    Compressed("compressed label", 0xC0),
    /**
     * Extended label [RFC 2671]
     */
    Extended("extended label", 0x40);

    /**
     * DNS label types are encoded on the first 2 bits
     */
    static final int LABEL_MASK = 0xC0;
    static final int LABEL_NOT_MASK = 0x3F;
    private final String externalName;
    private final int indexValue;

    DNSLabel(String name, int index) {
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
     * @param index index of the label
     * @return label
     */
    public static DNSLabel labelForByte(int index) {
        int maskedIndex = index & LABEL_MASK;
        for (DNSLabel label : DNSLabel.values()) {
            if (label.indexValue == maskedIndex) return label;
        }
        return Unknown;
    }

    /**
     * @param index index of the label
     * @return masked value
     */
    public static int labelValue(int index) {
        return index & LABEL_NOT_MASK;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}