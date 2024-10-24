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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceInfo.Fields;
import javax.jmdns.impl.DNSOutgoing.MessageOutputStream;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

import javax.jmdns.impl.util.ByteWrangler;


/**
 * DNS record
 *
 * @author Arthur van Hoff, Rick Blair, Werner Randelshofer, Pierre Frisch
 */
public abstract class DNSRecord extends DNSEntry {
    private final Logger logger = LoggerFactory.getLogger(DNSRecord.class);

    private int           _ttl;
    private long          _created;
    private int           _isStaleAndShouldBeRefreshedPercentage;
    private final int     _randomStaleRefreshOffset;

    /**
     * This source is mainly for debugging purposes, should be the address that sent this record.
     */
    private InetAddress   _source;

    /**
     * Create a DNSRecord with a name, type, class, and ttl.
     */
    DNSRecord(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique, int ttl) {
        super(name, type, recordClass, unique);
        this._ttl = ttl;
        this._created = System.currentTimeMillis();
        _randomStaleRefreshOffset = new Random().nextInt(3);
        _isStaleAndShouldBeRefreshedPercentage = DNSConstants.STALE_REFRESH_STARTING_PERCENTAGE + _randomStaleRefreshOffset;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        return (other instanceof DNSRecord) && super.equals(other) && sameValue((DNSRecord) other);
    }

    /**
     * True if this record has the same value as some other record.
     */
    abstract boolean sameValue(DNSRecord other);

    /**
     * True if this record has the same type as some other record.
     */
    boolean sameType(DNSRecord other) {
        return this.getRecordType() == other.getRecordType();
    }

    /**
     * Handles a query represented by this record.
     *
     * @return Returns true if a conflict with one of the services registered with JmDNS or with the hostname occurred.
     */
    abstract boolean handleQuery(JmDNSImpl dns, long expirationTime);

    /**
     * Handles a response represented by this record.
     *
     * @return Returns true if a conflict with one of the services registered with JmDNS or with the hostname occurred.
     */
    abstract boolean handleResponse(JmDNSImpl dns);

    /**
     * Adds this as an answer to the provided outgoing datagram.
     */
    abstract DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) throws IOException;

    /**
     * True if this record is suppressed by the answers in a message.
     */
    boolean suppressedBy(DNSIncoming msg) {
        try {
            for (DNSRecord answer : msg.getAllAnswers()) {
                if (suppressedBy(answer)) {
                    return true;
                }
            }
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            logger.warn("suppressedBy() message {} exception ", msg, e);
            // msg.print(true);
            return false;
        }
    }

    /**
     * True if this record would be suppressed by an answer. This is the case if this record would not have a significantly longer TTL.
     */
    boolean suppressedBy(DNSRecord other) {
        return this.equals(other) && (other._ttl > _ttl / 2);
    }

    /**
     * Get the expiration time of this record.
     */
    long getExpirationTime(int percent) {
        // ttl is in seconds the constant 10 is 1000 ms / 100 %
        return _created + (percent * ((long)_ttl) * 10L);
    }

    /**
     * Get the remaining TTL for this record.
     */
    int getRemainingTTL(long now) {
        return (int) Math.max(0, (getExpirationTime(100) - now) / 1000);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#isExpired(long)
     */
    @Override
    public boolean isExpired(long now) {
        return getExpirationTime(100) <= now;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#isStale(long)
     */
    @Override
    public boolean isStale(long now) {
        return getExpirationTime(50) <= now;
    }

    /**
     * Check if the record is stale and whether the record should be refreshed over the network.
     *
     * @param now
     *            update date
     * @return <code>true</code> is the record is stale and should be refreshed, <code>false</code> otherwise.
     */
    public boolean isStaleAndShouldBeRefreshed(long now) {
        return getExpirationTime(_isStaleAndShouldBeRefreshedPercentage) <= now;
    }

    /*
    * Increment the percentage that determines whether a record needs to be refreshed.
     */
    public void incrementRefreshPercentage() {
        _isStaleAndShouldBeRefreshedPercentage += DNSConstants.STALE_REFRESH_INCREMENT;
        if (_isStaleAndShouldBeRefreshedPercentage > 100) {
            _isStaleAndShouldBeRefreshedPercentage = 100;
        }
    }

    /**
     * Reset the TTL of a record. This avoids having to update the entire record in the cache.
     */
    void resetTTL(DNSRecord other) {
        _created = other._created;
        _ttl = other._ttl;
        _isStaleAndShouldBeRefreshedPercentage = DNSConstants.STALE_REFRESH_STARTING_PERCENTAGE + _randomStaleRefreshOffset;
    }

    /**
     * When a record flushed we don't remove it immediately, but mark it for rapid decay.
     */
    void setWillExpireSoon(long now) {
        _created = now;
        _ttl = DNSConstants.RECORD_EXPIRY_DELAY;
    }

    /**
     * Write this record into an outgoing message.
     */
    abstract void write(MessageOutputStream out);

    public static class IPv4Address extends Address {

        IPv4Address(String name, DNSRecordClass recordClass, boolean unique, int ttl, InetAddress addr) {
            super(name, DNSRecordType.TYPE_A, recordClass, unique, ttl, addr);
        }

        IPv4Address(String name, DNSRecordClass recordClass, boolean unique, int ttl, byte[] rawAddress) {
            super(name, DNSRecordType.TYPE_A, recordClass, unique, ttl, rawAddress);
        }

        @Override
        void write(MessageOutputStream out) {
            if (_addr == null) {
                return;
            }

            byte[] buffer = _addr.getAddress();

            // Check if the address is IPv6 (Inet6Address) and extract the last 4 bytes for IPv4 compatibility
            if (_addr instanceof Inet6Address) {
                buffer = new byte[4];
                System.arraycopy(_addr.getAddress(), 12, buffer, 0, 4);
            }

            out.writeBytes(buffer, 0, buffer.length);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {

            ServiceInfoImpl info = (ServiceInfoImpl) super.getServiceInfo(persistent);
            info.addAddress((Inet4Address) _addr);
            return info;
        }

    }

    public static class IPv6Address extends Address {

        IPv6Address(String name, DNSRecordClass recordClass, boolean unique, int ttl, InetAddress addr) {
            super(name, DNSRecordType.TYPE_AAAA, recordClass, unique, ttl, addr);
        }

        IPv6Address(String name, DNSRecordClass recordClass, boolean unique, int ttl, byte[] rawAddress) {
            super(name, DNSRecordType.TYPE_AAAA, recordClass, unique, ttl, rawAddress);
        }

        @Override
        void write(MessageOutputStream out) {
            if (_addr != null) {
                byte[] buffer = _addr.getAddress();

                // If we have a type AAAA record, we should answer with an IPv6 address
                if (_addr instanceof Inet4Address) {
                    // Create an IPv6-mapped IPv4 address
                    byte[] ipv6buffer = new byte[16];

                    // Fill the first 10 bytes with 0, bytes 11 and 12 with 0xFF, and the rest with the IPv4 address
                    ipv6buffer[10] = (byte) 0xFF;
                    ipv6buffer[11] = (byte) 0xFF;

                    // Copy the IPv4 address into the last 4 bytes of the IPv6 buffer
                    System.arraycopy(buffer, 0, ipv6buffer, 12, buffer.length);

                    buffer = ipv6buffer; // Update buffer to be the IPv6-mapped address
                }

                // Write the buffer to the output stream
                int length = buffer.length;
                out.writeBytes(buffer, 0, length);
            }
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {

            ServiceInfoImpl info = (ServiceInfoImpl) super.getServiceInfo(persistent);
            info.addAddress((Inet6Address) _addr);
            return info;
        }

    }

    /**
     * Address record.
     */
    public static abstract class Address extends DNSRecord {
        private final Logger logger1 = LoggerFactory.getLogger(Address.class);

        InetAddress           _addr;

        protected Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique, int ttl, InetAddress addr) {
            super(name, type, recordClass, unique, ttl);
            this._addr = addr;
        }

        protected Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique, int ttl, byte[] rawAddress) {
            super(name, type, recordClass, unique, ttl);
            try {
                this._addr = InetAddress.getByAddress(rawAddress);
            } catch (UnknownHostException exception) {
                logger1.warn("Address() exception ", exception);
            }
        }

        boolean same(DNSRecord other) {
            if (!(other instanceof Address)) {
                return false;
            }
            return ((sameName(other)) && ((sameValue(other))));
        }

        boolean sameName(DNSRecord other) {
            return this.getName().equalsIgnoreCase(other.getName());
        }

        @Override
        boolean sameValue(DNSRecord other) {
            try {
                if (!(other instanceof Address)) {
                    return false;
                }
                Address address = (Address) other;
                return Objects.equals(this.getAddress(), address.getAddress());
            } catch (Exception e) {
                logger1.info("Failed to compare addresses of DNSRecords", e);
                return false;
            }
        }

        @Override
        public boolean isSingleValued() {
            return false;
        }

        InetAddress getAddress() {
            return _addr;
        }

        /**
         * Creates a byte array representation of this record. This is needed for tie-break tests according to draft-cheshire-dnsext-multicastdns-04.txt chapter 9.2.
         */
        @Override
        protected void toByteArray(DataOutputStream dout) throws IOException {
            super.toByteArray(dout);
            if (this.getAddress() == null) {
                return;
            }

            byte[] buffer = this.getAddress().getAddress();
            for (byte b : buffer) {
                dout.writeByte(b);
            }
        }

        /**
         * Does the necessary actions, when this as a query.
         */
        @Override
        boolean handleQuery(JmDNSImpl dns, long expirationTime) {
            if (dns.getLocalHost().conflictWithRecord(this)) {
                DNSRecord.Address localAddress = dns.getLocalHost().getDNSAddressRecord(this.getRecordType(), this.isUnique(), DNSConstants.DNS_TTL);
                if (localAddress != null) {
                    int comparison = this.compareTo(localAddress);

                    if (comparison == 0) {
                        // the 2 records are identical this probably means we are seeing our own record.
                        // With multiple interfaces on a single computer it is possible to see our
                        // own records come in on different interfaces than the ones they were sent on.
                        // see section "10. Conflict Resolution" of mdns draft spec.
                        logger1.debug("handleQuery() Ignoring an identical address query");
                        return false;
                    }

                    logger1.debug("handleQuery() Conflicting query detected.");
                    // tiebreaker test
                    if (dns.isProbing() && comparison > 0) {
                        // We lost the tie-break. We have to choose a different name.
                        dns.getLocalHost().incrementHostName();
                        dns.getCache().clear();
                        for (ServiceInfo serviceInfo : dns.getServices().values()) {
                            ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
                            info.revertState();
                        }
                    }
                    dns.revertState();
                    return true;
                }
            }
            return false;
        }

        /**
         * Does the necessary actions, when this as a response.
         */
        @Override
        boolean handleResponse(JmDNSImpl dns) {
            if (dns.getLocalHost().conflictWithRecord(this)) {
                logger1.debug("handleResponse() Denial detected");

                if (dns.isProbing()) {
                    dns.getLocalHost().incrementHostName();
                    dns.getCache().clear();
                    for (ServiceInfo serviceInfo : dns.getServices().values()) {
                        ServiceInfoImpl info = (ServiceInfoImpl) serviceInfo;
                        info.revertState();
                    }
                }
                dns.revertState();
                return true;
            }
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) {
            return out;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {
            // info.setAddress(_addr); This is done in the subclass so we don't have to test for class type
            return new ServiceInfoImpl(this.getQualifiedNameMap(), 0, 0, 0, persistent, (byte[]) null);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceEvent(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        public ServiceEvent getServiceEvent(JmDNSImpl dns) {
            ServiceInfo info = this.getServiceInfo(false);
            ((ServiceInfoImpl) info).setDns(dns);
            return new ServiceEventImpl(dns, info.getType(), info.getName(), info);
        }

        /*
         * (non-Javadoc)
         * @see com.webobjects.discoveryservices.DNSRecord#toString(java.lang.StringBuilder)
         */
        @Override
        protected void toString(final StringBuilder sb) {
            super.toString(sb);
            sb.append(" address: '")
                .append(this.getAddress() != null ? this.getAddress().getHostAddress() : "null")
                .append('\'');
        }

    }

    /**
     * Pointer record.
     */
    public static class Pointer extends DNSRecord {
        // private static Logger logger = LoggerFactory.getLogger(Pointer.class);
        private final String _alias;

        public Pointer(String name, DNSRecordClass recordClass, boolean unique, int ttl, String alias) {
            super(name, DNSRecordType.TYPE_PTR, recordClass, unique, ttl);
            this._alias = alias;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSEntry#isSameEntry(javax.jmdns.impl.DNSEntry)
         */
        @Override
        public boolean isSameEntry(DNSEntry entry) {
            return super.isSameEntry(entry) && (entry instanceof Pointer) && this.sameValue((Pointer) entry);
        }

        @Override
        void write(MessageOutputStream out) {
            out.writeName(_alias);
        }

        @Override
        boolean sameValue(DNSRecord other) {
            if (!(other instanceof Pointer)) {
                return false;
            }
            Pointer pointer = (Pointer) other;
            if ((_alias == null) && (pointer._alias != null)) {
                return false;
            }
            return Objects.equals(_alias, pointer._alias);
        }

        @Override
        public boolean isSingleValued() {
            return false;
        }

        @Override
        boolean handleQuery(JmDNSImpl dns, long expirationTime) {
            // Nothing to do (?)
            // I think there is no possibility for conflicts for this record type?
            return false;
        }

        @Override
        boolean handleResponse(JmDNSImpl dns) {
            // Nothing to do (?)
            // I think there is no possibility for conflicts for this record type?
            return false;
        }

        String getAlias() {
            return _alias;
        }

        @Override
        DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) {
            return out;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {
            if (this.isServicesDiscoveryMetaQuery()) {
                // The service name is in the alias
                Map<Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(this.getAlias());
                return new ServiceInfoImpl(map, 0, 0, 0, persistent, (byte[]) null);
            } else if (this.isReverseLookup()) {
                return new ServiceInfoImpl(this.getQualifiedNameMap(), 0, 0, 0, persistent, (byte[]) null);
            } else if (this.isDomainDiscoveryQuery()) {
                // FIXME [PJYF Nov 16 2010] We do not currently support domain discovery
                return new ServiceInfoImpl(this.getQualifiedNameMap(), 0, 0, 0, persistent, (byte[]) null);
            }
            Map<Fields, String> map = ServiceTypeDecoder.decodeQualifiedNameMapForType(this.getAlias());
            map.put(Fields.Subtype, this.getQualifiedNameMap().get(Fields.Subtype));
            return new ServiceInfoImpl(map, 0, 0, 0, persistent, this.getAlias());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceEvent(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        public ServiceEvent getServiceEvent(JmDNSImpl dns) {
            ServiceInfo info = this.getServiceInfo(false);
            ((ServiceInfoImpl) info).setDns(dns);
            String domainName = info.getType();
            String serviceName = JmDNSImpl.toUnqualifiedName(domainName, this.getAlias());
            return new ServiceEventImpl(dns, domainName, serviceName, info);
        }

        /*
         * (non-Javadoc)
         * @see com.webobjects.discoveryservices.DNSRecord#toString(java.lang.StringBuilder)
         */
        @Override
        protected void toString(final StringBuilder sb) {
            super.toString(sb);
            sb.append(" alias: '")
                .append(_alias)
                .append('\'');
        }

    }

    public static class Text extends DNSRecord {
        // private static Logger logger = LoggerFactory.getLogger(Text.class);
        private final byte[] _text;

        public Text(String name, DNSRecordClass recordClass, boolean unique, int ttl, byte[] text) {
            super(name, DNSRecordType.TYPE_TXT, recordClass, unique, ttl);
            this._text = (text != null && text.length > 0 ? text : ByteWrangler.EMPTY_TXT);
        }

        /**
         * @return the text
         */
        byte[] getText() {
            return this._text;
        }

        @Override
        void write(MessageOutputStream out) {
            out.writeBytes(_text, 0, _text.length);
        }

        @Override
        boolean sameValue(DNSRecord other) {
            // Check if other is not null and is of the correct type
            if (!(other instanceof Text)) {
                return false;
            }

            Text txt = (Text) other;

            // Use Objects.equals to handle null-safe comparison for both arrays
            if (!Arrays.equals(_text, txt._text)) {
                return false;
            }

            // Compare the contents of both arrays if they are non-null
            if (_text != null) {
                for (int i = 0; i < _text.length; i++) {
                    if (!Objects.equals(_text[i], txt._text[i])) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Override
        public boolean isSingleValued() {
            return true;
        }

        @Override
        boolean handleQuery(JmDNSImpl dns, long expirationTime) {
            // Nothing to do (?)
            // I think there is no possibility for conflicts for this record type?
            return false;
        }

        @Override
        boolean handleResponse(JmDNSImpl dns) {
            // Nothing to do (?)
            // Shouldn't we care if we get a conflict at this level?
            /*
             * ServiceInfo info = (ServiceInfo) dns.services.get(name.toLowerCase()); if (info != null) { if (! Arrays.equals(text,info.text)) { info.revertState(); return true; } }
             */
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) {
            return out;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {
            return new ServiceInfoImpl(this.getQualifiedNameMap(), 0, 0, 0, persistent, _text);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceEvent(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        public ServiceEvent getServiceEvent(JmDNSImpl dns) {
            ServiceInfo info = this.getServiceInfo(false);
            ((ServiceInfoImpl) info).setDns(dns);
            return new ServiceEventImpl(dns, info.getType(), info.getName(), info);
        }

        /*
         * (non-Javadoc)
         * @see com.webobjects.discoveryservices.DNSRecord#toString(java.lang.StringBuilder)
         */
        @Override
        protected void toString(final StringBuilder sb) {
            super.toString(sb);
            sb.append(" text: '");

            final String text = ByteWrangler.readUTF(_text);

            // if the text is longer than 20 characters cut it to 17 chars
            // and add "..." at the end
            if (20 < text.length()) {
                sb.append(text, 0, 17).append("...");
            } else {
                sb.append(text);
            }
            sb.append('\'');
        }

    }

    /**
     * Service record.
     */
    public static class Service extends DNSRecord {
        private final int     _priority;
        private final int     _weight;
        private final int     _port;
        private final String  _server;

        public Service(String name, DNSRecordClass recordClass, boolean unique, int ttl, int priority, int weight, int port, String server) {
            super(name, DNSRecordType.TYPE_SRV, recordClass, unique, ttl);
            this._priority = priority;
            this._weight = weight;
            this._port = port;
            this._server = server;
        }

        @Override
        void write(MessageOutputStream out) {
            out.writeShort(_priority);
            out.writeShort(_weight);
            out.writeShort(_port);
            if (DNSIncoming.USE_DOMAIN_NAME_FORMAT_FOR_SRV_TARGET) {
                out.writeName(_server);
            } else {
                // [PJYF Nov 13 2010] Do we still need this? This looks dreadful. All label are supposed to start by a length.
                out.writeUTF(_server, 0, _server.length());

                // add a zero byte to the end just to be safe, this is the strange form
                // used by the BonjourConformanceTest
                out.writeByte(0);
            }
        }

        @Override
        protected void toByteArray(DataOutputStream dout) throws IOException {
            super.toByteArray(dout);
            dout.writeShort(_priority);
            dout.writeShort(_weight);
            dout.writeShort(_port);
            try {
                dout.write(_server.getBytes(StandardCharsets.UTF_8));
            } catch (UnsupportedEncodingException exception) {
                /* UTF-8 is always present */
            }
        }

        String getServer() {
            return _server;
        }

        /**
         * @return the priority
         */
        public int getPriority() {
            return this._priority;
        }

        /**
         * @return the weight
         */
        public int getWeight() {
            return this._weight;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return this._port;
        }

        @Override
        boolean sameValue(DNSRecord other) {
            if (!(other instanceof Service)) {
                return false;
            }
            Service s = (Service) other;
            return (_priority == s._priority) && (_weight == s._weight) && (_port == s._port) && _server.equals(s._server);
        }

        @Override
        public boolean isSingleValued() {
            return true;
        }

        @Override
        boolean handleQuery(JmDNSImpl dns, long expirationTime) {
            ServiceInfoImpl info = (ServiceInfoImpl) dns.getServices().get(this.getKey());
            if (info != null && (info.isAnnouncing() || info.isAnnounced()) && (_port != info.getPort() || !_server.equalsIgnoreCase(dns.getLocalHost().getName()))) {
                logger.debug("handleQuery() Conflicting probe detected from: {}", getRecordSource());
                DNSRecord.Service localService = new DNSRecord.Service(info.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, info.getPriority(), info.getWeight(), info.getPort(), dns.getLocalHost().getName());

                // This block is useful for debugging race conditions when jmDNS is responding to itself.
                try {
                    if (dns.getInetAddress().equals(getRecordSource())) {
                        logger.warn("Got conflicting probe from ourselves\nincoming: {}\nlocal   : {}", this, localService);
                    }
                } catch (IOException e) {
                    logger.warn("IOException", e);
                }

                int comparison = this.compareTo(localService);

                if (comparison == 0) {
                    // the 2 records are identical this probably means we are seeing our own record.
                    // With multiple interfaces on a single computer it is possible to see our
                    // own records come in on different interfaces than the ones they were sent on.
                    // see section "10. Conflict Resolution" of mdns draft spec.
                    logger.debug("handleQuery() Ignoring a identical service query");
                    return false;
                }

                // tiebreaker test
                if (info.isProbing() && comparison > 0) {
                    // We lost the tie-break
                    String oldName = info.getQualifiedName().toLowerCase();
                    info.setName(NameRegister.Factory.getRegistry().incrementName(dns.getLocalHost().getInetAddress(), info.getName(), NameRegister.NameType.SERVICE));
                    dns.getServices().remove(oldName);
                    dns.getServices().put(info.getQualifiedName().toLowerCase(), info);
                    logger.debug("handleQuery() Lost tie break: new unique name chosen:{}", info.getName());

                    // We revert the state to start probing again with the new name
                    info.revertState();
                } else {
                    // We won the tie-break, so this conflicting probe should be ignored
                    // See paragraph 3 of section 9.2 in mdns draft spec
                    return false;
                }

                return true;

            }
            return false;
        }

        @Override
        boolean handleResponse(JmDNSImpl dns) {
            ServiceInfoImpl info = (ServiceInfoImpl) dns.getServices().get(this.getKey());
            if (info != null && (_port != info.getPort() || !_server.equalsIgnoreCase(dns.getLocalHost().getName()))) {
                logger.debug("handleResponse() Denial detected");

                if (info.isProbing()) {
                    String oldName = info.getQualifiedName().toLowerCase();
                    info.setName(NameRegister.Factory.getRegistry().incrementName(dns.getLocalHost().getInetAddress(), info.getName(), NameRegister.NameType.SERVICE));
                    dns.getServices().remove(oldName);
                    dns.getServices().put(info.getQualifiedName().toLowerCase(), info);
                    logger.debug("handleResponse() New unique name chose:{}", info.getName());

                }
                info.revertState();
                return true;
            }
            return false;
        }

        @Override
        DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) throws IOException {
            ServiceInfoImpl info = (ServiceInfoImpl) dns.getServices().get(this.getKey());
            if (info != null) {
                if (this._port == info.getPort() != _server.equals(dns.getLocalHost().getName())) {
                    return dns.addAnswer(in, addr, port, out, new DNSRecord.Service(info.getQualifiedName(), DNSRecordClass.CLASS_IN, DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, info.getPriority(), info.getWeight(), info.getPort(), dns
                            .getLocalHost().getName()));
                }
            }
            return out;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {
            return new ServiceInfoImpl(this.getQualifiedNameMap(), _port, _weight, _priority, persistent, _server);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceEvent(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        public ServiceEvent getServiceEvent(JmDNSImpl dns) {
            ServiceInfo info = this.getServiceInfo(false);
            ((ServiceInfoImpl) info).setDns(dns);
            return new ServiceEventImpl(dns, info.getType(), info.getName(), info);

        }

        /*
         * (non-Javadoc)
         * @see com.webobjects.discoveryservices.DNSRecord#toString(java.lang.StringBuilder)
         */
        @Override
        protected void toString(final StringBuilder sb) {
            super.toString(sb);
            sb.append(" server: '")
                .append(_server).append(':').append(_port)
                .append('\'');
        }

    }

    public static class HostInformation extends DNSRecord {
        String _os;
        String _cpu;

        /**
         * @param name
         * @param recordClass
         * @param unique
         * @param ttl
         * @param cpu
         * @param os
         */
        public HostInformation(String name, DNSRecordClass recordClass, boolean unique, int ttl, String cpu, String os) {
            super(name, DNSRecordType.TYPE_HINFO, recordClass, unique, ttl);
            _cpu = cpu;
            _os = os;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#addAnswer(javax.jmdns.impl.JmDNSImpl, javax.jmdns.impl.DNSIncoming, java.net.InetAddress, int, javax.jmdns.impl.DNSOutgoing)
         */
        @Override
        DNSOutgoing addAnswer(JmDNSImpl dns, DNSIncoming in, InetAddress addr, int port, DNSOutgoing out) {
            return out;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#handleQuery(javax.jmdns.impl.JmDNSImpl, long)
         */
        @Override
        boolean handleQuery(JmDNSImpl dns, long expirationTime) {
            return false;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#handleResponse(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        boolean handleResponse(JmDNSImpl dns) {
            return false;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#sameValue(javax.jmdns.impl.DNSRecord)
         */
        @Override
        boolean sameValue(DNSRecord other) {
            if (!(other instanceof HostInformation)) {
                return false;
            }
            HostInformation hostInformation = (HostInformation) other;

            // Use Objects.equals for null-safe comparisons
            return Objects.equals(_cpu, hostInformation._cpu) && Objects.equals(_os, hostInformation._os);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#isSingleValued()
         */
        @Override
        public boolean isSingleValued() {
            return true;
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#write(javax.jmdns.impl.DNSOutgoing)
         */
        @Override
        void write(MessageOutputStream out) {
            String hostInfo = _cpu + " " + _os;
            out.writeUTF(hostInfo, 0, hostInfo.length());
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceInfo(boolean)
         */
        @Override
        public ServiceInfo getServiceInfo(boolean persistent) {
            Map<String, String> hinfo = new HashMap<>(2);
            hinfo.put("cpu", _cpu);
            hinfo.put("os", _os);
            return new ServiceInfoImpl(this.getQualifiedNameMap(), 0, 0, 0, persistent, hinfo);
        }

        /*
         * (non-Javadoc)
         * @see javax.jmdns.impl.DNSRecord#getServiceEvent(javax.jmdns.impl.JmDNSImpl)
         */
        @Override
        public ServiceEvent getServiceEvent(JmDNSImpl dns) {
            ServiceInfo info = this.getServiceInfo(false);
            ((ServiceInfoImpl) info).setDns(dns);
            return new ServiceEventImpl(dns, info.getType(), info.getName(), info);
        }

        /*
         * (non-Javadoc)
         * @see com.webobjects.discoveryservices.DNSRecord#toString(java.lang.StringBuilder)
         */
        @Override
        protected void toString(final StringBuilder sb) {
            super.toString(sb);
            sb.append(" cpu: '").append(_cpu)
                .append("' os: '").append( _os)
                .append('\'');
        }

    }

    /**
     * Determine if a record can have multiple values in the cache.
     *
     * @return <code>false</code> if this record can have multiple values in the cache, <code>true</code> otherwise.
     */
    public abstract boolean isSingleValued();

    /**
     * Return service information associated with that record if appropriate.
     *
     * @return service information
     */
    public ServiceInfo getServiceInfo() {
        return this.getServiceInfo(false);
    }

    /**
     * Return service information associated with that record if appropriate.
     *
     * @param persistent
     *            if <code>true</code> ServiceListener.resolveService will be called whenever new information is received.
     * @return service information
     */
    public abstract ServiceInfo getServiceInfo(boolean persistent);

    /**
     * Creates and return a service event for this record.
     *
     * @param dns
     *            DNS serviced by this event
     * @return service event
     */
    public abstract ServiceEvent getServiceEvent(JmDNSImpl dns);

    public void setRecordSource(InetAddress source) {
        this._source = source;
    }

    public InetAddress getRecordSource() {
        return _source;
    }

    @Override
    protected void toString(final StringBuilder sb) {
        super.toString(sb);
        final int remainingTTL = getRemainingTTL(System.currentTimeMillis());
        sb.append(" ttl: '").append(remainingTTL).append('/').append(_ttl).append('\'');
    }

    public void setTTL(int ttl) {
        this._ttl = ttl;
    }

    public int getTTL() {
        return _ttl;
    }

    public long getCreated() {
        return this._created;
    }

}