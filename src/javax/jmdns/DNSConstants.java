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

/**
 * DNS constants.
 *
 * @author	Arthur van Hoff
 * @version 	%I%, %G%
 */
abstract class DNSConstants
{
    final static String MDNS_GROUP	= "224.0.0.251"; 
    final static int MDNS_PORT		= 5353;
    final static int DNS_PORT		= 53;

    final static int MAX_MSG_TYPICAL	= 1460;
    final static int MAX_MSG_ABSOLUTE	= 8972;
    
    final static int FLAGS_QR_MASK	= 0x8000;	// Query response mask
    final static int FLAGS_QR_QUERY	= 0x0000;	// Query 
    final static int FLAGS_QR_RESPONSE	= 0x8000;	// Response

    final static int FLAGS_AA		= 0x0400;	// Authorative answer
    final static int FLAGS_TC		= 0x0200;	// Truncated
    final static int FLAGS_RD		= 0x0100;	// Recursion desired
    final static int FLAGS_RA		= 0x8000;	// Recursion available

    final static int FLAGS_Z		= 0x0040;	// Zero
    final static int FLAGS_AD		= 0x0020;	// Authentic data
    final static int FLAGS_CD		= 0x0010;	// Checking disabled


    final static int CLASS_IN		= 1;		// Final Static Internet
    final static int CLASS_CS		= 2;		// CSNET
    final static int CLASS_CH		= 3;		// CHAOS
    final static int CLASS_HS		= 4;		// Hesiod
    final static int CLASS_NONE		= 254;		// Used in DNS UPDATE [RFC 2136]
    final static int CLASS_ANY		= 255;		// Not a DNS class, but a DNS query class, meaning "all classes"
    final static int CLASS_MASK		= 0x7FFF;	// Multicast DNS uses the bottom 15 bits to identify the record class...
    final static int CLASS_UNIQUE	= 0x8000;	// ... and the top bit indicates that all other cached records are now invalid

    final static int TYPE_A		= 1; 		// Address
    final static int TYPE_NS		= 2;		// Name Server
    final static int TYPE_MD		= 3;		// Mail Destination
    final static int TYPE_MF		= 4;		// Mail Forwarder
    final static int TYPE_CNAME		= 5;		// Canonical Name
    final static int TYPE_SOA		= 6;		// Start of Authority
    final static int TYPE_MB		= 7;		// Mailbox
    final static int TYPE_MG		= 8;		// Mail Group
    final static int TYPE_MR		= 9;		// Mail Rename
    final static int TYPE_NULL		= 10;		// NULL RR
    final static int TYPE_WKS		= 11;		// Well-known-service
    final static int TYPE_PTR		= 12;		// Domain Name pofinal static inter
    final static int TYPE_HINFO		= 13;		// Host information
    final static int TYPE_MINFO		= 14;		// Mailbox information
    final static int TYPE_MX		= 15;		// Mail exchanger
    final static int TYPE_TXT		= 16;		// Arbitrary text string
    final static int TYPE_SRV		= 33;		// Service record
    final static int TYPE_ANY		= 255;
}
