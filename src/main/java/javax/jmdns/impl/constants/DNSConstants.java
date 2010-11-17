// Copyright 2003-2005 Arthur van Hoff, Rick Blair
// Licensed under Apache License version 2.0
// Original license LGPL

package javax.jmdns.impl.constants;

/**
 * DNS constants.
 * 
 * @version %I%, %G%
 * @author Arthur van Hoff, Jeff Sonstein, Werner Randelshofer, Pierre Frisch, Rick Blair
 */
public final class DNSConstants {
    // http://www.iana.org/assignments/dns-parameters

    // changed to final class - jeffs
    public final static String MDNS_GROUP                     = "224.0.0.251";
    public final static String MDNS_GROUP_IPV6                = "FF02::FB";
    public final static int    MDNS_PORT                      = Integer.parseInt(System.getProperty("net.mdns.port", "5353"));
    public final static int    DNS_PORT                       = 53;
    public final static int    DNS_TTL                        = 60 * 60;                                                      // default one hour TTL
    // public final static int DNS_TTL = 120 * 60; // two hour TTL (draft-cheshire-dnsext-multicastdns.txt ch 13)

    public final static int    MAX_MSG_TYPICAL                = 1460;
    public final static int    MAX_MSG_ABSOLUTE               = 8972;

    public final static int    FLAGS_QR_MASK                  = 0x8000;                                                       // Query response mask
    public final static int    FLAGS_QR_QUERY                 = 0x0000;                                                       // Query
    public final static int    FLAGS_QR_RESPONSE              = 0x8000;                                                       // Response

    public final static int    FLAGS_AA                       = 0x0400;                                                       // Authorative answer
    public final static int    FLAGS_TC                       = 0x0200;                                                       // Truncated
    public final static int    FLAGS_RD                       = 0x0100;                                                       // Recursion desired
    public final static int    FLAGS_RA                       = 0x8000;                                                       // Recursion available

    public final static int    FLAGS_Z                        = 0x0040;                                                       // Zero
    public final static int    FLAGS_AD                       = 0x0020;                                                       // Authentic data
    public final static int    FLAGS_CD                       = 0x0010;                                                       // Checking disabled

    // Time Intervals for various functions

    public final static int    SHARED_QUERY_TIME              = 20;                                                           // milliseconds before send shared query
    public final static int    QUERY_WAIT_INTERVAL            = 225;                                                          // milliseconds between query loops.
    public final static int    PROBE_WAIT_INTERVAL            = 250;                                                          // milliseconds between probe loops.
    public final static int    RESPONSE_MIN_WAIT_INTERVAL     = 20;                                                           // minimal wait interval for response.
    public final static int    RESPONSE_MAX_WAIT_INTERVAL     = 115;                                                          // maximal wait interval for response
    public final static int    PROBE_CONFLICT_INTERVAL        = 1000;                                                         // milliseconds to wait after conflict.
    public final static int    PROBE_THROTTLE_COUNT           = 10;                                                           // After x tries go 1 time a sec. on probes.
    public final static int    PROBE_THROTTLE_COUNT_INTERVAL  = 5000;                                                         // We only increment the throttle count, if the previous increment is inside this interval.
    public final static int    ANNOUNCE_WAIT_INTERVAL         = 1000;                                                         // milliseconds between Announce loops.
    public final static int    RECORD_REAPER_INTERVAL         = 10000;                                                        // milliseconds between cache cleanups.
    public final static int    KNOWN_ANSWER_TTL               = 120;
    public final static int    ANNOUNCED_RENEWAL_TTL_INTERVAL = DNS_TTL * 500;                                                // 50% of the TTL in milliseconds

    public final static long   CLOSE_TIMEOUT                  = ANNOUNCE_WAIT_INTERVAL * 5L;
    public final static long   SERVICE_INFO_TIMEOUT           = ANNOUNCE_WAIT_INTERVAL * 6L;

    public final static int    NETWORK_CHECK_INTERVAL         = 10 * 1000;                                                    // 10 secondes

}
