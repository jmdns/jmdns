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
package javax.jmdns.impl.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;

import org.junit.jupiter.api.Test;

class ResponderTest {

    private static final String SERVICE_TYPE = "_printer._tcp.local.";
    private static final String SUBTYPE = "_universal._sub." + SERVICE_TYPE;
    private static final String HOST_NAME = "printer.local.";

    @Test
    void groupsEveryServiceInALargeResponseWithItsRelatedRecords() {
        assertPrinterAnnouncementsAreGrouped(20);
    }

    @Test
    void groupsTenPrinterAnnouncements() {
        assertPrinterAnnouncementsAreGrouped(10);
    }

    private void assertPrinterAnnouncementsAreGrouped(int printerCount) {
        Set<DNSRecord> answers = new HashSet<>();
        Map<DNSRecord, Set<DNSRecord>> expectedGroups = new HashMap<>();

        for (int serviceNumber = 0; serviceNumber < printerCount; serviceNumber++) {
            String serviceName = "printer-" + serviceNumber + "." + SERVICE_TYPE;
            DNSRecord.Pointer pointer = new DNSRecord.Pointer(SERVICE_TYPE, DNSRecordClass.CLASS_IN, false, DNSConstants.DNS_TTL, serviceName);
            DNSRecord.Pointer subtypePointer = new DNSRecord.Pointer(SUBTYPE, DNSRecordClass.CLASS_IN, false, DNSConstants.DNS_TTL, serviceName);
            DNSRecord.Service service = new DNSRecord.Service(serviceName, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, 0, 0, 515, HOST_NAME);
            DNSRecord.Text text = new DNSRecord.Text(serviceName, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, new byte[0]);
            answers.add(pointer);
            answers.add(subtypePointer);
            answers.add(service);
            answers.add(text);

            Set<DNSRecord> expectedGroup = new HashSet<>();
            expectedGroup.add(pointer);
            expectedGroup.add(subtypePointer);
            expectedGroup.add(service);
            expectedGroup.add(text);
            expectedGroups.put(service, expectedGroup);
        }

        List<Set<DNSRecord>> responseGroups = Responder.createResponseGroups(answers);

        assertEquals(printerCount, responseGroups.size());
        for (Map.Entry<DNSRecord, Set<DNSRecord>> expectedGroup : expectedGroups.entrySet()) {
            List<Set<DNSRecord>> groupsForService = new ArrayList<>();
            for (Set<DNSRecord> responseGroup : responseGroups) {
                if (responseGroup.contains(expectedGroup.getKey())) {
                    groupsForService.add(responseGroup);
                }
            }
            assertEquals(1, groupsForService.size());
            assertEquals(expectedGroup.getValue(), groupsForService.get(0));
        }
    }

    @Test
    void keepsASingleServiceResponseTogether() {
        String serviceName = "printer." + SERVICE_TYPE;
        Set<DNSRecord> answers = new HashSet<>();
        answers.add(new DNSRecord.Pointer(SERVICE_TYPE, DNSRecordClass.CLASS_IN, false, DNSConstants.DNS_TTL, serviceName));
        answers.add(new DNSRecord.Service(serviceName, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, 0, 0, 515, HOST_NAME));
        answers.add(new DNSRecord.Text(serviceName, DNSRecordClass.CLASS_IN, true, DNSConstants.DNS_TTL, new byte[0]));

        List<Set<DNSRecord>> responseGroups = Responder.createResponseGroups(answers);

        assertEquals(1, responseGroups.size());
        assertEquals(answers, responseGroups.get(0));
    }

}
