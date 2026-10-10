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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Tests response routing through the socket listener using encoded DNS queries. */
class SocketListenerTest {
    private static final int LEGACY_QUERY_PORT =
            DNSConstants.MDNS_PORT == 65535 ? 65534 : DNSConstants.MDNS_PORT + 1;

    @Test
    void legacyQueryWithoutQuBitUsesUnicast() throws Exception {
        verifyQueryDestination(LEGACY_QUERY_PORT, false, false);
    }

    @Test
    void legacyQueryWithQuBitUsesUnicast() throws Exception {
        verifyQueryDestination(LEGACY_QUERY_PORT, true, false);
    }

    @Test
    void mdnsQueryWithQuBitUsesUnicast() throws Exception {
        verifyQueryDestination(DNSConstants.MDNS_PORT, true, false);
    }

    @Test
    void mdnsQueryWithoutQuBitUsesMulticast() throws Exception {
        verifyQueryDestination(DNSConstants.MDNS_PORT, false, true);
    }

    /**
     * Passes one encoded query through the real listener and verifies the response destination.
     *
     * @param sourcePort the querier's UDP port
     * @param qu whether the question requests a unicast response
     * @param multicast whether the expected destination is the multicast group
     * @throws Exception if the query cannot be encoded or processed
     */
    private void verifyQueryDestination(int sourcePort, boolean qu, boolean multicast) throws Exception {
        InetAddress sender = InetAddress.getByName("192.0.2.1");
        InetAddress group = InetAddress.getByName(DNSConstants.MDNS_GROUP);
        DNSOutgoing outgoing = new DNSOutgoing(0, false);
        outgoing.addQuestion(DNSQuestion.newQuestion("test.local.", DNSRecordType.TYPE_A,
                DNSRecordClass.CLASS_IN, qu));
        byte[] data = outgoing.data();
        // DNSOutgoing does not encode the QU bit, so set it in the wire question explicitly.
        if (qu) {
            data[data.length - 2] |= (byte) 0x80;
        }

        JmDNSImpl dns = mock(JmDNSImpl.class);
        MulticastSocket socket = mock(MulticastSocket.class);
        HostInfo host = mock(HostInfo.class);
        when(dns.getName()).thenReturn("test");
        when(dns.getSocket()).thenReturn(socket);
        when(dns.getLocalHost()).thenReturn(host);
        when(dns.getGroup()).thenReturn(group);
        // Enter the loop, process one packet, then exit without starting a thread.
        when(dns.isCanceling()).thenReturn(false, false, true);
        doAnswer(invocation -> {
            DatagramPacket packet = invocation.getArgument(0);
            System.arraycopy(data, 0, packet.getData(), 0, data.length);
            packet.setLength(data.length);
            packet.setAddress(sender);
            packet.setPort(sourcePort);
            return null;
        }).when(socket).receive(any(DatagramPacket.class));

        new SocketListener(dns).run();

        verify(dns).handleQuery(any(DNSIncoming.class), eq(multicast ? group : sender),
                eq(multicast ? DNSConstants.MDNS_PORT : sourcePort));
        verify(dns, never()).handleResponse(any(DNSIncoming.class));
        verify(dns, never()).recover();
        verify(socket).receive(any(DatagramPacket.class));
    }
}