// %Z%%M%, %I%, %G%
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package javax.jmdns;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

/**
 * HostInfo information on the local host to be able to cope with change of addresses.
 *
 * @author	Pierre Frisch, Werner Randelshofer
 * @version 	%I%, %G%
 */
class HostInfo {
    private static Logger logger = Logger.getLogger(HostInfo.class.toString());
    protected String name;
    protected InetAddress address;
    protected NetworkInterface interfaze;
    /**
     * This is used to create a unique name for the host name.
     */
    private int hostNameCount;
    
    public HostInfo(InetAddress address, String name) {
        super();
        this.address = address;
        this.name = name;
        if (address != null) {
            try {
                interfaze = NetworkInterface.getByInetAddress(address);
            } catch (Exception exception) {
                // FIXME Shouldn't we take an action here?
                logger.log(Level.WARNING, "LocalHostInfo() exception " , exception);
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public InetAddress getAddress() {
        return address;
    }
    
    public NetworkInterface getInterface() {
        return interfaze;
    }
    
    synchronized String incrementHostName() {
        hostNameCount++;
        int plocal = name.indexOf(".local.");
        int punder = name.lastIndexOf("-");
		name = name.substring(0, ( punder == -1 ? plocal : punder ) ) + "-" + hostNameCount + ".local.";
		return name;
    }
    
    boolean shouldIgnorePacket(DatagramPacket packet) {
        boolean result = false;
        if ( getAddress() != null) {
            InetAddress from = packet.getAddress();
            if (from != null) {
                if ( from.isLinkLocalAddress() && ( ! getAddress().isLinkLocalAddress()) ) {
                    // Ignore linklocal packets on regular interfaces, unless this is
                    // also a linklocal interface. This is to avoid duplicates. This is
                    // a terrible hack caused by the lack of an API to get the address
                    // of the interface on which the packet was received.
                    result = true;
                }
                if ( from.isLoopbackAddress() && ( ! getAddress().isLoopbackAddress()) ) {
                    // Ignore loopback packets on a regular interface unless this is
                    // also a loopback interface.
                    result = true;
                }
            }
        }
        return result;
    }
    
    DNSRecord.Address getDNSAddressRecord(DNSRecord.Address address) {
        return ( DNSConstants.TYPE_AAAA == address.type ? getDNS6AddressRecord() : getDNS4AddressRecord() );
    }
    
    DNSRecord.Address getDNS4AddressRecord() {
        if ( (getAddress() != null) &&
        ( ( getAddress() instanceof Inet4Address) ||
        ( (getAddress() instanceof Inet6Address) && ( ((Inet6Address)getAddress()).isIPv4CompatibleAddress() ) ) ) ) {
            return new DNSRecord.Address(getName(), DNSConstants.TYPE_A, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, getAddress());
        }
        return null;
    }
    
    DNSRecord.Address getDNS6AddressRecord() {
        if ( (getAddress() != null) && ( getAddress() instanceof Inet6Address) ) {
            return new DNSRecord.Address(getName(), DNSConstants.TYPE_AAAA, DNSConstants.CLASS_IN, DNSConstants.DNS_TTL, getAddress());
        }
        return null;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("local host info[");
        buf.append( getName() != null ? getName() : "no name" );
        buf.append(", ");
        buf.append( getInterface() != null ? getInterface().getDisplayName() : "???" );
        buf.append(":");
        buf.append( getAddress() != null ? getAddress().getHostAddress() : "no address" );
        buf.append("]");
        return buf.toString();
    }
    
}
