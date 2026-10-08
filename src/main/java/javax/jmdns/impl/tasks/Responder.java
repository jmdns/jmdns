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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.impl.DNSIncoming;
import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * The Responder sends a single answer for the specified service infos and for the host name.
 */
public class Responder extends DNSTask {
    private final Logger logger = LoggerFactory.getLogger(Responder.class);
    private final DNSIncoming dnsIncoming;
    private final InetAddress inetAddress;
    private final int port;
    private final boolean unicast;

    public Responder(JmDNSImpl jmDNSImpl, DNSIncoming in, InetAddress addr, int port) {
        super(jmDNSImpl);
        this.dnsIncoming = in;
        this.inetAddress = addr;
        this.port = port;
        this.unicast = (port != DNSConstants.MDNS_PORT);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "Responder(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return super.toString() + " incoming: " + dnsIncoming;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#start(java.util.Timer)
     */
    @Override
    public void start(Timer timer) {
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "7 Responding":
        // We respond immediately if we know for sure, that we are the only one who can respond to the query.
        // In all other cases, we respond within 20-120 ms.
        //
        // According to draft-cheshire-dnsext-multicastdns.txt chapter "6.2 Multi-Packet Known Answer Suppression":
        // We respond after 20-120 ms if the query is truncated.

        boolean iAmTheOnlyOne = true;
        for (DNSQuestion question : dnsIncoming.getQuestions()) {
            logger.trace("{}.start() question={}", this.getName(), question);
            iAmTheOnlyOne = question.iAmTheOnlyOne(this.getDns());
            if (!iAmTheOnlyOne) {
                break;
            }
        }
        int delay = (iAmTheOnlyOne && !dnsIncoming.isTruncated()) ? 0 : DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + JmDNSImpl.getRandom().nextInt(DNSConstants.RESPONSE_MAX_WAIT_INTERVAL - DNSConstants.RESPONSE_MIN_WAIT_INTERVAL + 1) - dnsIncoming.elapseSinceArrival();
        if (delay < 0) {
            delay = 0;
        }
        logger.trace("{}.start() Responder chosen delay={}", this.getName(), delay);

        if (!this.getDns().isCanceling() && !this.getDns().isCanceled()) {
            timer.schedule(this, delay);
        }
    }

    @Override
    public void run() {
        this.getDns().respondToQuery(dnsIncoming);

        // We use these sets to prevent duplicate records
        Set<DNSQuestion> questions = new HashSet<>();
        Set<DNSRecord> answers = new HashSet<>();

        if (this.getDns().isAnnounced()) {
            try {
                // Answer questions
                for (DNSQuestion question : dnsIncoming.getQuestions()) {
                    logger.debug("{}.run() JmDNS responding to: {}", this.getName(), question);

                    // for unicast responses the question must be included
                    if (unicast) {
                        questions.add(question);
                    }

                    question.addAnswers(this.getDns(), answers);
                }

                // remove known answers, if the TTL is at least half of the correct value. (See Draft Cheshire chapter 7.1.).
                long now = System.currentTimeMillis();
                for (DNSRecord knownAnswer : dnsIncoming.getAnswers()) {
                    if (knownAnswer.isStale(now)) {
                        answers.remove(knownAnswer);
                        logger.debug("{} - JmDNS Responder Known Answer Removed", this.getName());
                    }
                }

                // respond if we have answers
                if (!answers.isEmpty()) {
                    logger.debug("{}.run() JmDNS responding", this.getName());
                    this.sendResponseGroups(questions, answers);
                }
            } catch (Throwable e) {
                logger.warn("{}.run() exception ", this.getName(), e);
                this.getDns().close();
            }
        }
    }

    private void sendResponseGroups(Set<DNSQuestion> questions, Set<DNSRecord> answers) throws IOException {
        List<Set<DNSRecord>> packetGroups = new ArrayList<>();
        DNSOutgoing out = this.createOutgoing(questions, packetGroups);

        for (Set<DNSRecord> responseGroup : createResponseGroups(answers)) {
            packetGroups.add(responseGroup);
            try {
                out = this.createOutgoing(questions, packetGroups);
            } catch (IOException exception) {
                packetGroups.remove(packetGroups.size() - 1);
                if (packetGroups.isEmpty()) {
                    out = this.createOutgoing(questions, packetGroups);
                    for (DNSRecord answer : responseGroup) {
                        out = this.addAnswer(out, dnsIncoming, answer);
                    }
                    if (!out.isEmpty()) {
                        this.getDns().send(out);
                    }
                    out = this.createOutgoing(questions, packetGroups);
                    continue;
                }

                this.getDns().send(out);
                packetGroups.clear();
                packetGroups.add(responseGroup);
                out = this.createOutgoing(questions, packetGroups);
            }
        }
        if (!packetGroups.isEmpty() && !out.isEmpty()) {
            this.getDns().send(out);
        }
    }

    private DNSOutgoing createOutgoing(Set<DNSQuestion> questions, List<Set<DNSRecord>> responseGroups) throws IOException {
        DNSOutgoing out = new DNSOutgoing(DNSConstants.FLAGS_QR_RESPONSE | DNSConstants.FLAGS_AA, !unicast, dnsIncoming.getSenderUDPPayload());
        out.setDestination(new InetSocketAddress(inetAddress, port));
        out.setId(dnsIncoming.getId());

        for (DNSQuestion question : questions) {
            if (question != null) {
                out.addQuestion(question);
            }
        }

        Set<DNSRecord> answers = new LinkedHashSet<>();
        for (Set<DNSRecord> responseGroup : responseGroups) {
            answers.addAll(responseGroup);
        }
        for (DNSRecord answer : answers) {
            if (answer != null) {
                out.addAnswer(dnsIncoming, answer);
            }
        }
        return out;
    }

    static List<Set<DNSRecord>> createResponseGroups(Set<DNSRecord> answers) {
        Map<String, Set<DNSRecord>> recordsByName = new HashMap<>();
        Map<String, Set<DNSRecord>> pointersByAlias = new HashMap<>();
        Set<DNSRecord> addressRecords = new LinkedHashSet<>();
        for (DNSRecord answer : answers) {
            recordsByName.computeIfAbsent(answer.getName(), name -> new LinkedHashSet<>()).add(answer);
            if (isAddressRecord(answer)) {
                addressRecords.add(answer);
            }
            if (answer instanceof DNSRecord.Pointer) {
                String alias = ((DNSRecord.Pointer) answer).getAlias();
                pointersByAlias.computeIfAbsent(alias, name -> new LinkedHashSet<>()).add(answer);
            }
        }
        if (pointersByAlias.size() <= 1) {
            List<Set<DNSRecord>> responseGroups = new ArrayList<>();
            responseGroups.add(new LinkedHashSet<>(answers));
            return responseGroups;
        }

        List<Set<DNSRecord>> responseGroups = new ArrayList<>();
        for (Map.Entry<String, Set<DNSRecord>> entry : pointersByAlias.entrySet()) {
            Set<DNSRecord> responseGroup = new LinkedHashSet<>(addressRecords);
            responseGroup.addAll(entry.getValue());
            Set<DNSRecord> serviceRecords = recordsByName.get(entry.getKey());
            if (serviceRecords != null) {
                responseGroup.addAll(serviceRecords);
            }
            responseGroups.add(responseGroup);
        }
        return responseGroups;
    }

    private static boolean isAddressRecord(DNSRecord record) {
        return record.getRecordType() == DNSRecordType.TYPE_A || record.getRecordType() == DNSRecordType.TYPE_AAAA;
    }

}
