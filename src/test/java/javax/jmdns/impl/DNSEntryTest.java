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

import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DNSEntryTest {

    private static DNSQuestion q(String name, DNSRecordType type, DNSRecordClass clazz) {
        // unique flag is irrelevant for isSameEntry
        return new DNSQuestion(name, type, clazz, false);
    }

    @Test
    void isSameEntryTrueWhenAllMatchAndSameSubtype() {
        String name1 = "AbCdE._printer._sub._http._tcp.Panoramix.local."; // mixed case to exercise key lowercasing
        String name2 = "abcde._printer._sub._http._tcp.panoramix.local.";

        DNSEntry a = q(name1, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);
        DNSEntry b = q(name2, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);

        assertTrue(a.isSameEntry(b));
        assertTrue(b.isSameEntry(a));
    }

    @Test
    void isSameEntryFalseWhenSubtypeDiffers() {
        String nameWithPrinter = "abcde._printer._sub._http._tcp.local.";
        String nameWithScanner = "abcde._scanner._sub._http._tcp.local.";

        DNSEntry a = q(nameWithPrinter, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);
        DNSEntry b = q(nameWithScanner, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);

        // key and record type/class match, but subtype differs => false
        assertFalse(a.isSameEntry(b));
        assertFalse(b.isSameEntry(a));
    }

    @Test
    void isSameEntryFalseWhenRecordTypeDiffers() {
        String name = "any_instance_name._http._tcp.local.";

        DNSEntry a = q(name, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);
        DNSEntry b = q(name, DNSRecordType.TYPE_TXT, DNSRecordClass.CLASS_IN);

        assertFalse(a.isSameEntry(b));
        assertFalse(b.isSameEntry(a));
    }

    @Test
    void isSameEntryTrueWhenClassAnyOnEitherSide() {
        String name = "abcde._printer._sub._http._tcp.local.";

        DNSEntry inClass = q(name, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);
        DNSEntry anyClass = q(name, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_ANY);

        assertTrue(inClass.isSameEntry(anyClass));
        assertTrue(anyClass.isSameEntry(inClass));
    }

    @Test
    void isSameEntryFalseWhenKeyDiffers() {
        String name1 = "alpha._printer._sub._http._tcp.local.";
        String name2 = "beta._printer._sub._http._tcp.local.";

        DNSEntry a = q(name1, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);
        DNSEntry b = q(name2, DNSRecordType.TYPE_SRV, DNSRecordClass.CLASS_IN);

        assertFalse(a.isSameEntry(b));
        assertFalse(b.isSameEntry(a));
    }
}
