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
package javax.jmdns.impl.tasks.resolver;

import java.io.IOException;

import javax.jmdns.impl.DNSOutgoing;
import javax.jmdns.impl.DNSQuestion;
import javax.jmdns.impl.DNSRecord;
import javax.jmdns.impl.JmDNSImpl;
import javax.jmdns.impl.JmDNSImpl.ServiceTypeEntry;
import javax.jmdns.impl.constants.DNSConstants;
import javax.jmdns.impl.constants.DNSRecordClass;
import javax.jmdns.impl.constants.DNSRecordType;

/**
 * Helper class to resolve service types.
 * <p/>
 * The TypeResolver queries three times consecutively for service types, and then removes itself from the timer.
 * <p/>
 * The TypeResolver will run only if JmDNS is in state ANNOUNCED.
 */
public class TypeResolver extends DNSResolverTask {

    /**
     * @param jmDNSImpl the implementation
     */
    public TypeResolver(JmDNSImpl jmDNSImpl) {
        super(jmDNSImpl);
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.DNSTask#getName()
     */
    @Override
    public String getName() {
        return "TypeResolver(" + (this.getDns() != null ? this.getDns().getName() : "") + ")";
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addAnswers(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addAnswers(final DNSOutgoing out) throws IOException {
        DNSOutgoing newOut = out;
        final long now = System.currentTimeMillis();
        for (final ServiceTypeEntry typeEntry : this.getDns().getServiceTypes().values()) {
            newOut = this.addAnswer(newOut, new DNSRecord.Pointer("_services._dns-sd._udp.local.", DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE, DNSConstants.DNS_TTL, typeEntry.getType()), now);
        }
        return newOut;
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#addQuestions(javax.jmdns.impl.DNSOutgoing)
     */
    @Override
    protected DNSOutgoing addQuestions(DNSOutgoing out) throws IOException {
        return this.addQuestion(out, DNSQuestion.newQuestion("_services._dns-sd._udp.local.", DNSRecordType.TYPE_PTR, DNSRecordClass.CLASS_IN, DNSRecordClass.NOT_UNIQUE));
    }

    /*
     * (non-Javadoc)
     * @see javax.jmdns.impl.tasks.Resolver#description()
     */
    @Override
    protected String description() {
        return "querying type";
    }
}