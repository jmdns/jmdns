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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DNS Record Class
 * 
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public enum DNSRecordClass {
    /**
     * Unknown record class
     */
    CLASS_UNKNOWN("?", 0),
    /**
     * static final Internet
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
     * Multicast DNS uses the bottom 15 bits to identify the record class...<br/>
     * Except for pseudo records like OPT.
     */
    public static final int CLASS_MASK = 0x7FFF;

    /**
     * For answers the top bit indicates that all other cached records are now invalid.<br/>
     * For questions, it indicates that we should send a unicast response.
     */
    public static final int CLASS_UNIQUE = 0x8000;

    public static final boolean UNIQUE = true;

    public static final boolean NOT_UNIQUE = false;

    private final String externalName;

    private final int indexValue;

    DNSRecordClass(String name, int index) {
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
     * Checks if the class is unique
     * 
     * @param index
     * @return <code>true</code> is the class is unique, <code>false</code> otherwise.
     */
    public boolean isUnique(int index) {
        return (this != CLASS_UNKNOWN) && ((index & CLASS_UNIQUE) != 0);
    }

    /**
     * @param name
     * @return class for name
     */
    public static DNSRecordClass classForName(String name) {
        if (name != null) {
            String lowerCaseName = name.toLowerCase();
            for (DNSRecordClass recordClass : DNSRecordClass.values()) {
                if (recordClass.externalName.equals(lowerCaseName)) return recordClass;
            }
        }

        final Logger logger = LoggerFactory.getLogger(DNSRecordClass.class);
        logger.warn("Could not find record class for name: {}", name);

        return CLASS_UNKNOWN;
    }

    /**
     * @param index
     * @return class for name
     */
    public static DNSRecordClass classForIndex(int index) {
        int maskedIndex = index & CLASS_MASK;
        for (DNSRecordClass recordClass : DNSRecordClass.values()) {
            if (recordClass.indexValue == maskedIndex) return recordClass;
        }

        final Logger logger = LoggerFactory.getLogger(DNSRecordClass.class);
        logger.debug("Could not find record class for index: {}", index);

        return CLASS_UNKNOWN;
    }

    @Override
    public String toString() {
        return this.name() + " index " + this.indexValue();
    }

}
