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

import java.net.InetAddress;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceInfo.Fields;
import javax.jmdns.impl.JmDNSImpl.ServiceTypeEntry;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * A DNS question.
 *
 * @author Arthur van Hoff, Pierre Frisch
 */
public class DNSQuestion extends DNSEntry {
    protected final Logger logger = LoggerFactory.getLogger(DNSQuestion.class);

    /**
     * Checks if the local host name matches the name of this instance, ignoring case.
     * If there is a match, adds the DNS records for the local host to the provided set of answers.
     *
     * @param jmDNSImpl The {@link JmDNSImpl} instance representing the local DNS service.
     * @param answers   The set of {@link DNSRecord} to which the local host's DNS records should be added if a match is found.
     * @return {@code true} if the local host name matches this instance's name (case-insensitive); {@code false} otherwise.
     */
    boolean processHostMatch(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
        HostInfo hostInfo = jmDNSImpl.getLocalHost();
        String localHostName = hostInfo.getName();

        // Use equalsIgnoreCase directly without lowercasing.
        if (!localHostName.equalsIgnoreCase(this.getName())) {
            return false;
        }

        // If the names match, add all the answers and return true.
        answers.addAll(hostInfo.answers(this.getRecordClass(), this.isUnique(), DNSConstants.DNS_TTL));
        return true;
    }

    /**
     * Address question.
     */
    private static class DNS4Address extends DNSQuestion {
        DNS4Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            if (this.processHostMatch(jmDNSImpl, answers)) return;
            DNSRecord answer = jmDNSImpl.getLocalHost().getDNSAddressRecord(this.getRecordType(), DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL);
            if (answer != null) {
                answers.add(answer);
            }
        }

        @Override
        public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
            String name = this.getName().toLowerCase();
            return jmDNSImpl.getLocalHost().getName().equals(name) || jmDNSImpl.getServices().containsKey(name);
        }

    }

    /**
     * Address question.
     */
    private static class DNS6Address extends DNSQuestion {
        DNS6Address(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            if (this.processHostMatch(jmDNSImpl, answers)) return;
            DNSRecord answer = jmDNSImpl.getLocalHost().getDNSAddressRecord(this.getRecordType(), DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL);
            if (answer != null) {
                answers.add(answer);
            }
        }

        @Override
        public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
            String name = this.getName().toLowerCase();
            return jmDNSImpl.getLocalHost().getName().equals(name) || jmDNSImpl.getServices().containsKey(name);
        }

    }

    /**
     * Host Information question.
     */
    private static class HostInformation extends DNSQuestion {
        HostInformation(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }
    }

    /**
     * Pointer question.
     */
    private static class Pointer extends DNSQuestion {
        Pointer(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            // find matching services
            for (ServiceInfo serviceInfo : jmDNSImpl.getServices().values()) {
                this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) serviceInfo);
            }
            if (this.isServicesDiscoveryMetaQuery()) {
                for (final ServiceTypeEntry typeEntry : jmDNSImpl.getServiceTypes().values()) {
                    answers.add(new DNSRecord.Pointer("_services._dns-sd._udp.local.", DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, typeEntry.getType()));
                }
            } else if (this.isReverseLookup()) {
                String ipValue = this.getQualifiedNameMap().get(Fields.Instance);
                if ((ipValue != null) && (!ipValue.isEmpty())) {
                    InetAddress address = jmDNSImpl.getLocalHost().getInetAddress();
                    String hostIPAddress = (address != null ? address.getHostAddress() : "");
                    if (ipValue.equalsIgnoreCase(hostIPAddress)) {
                        if (this.isV4ReverseLookup()) {
                            answers.add(jmDNSImpl.getLocalHost().getDNSReverseAddressRecord(DNSRecordType.TYPE_A, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL));
                        }
                        if (this.isV6ReverseLookup()) {
                            answers.add(jmDNSImpl.getLocalHost().getDNSReverseAddressRecord(DNSRecordType.TYPE_AAAA, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL));
                        }
                    }
                }
            } else if (this.isDomainDiscoveryQuery()) {
                // TODO: We do not currently support domain discovery
                logger.debug("Domain Discovery Query not implemented yet");
            }
        }

    }

    /**
     * Service question.
     */
    private static class Service extends DNSQuestion {
        Service(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            if (this.processHostMatch(jmDNSImpl, answers)) return;
            String lowerCaseName = this.getName().toLowerCase();
            // Service type request
            if (jmDNSImpl.getServiceTypes().containsKey(lowerCaseName)) {
                DNSQuestion question = new Pointer(this.getName(), DNSRecordType.TYPE_PTR, this.getRecordClass(), this.isUnique());
                question.addAnswers(jmDNSImpl, answers);
                return;
            }

            this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) jmDNSImpl.getServices().get(lowerCaseName));
        }

        @Override
        public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
            String name = this.getName().toLowerCase();
            return jmDNSImpl.getLocalHost().getName().equals(name) || jmDNSImpl.getServices().containsKey(name);
        }

    }

    /**
     * Text question.
     */
    private static class Text extends DNSQuestion {
        Text(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) jmDNSImpl.getServices().get(this.getName().toLowerCase()));
        }

        @Override
        public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
            String name = this.getName().toLowerCase();
            return jmDNSImpl.getLocalHost().getName().equals(name) || jmDNSImpl.getServices().containsKey(name);
        }

    }

    /**
     * AllRecords question.
     */
    private static class AllRecords extends DNSQuestion {
        AllRecords(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
            super(name, type, recordClass, unique);
        }

        @Override
        public boolean isSameType(DNSEntry entry) {
            // We match all non-null entry
            return (entry != null);
        }

        @Override
        public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
            if (this.processHostMatch(jmDNSImpl, answers)) return;
            String lowerCaseName = this.getName().toLowerCase();
            // Service type request
            if (jmDNSImpl.getServiceTypes().containsKey(lowerCaseName)) {
                DNSQuestion question = new Pointer(this.getName(), DNSRecordType.TYPE_PTR, this.getRecordClass(), this.isUnique());
                question.addAnswers(jmDNSImpl, answers);
                return;
            }

            for (ServiceInfo serviceInfo : jmDNSImpl.getServices().values()) {
                this.addAnswersForServiceInfo(jmDNSImpl, answers, (ServiceInfoImpl) serviceInfo);
            }
        }

        @Override
        public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
            String name = this.getName().toLowerCase();
            return jmDNSImpl.getLocalHost().getName().equals(name) || jmDNSImpl.getServices().containsKey(name);
        }

    }

    DNSQuestion(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
        super(name, type, recordClass, unique);
    }

    /**
     * Create a question.
     *
     * @param name
     *            DNS name to be resolved
     * @param type
     *            Record type to resolve
     * @param recordClass
     *            Record class to resolve
     * @param unique
     *            Request unicast response (Currently not supported in this implementation)
     * @return new question
     */
    public static DNSQuestion newQuestion(String name, DNSRecordType type, DNSRecordClass recordClass, boolean unique) {
        switch (type) {
            case TYPE_A:
                return new DNS4Address(name, type, recordClass, unique);
            case TYPE_A6:
            case TYPE_AAAA:
                return new DNS6Address(name, type, recordClass, unique);
            case TYPE_ANY:
                return new AllRecords(name, type, recordClass, unique);
            case TYPE_HINFO:
                return new HostInformation(name, type, recordClass, unique);
            case TYPE_PTR:
                return new Pointer(name, type, recordClass, unique);
            case TYPE_SRV:
                return new Service(name, type, recordClass, unique);
            case TYPE_TXT:
                return new Text(name, type, recordClass, unique);
            default:
                return new DNSQuestion(name, type, recordClass, unique);
        }
    }

    /**
     * Check if this question is answered by a given DNS record.
     */
    boolean answeredBy(DNSEntry rec) {
        return this.isSameRecordClass(rec) && this.isSameType(rec) && this.getName().equals(rec.getName());
    }

    /**
     * Adds answers to the list for our question.
     *
     * @param jmDNSImpl
     *            DNS holding the records
     * @param answers
     *            List of previous answer to append.
     */
    public void addAnswers(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers) {
        // By default, we do nothing
    }

    protected void addAnswersForServiceInfo(JmDNSImpl jmDNSImpl, Set<DNSRecord> answers, ServiceInfoImpl info) {
        if ((info != null) && info.isAnnounced()) {
            if (this.getName().equalsIgnoreCase(info.getQualifiedName()) || this.getName().equalsIgnoreCase(info.getType()) || this.getName().equalsIgnoreCase(info.getTypeWithSubtype())) {
                answers.addAll(jmDNSImpl.getLocalHost().answers(this.getRecordClass(), DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL));
                answers.addAll(info.answers(this.getRecordClass(), DNSRecordClass.UNIQUE, DNSConstants.DNS_TTL, jmDNSImpl.getLocalHost(), this.getSubtype()));
            }
            logger.debug("{} DNSQuestion({}).addAnswersForServiceInfo(): info: {}\n{}", jmDNSImpl.getName(), this.getName(), info, answers);
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#isStale(long)
     */
    @Override
    public boolean isStale(long now) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#isExpired(long)
     */
    @Override
    public boolean isExpired(long now) {
        return false;
    }

    /**
     * Checks if we are the only to be able to answer that question.
     *
     * @param jmDNSImpl
     *            DNS holding the records
     * @return <code>true</code> if we are the only one with the answer to the question, <code>false</code> otherwise.
     */
    public boolean iAmTheOnlyOne(JmDNSImpl jmDNSImpl) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.DNSEntry#toString(java.lang.StringBuilder)
     */
    @Override
    public void toString(final StringBuilder sb) {
        // do nothing
    }

}
