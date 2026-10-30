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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class HostInfoTest {

    @Test
    void ignoresIPv6PacketOnIPv4Host() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("192.0.2.1", "2001:db8::2"));
    }

    @Test
    void ignoresIPv6LinkLocalPacketOnIPv4Host() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("192.0.2.1", "fe80::2"));
    }

    @Test
    void ignoresIPv4PacketOnIPv6Host() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("2001:db8::1", "192.0.2.2"));
    }

    @Test
    void acceptsIPv4PacketOnIPv4Host() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("192.0.2.1", "192.0.2.2"));
    }

    @Test
    void acceptsIPv6PacketOnIPv6Host() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("2001:db8::1", "2001:db8::2"));
    }

    @Test
    void acceptsIPv6LinkLocalPacketOnIPv6Host() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("2001:db8::1", "fe80::2"));
    }

    @Test
    void acceptsLinkLocalPacketOnLinkLocalHost() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("fe80::1", "fe80::2"));
    }

    @Test
    void ignoresNonLinkLocalPacketOnLinkLocalHost() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("fe80::1", "2001:db8::2"));
    }

    @Test
    void ignoresIPv4LoopbackPacketOnNonLoopbackHost() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("192.0.2.1", "127.0.0.1"));
    }

    @Test
    void acceptsIPv4LoopbackPacketOnLoopbackHost() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("127.0.0.1", "127.0.0.1"));
    }

    @Test
    void ignoresIPv6LoopbackPacketOnNonLoopbackHost() throws UnknownHostException {
        assertTrue(shouldIgnorePacket("2001:db8::1", "::1"));
    }

    @Test
    void acceptsIPv6LoopbackPacketOnLoopbackHost() throws UnknownHostException {
        assertFalse(shouldIgnorePacket("::1", "::1"));
    }

    private boolean shouldIgnorePacket(String localAddress, String sourceAddress) throws UnknownHostException {
        InetAddress address = InetAddress.getByAddress("test-host", InetAddress.getByName(localAddress).getAddress());
        HostInfo host = HostInfo.newHostInfo(address, null, "test-host");
        DatagramPacket packet = new DatagramPacket(new byte[0], 0, InetAddress.getByName(sourceAddress), 5353);
        return host.shouldIgnorePacket(packet);
    }
}
