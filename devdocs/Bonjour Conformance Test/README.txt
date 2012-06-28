
Bonjour Conformance Test
Apple, Inc.

Use of the included Apple software is subject to the Software License Agreement ("License") that accompanies it.  Please read the License carefully before using this software.

The current version of the Conformance Test will only test products that do advertising.  A future version will add testing specifically designed for products that do browsing.  If your product does browsing and you would like to license the Bonjour logo today, the only requirement is that you pass the link-local addressing section of the Conformance Test.

Please send questions or comments regarding the test to Bonjour@apple.com.

See enclosed test outline (Conformance Test Outline.txt) for a complete description of test.

-------------------------------------------------------------------

To repeat these options, run "./BonjourConformanceTest -h".

Usage: BonjourConformanceTest [-Q] [-I interface name] [-F filename] [-L link-local options] [-M mdns options] [-N mixed-network options]
(running without options performs all subtests)

configuration options:
        -Q (quiet operation)
        -I (specify test-machine interface - must be active, with assigned IPv4 address)
        -6 (Use IPv6. Currently supported only with -M tests)
        -F (specify result file name - overwrites any existing file)
        -L (perform link-local address allocation test)
        -M (perform multicast-DNS test)
        -N (perform mixed-network interoperability test)
        -D (Enable Debug logging: 2 - Link Local 4 - MDNS 8 - Mixed Net 16 - Common Networking)

link-local options (to disable subtests):
        p       (probe denials)
        s       (simultaneous probes)
        r       (rate limiting)
        c       (subsequent conflicts)
        h       (hot-plugging)

multicast DNS options (to disable subtests):
        o       (old (pre 10.2.5) probe tiebreaker rules)
        p       (probe denials)
        s       (simultaneous probes)
        r       (rate limiting)
        u       (subsequent conflict)
        S       (srv probing)
        v       (simple reply verification)
        t       (shared reply timing)
        m       (multiple questions)
        d       (duplicate suppression)
        a       (reply aggregation)
        n       (manual name-change)
        h       (hot-plugging)
        c       (case change in queries and probe denials/conflicts)

mixed-network interoperability options (to disable subtests):
        t       (mDNS iP TTL check)
        l       (link-local (device) to routable (test machine) communication)
        r       (routable (device) to link-local (test machine) communication)
        u       (unicast interoperability)
        c       (chattiness)


Test must be run by a super-user.

Example: 'sudo ./BonjourConformanceTest -I en0 -L p -M hu' would run the link-local test with simultaneous probing disabled, and the multicast DNS test with hot-plugging and subsequent conflict tests disabled, using interface en0 on test machine.

Recommended testing environment:  The test machine (a Mac running OS X v10.2 or higher) and the device being tested should be connected via an ethernet hub (or Airport base station) in an isolated local network, detached from the Internet.  There should be no other devices or machines attached to the network.

------------------------------------------------------------------------

Release Notes:

Version 1.2.1 relaxes restrictions in the IP TTL check (requiring the TTL to be set only in mDNS packets, instead of all packets), and includes various bug fixes.

Version 1.2 features SRV record probe testing, enhanced query reply testing, more extensive unicast interoperability testing, support for devices that conform to IPv4 Link-Local draft 8, and various bug fixes.

Between versions 1.0 and 1.1, the tiebreaker rules for mDNS probes is reversed (reflecting a change in the specification.)  Devices using the old rules must be run with "-M o" options, and will still pass with a warning.

In v1.1, a failure is generated if the Cache Flush bit is set in announcements for PTR records.  Previously, these records were ignored by the test.

Known Issues:

Error messages referencing the "service name cache", or "no cached record" indicate that the test does not know the names of the device's services (required for query testing).  This is due to the device failing to correctly announce its PTR, SRV, A, or TXT records.

Time measurements for query responses may be affected by scheduling latency (on the order of milliseconds).

Service and host names with dot (".") literals not properly handled by test.

If running the test against Mac OS X, please dissable Personal File sharing, and enable some other Bonjour application or service (such as FTP access.)




