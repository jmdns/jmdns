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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Timer;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DNSTaskTest {

    private static final int SMALL_PAYLOAD = 200;

    private static final class TestTask extends DNSTask {
        TestTask(JmDNSImpl dns) {
            super(dns);
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public void start(Timer timer) {
            // not used
        }

        @Override
        public void run() {
            // not used
        }
    }

    private static List<DNSOutgoing> rollOver(int flags, boolean multicast) throws IOException {
        JmDNSImpl dns = mock(JmDNSImpl.class);
        TestTask task = new TestTask(dns);
        DNSOutgoing out = new DNSOutgoing(flags, multicast, SMALL_PAYLOAD);
        for (int i = 0; i < 20; i++) {
            DNSQuestion question = DNSQuestion.newQuestion("service-" + i + "._tcp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, false);
            out = task.addQuestion(out, question);
        }
        ArgumentCaptor<DNSOutgoing> sent = ArgumentCaptor.forClass(DNSOutgoing.class);
        verify(dns, atLeastOnce()).send(sent.capture());
        return sent.getAllValues();
    }

    @Test
    void multicastResponseRolloverDoesNotSetTruncatedBit() throws IOException {
        List<DNSOutgoing> sent = rollOver(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, true);
        for (DNSOutgoing out : sent) {
            assertFalse(out.isTruncated());
        }
    }

    @Test
    void unicastResponseRolloverStillSetsTruncatedBit() throws IOException {
        List<DNSOutgoing> sent = rollOver(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, false);
        assertTrue(sent.get(0).isTruncated());
    }

    @Test
    void multicastQueryRolloverStillSetsTruncatedBit() throws IOException {
        List<DNSOutgoing> sent = rollOver(DNSConstants.FLAGS_QR_QUERY, true);
        assertTrue(sent.get(0).isTruncated());
    }
}
